package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.operations.DataStoreOperations;
import ir.sadeqcloud.processor.service.operations.RedisOperation.RedisOperations;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.SessionStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaStreamBrancher;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TransferProcessingTopology {
    private DataStoreOperations dataStoreOperations;
    private Serde<TransferRequest> transferRequestSerde;
    private Serde<TransferResponse> transferResponseSerde;
    private Serde<String> stringSerde =Serdes.String();
    private Serde<StringBuilder> stringBuilderSerde;//TODO populate
    private CoreGatewayIssueDocument coreGatewayIssueDocument;
    private CoreGatewayStatusChecker coreGatewayStatusChecker;
    private CoreGatewayReverseIssue coreGatewayReverseIssue;//TODO populate

    @Autowired
    public TransferProcessingTopology(Serde<TransferRequest> transferRequestSerde,
                                      RedisOperations redisOperations,
                                      Serde<TransferResponse> transferResponseSerde,
                                      CoreGatewayIssueDocument coreGatewayIssueDocument,
                                      CoreGatewayStatusChecker coreGatewayStatusChecker){
        this.coreGatewayIssueDocument = coreGatewayIssueDocument;
        this.coreGatewayStatusChecker=coreGatewayStatusChecker;
        this.transferRequestSerde=transferRequestSerde;
        this.dataStoreOperations = redisOperations;
        this.transferResponseSerde=transferResponseSerde;
    }
    @Bean("topologySourceNode")
    public KStream<String,TransferRequest> sourceProcessing(StreamsBuilder streamsBuilder){
    return streamsBuilder.stream(PropertyConstants.getInputTopic(),
            Consumed.with(Serdes.String(),transferRequestSerde).withName("source-node"));
    }
    @Bean
    private void branchStream(@Qualifier("topologySourceNode") KStream<String,TransferRequest> sourceNode){
         new KafkaStreamBrancher<String, TransferRequest>().
                 branch((string, transferRequest) -> transferRequest.getRequestType() == RequestType.PROCEED_WITHDRAW, ks -> withdrawProcessing(ks))
                 .branch((string,transferRequest)->transferRequest.getRequestType()==RequestType.REVERSE,ks->reverseProcessing(ks)).
                 onTopOf(sourceNode);
    }

    public void withdrawProcessing(KStream<String,TransferRequest> withdrawBranch){
        KStream<String, TransferResponse> successOrNeedsRetryKStream = withdrawBranch.
                mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest), Named.as("map-to-response"))
                .mapValues(new ThresholdLimitCheckerValueMapper(dataStoreOperations), Named.as("threshold-limit-checker"))
                .mapValues(coreGatewayIssueDocument,Named.as("issue-document"));

        successOrNeedsRetryKStream.
                split(Named.as("failure-checker"))
                .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(),Branched.withConsumer(ks->retryOnCoreGatewayTimeout(ks)))
                .defaultBranch(Branched.withConsumer(ks->withdrawFinished(ks)));

    }
    public void reverseProcessing(KStream<String,TransferRequest> reverseBranch){
        reverseBranch.mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest),Named.as("map-to-response"))
                     .mapValues(coreGatewayReverseIssue,Named.as("reverse-issue"))
                        .split(Named.as("failure-checker"))
                            .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(),Branched.withConsumer(ks->retryOnCoreGatewayTimeout(ks)))
                            .defaultBranch(Branched.withConsumer(ks->withdrawFinished(ks)));

    }

    /**
     * the retry mechanism takes time based on req timeout and number of retries
     * so consider to config the poll.max.interval.ms property
     * to not kick-out of consumer-group
     */
    private void retryOnCoreGatewayTimeout(KStream<String,TransferResponse> kStream){
    kStream.mapValues(coreGatewayStatusChecker)
            .split(Named.as("retry-node"))
            .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(),Branched.withConsumer(ks->retryOnCoreGatewayTimeout(ks)))
            .defaultBranch(Branched.withConsumer(ks->withdrawFinished(ks)));
    }

    private void withdrawFinished(KStream<String,TransferResponse> kStream){
        kStream.to(PropertyConstants.getNormalOutputTopic(),Produced.with(stringSerde,transferResponseSerde));
        kStream.filter((s, transferResponse) -> transferResponse.getRequestType()!=RequestType.REVERSE)
                .mapValues(transferResponse -> TransferResponse.buildNotification(transferResponse))
                .groupByKey()//group by accountNo
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMillis(1_000)))//no grace means out-of-order records ignored , so consider the record orders
                /**
                 * SessionWindowedKStream is an abstraction of a windowed record stream of KeyValue pairs.
                 * It is an intermediate representation after a grouping and windowing of a KStream
                 * before an aggregation is applied to the new (partitioned) windows resulting in a windowed KTable
                 * (a windowed KTable is a KTable with key type Windowed KTable<Windowed<K>,VR>).
                 */
                .aggregate(()->new StringBuilder()
                           ,(k,v,VA)->{VA.append("\n");return VA.append(v);}
                           , (s, VA1, VA2) ->{VA1.append("\n");return VA1.append(VA2);}//merger , merge two sessions into one , in case of two session merge caused by out-of-order record arrival
                           , materializedStateStoreForNotification());
        /**
         *  The result is written into a local SessionStore (which is basically an ever-updating materialized view).
         *  Furthermore, updates to the store are sent downstream into a KTable changelog stream.
         *
         *  SessionStore Interface for storing the aggregated values of sessions.
         * The key is internally represented as Windowed<K> that comprises the plain key and the Window that represents window start- and end-timestamp.
         */
    }

    private Materialized<String,StringBuilder, SessionStore<Bytes,byte[]>> materializedStateStoreForNotification(){
        Materialized<String, StringBuilder, SessionStore<Bytes,byte[]>> notificationSerde = Materialized.as("notificationSerde");
        notificationSerde.withKeySerde(stringSerde);
        notificationSerde.withValueSerde(stringBuilderSerde);
        notificationSerde.withCachingEnabled(); //in-memory cache, cause record compaction
        return notificationSerde;
    }
}
