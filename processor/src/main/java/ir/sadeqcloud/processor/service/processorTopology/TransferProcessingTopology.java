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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaStreamBrancher;
import org.springframework.stereotype.Service;

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
                .aggregate(()->new StringBuilder()
                           ,(k,v,VA)->{VA.append("\n");return VA.append(v);}
                           , materializedStateStoreForNotification());
    }
    private Materialized<String,StringBuilder, KeyValueStore<Bytes,byte[]>> materializedStateStoreForNotification(){
        Materialized<String, StringBuilder, KeyValueStore<Bytes,byte[]>> notificationSerde = Materialized.as("notificationSerde");
        notificationSerde.withKeySerde(stringSerde);
        notificationSerde.withValueSerde(stringBuilderSerde);
        notificationSerde.withCachingEnabled();
        return notificationSerde;
    }
}
