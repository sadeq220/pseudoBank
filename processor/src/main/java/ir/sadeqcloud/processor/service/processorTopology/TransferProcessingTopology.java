package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.ResponseStatus;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransferProcessingTopology {
    private DataStoreOperations dataStoreOperations;
    private Serde<TransferRequest> transferRequestSerde;
    private Serde<TransferResponse> transferResponseSerde;
    private Serde<String> stringSerde =Serdes.String();
    private Serde<StringBuilder> stringBuilderSerde;
    private CoreGatewayIssueDocument coreGatewayIssueDocument;//TODO write common,base interface for these three coreGateway callers
    private CoreGatewayStatusChecker coreGatewayStatusChecker;
    private CoreGatewayReverseIssue coreGatewayReverseIssue;
    private AtomicInteger atomicInteger=new AtomicInteger(0);
    @Autowired
    public TransferProcessingTopology(Serde<TransferRequest> transferRequestSerde,
                                      RedisOperations redisOperations,
                                      Serde<TransferResponse> transferResponseSerde,
                                      CoreGatewayIssueDocument coreGatewayIssueDocument,
                                      CoreGatewayStatusChecker coreGatewayStatusChecker,
                                      Serde<StringBuilder> stringBuilderSerde,
                                      CoreGatewayReverseIssue coreGatewayReverseIssue){
        this.coreGatewayIssueDocument = coreGatewayIssueDocument;
        this.coreGatewayStatusChecker=coreGatewayStatusChecker;
        this.transferRequestSerde=transferRequestSerde;
        this.dataStoreOperations = redisOperations;
        this.transferResponseSerde=transferResponseSerde;
        this.stringBuilderSerde=stringBuilderSerde;
        this.coreGatewayReverseIssue=coreGatewayReverseIssue;
    }
    @Bean("topologySourceNode")
    public KStream<String,TransferRequest> sourceProcessing(StreamsBuilder streamsBuilder){
    return streamsBuilder.stream(PropertyConstants.getInputTopic(),
            Consumed.with(Serdes.String(),transferRequestSerde).withName("source-node"));
    }
    @Bean("sourceBranch")
    public Map<String,KStream<String,TransferRequest>> branchStream(@Qualifier("topologySourceNode") KStream<String,TransferRequest> sourceNode){
        Map<String,KStream<String,TransferRequest>> splattedKStream=new HashMap<>();
        new KafkaStreamBrancher<String, TransferRequest>().
                 branch((string, transferRequest) -> transferRequest.getRequestType() == RequestType.PROCEED_WITHDRAW, ks -> splattedKStream.put("withdraw",ks))
                 .branch((string,transferRequest)->transferRequest.getRequestType()==RequestType.REVERSE,ks->splattedKStream.put("reverse",ks)).
                 onTopOf(sourceNode);
         return splattedKStream;
    }
    @Bean(name = "withdrawProcessing")
    public Map<String,KStream<String,TransferResponse>> withdrawProcessing(@Qualifier("sourceBranch") Map<String,KStream<String,TransferRequest>> sourceBranch){
        Map<String,KStream<String,TransferResponse>> splattedKStream=new HashMap<>();
        KStream<String,TransferRequest> withdrawBranch=sourceBranch.get("withdraw");
        KStream<String, TransferResponse> successOrNeedsRetryKStream = withdrawBranch.
                mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest), Named.as("map-to-response-withdraw"))
                .mapValues(new ThresholdLimitCheckerValueMapper(dataStoreOperations), Named.as("threshold-limit-checker"))
                .mapValues(coreGatewayIssueDocument,Named.as("issue-document"));

        successOrNeedsRetryKStream.
                split(Named.as("failure-checker-withdraw"))
                .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(),Branched.withConsumer(ks->splattedKStream.put("timeout",ks),"_withdraw-timeout"))
                .defaultBranch(Branched.withConsumer(ks->splattedKStream.put("final",ks),"_successful-withdraw"));
        return splattedKStream;
    }
    @Bean(name = "reverseProcessing")
    public Map<String,KStream<String,TransferResponse>> reverseProcessing(@Qualifier("sourceBranch") Map<String,KStream<String,TransferRequest>> sourceBranch){
        KStream<String,TransferRequest> reverseBranch=sourceBranch.get("reverse");
        Map<String,KStream<String,TransferResponse>> splattedKStream=new HashMap<>();
        reverseBranch.mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest),Named.as("map-to-response-reverse"))
                     .mapValues(coreGatewayReverseIssue,Named.as("reverse-issue"))
                        .split(Named.as("failure-checker-reverse"))
                            .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(),Branched.withConsumer(ks->splattedKStream.put("timeout",ks),"_reverse-timeout"))
                            .defaultBranch(Branched.withConsumer(ks->splattedKStream.put("final",ks),"_successful-reverse"));
        return splattedKStream;

    }

    /**
     * the retry mechanism takes time based on req timeout and number of retries
     * so consider to config the poll.max.interval.ms property
     * to not kick-out of consumer-group
     */
    @Bean(name = "retryNodes")
    public Map<String, KStream<String, TransferResponse>> retryOnCoreGatewayTimeout(@Qualifier("withdrawProcessing") Map<String,KStream<String,TransferResponse>> splattedKStreamWithdraw,
                                                                                     @Qualifier("reverseProcessing") Map<String,KStream<String,TransferResponse>> splattedKStreamReverse){
        KStream<String, TransferResponse> timeoutOnWithdraw = splattedKStreamWithdraw.get("timeout");
        KStream<String, TransferResponse> timeoutOnReverse = splattedKStreamReverse.get("timeout");
        Map<String,KStream<String,TransferResponse>> splattedKStream=new HashMap<>();
        KStream<String, TransferResponse> mergedStreams = timeoutOnWithdraw.merge(timeoutOnReverse,Named.as("WITHDRAW_REVERSE_TIMEOUT_MERGE"));
        Map<String, KStream<String, TransferResponse>> retryNodes = retryMechanism(mergedStreams, splattedKStream);
        return retryNodes;
    }
    private Map<String,KStream<String,TransferResponse>> retryMechanism(KStream<String,TransferResponse> kStream,Map<String,KStream<String,TransferResponse>> retryNodes){
        if(atomicInteger.incrementAndGet()<PropertyConstants.getMaxRetryOnCoreGatewayTimeout()){
    kStream.mapValues(coreGatewayStatusChecker,Named.as("correlation-status-check"))
            .split(Named.as("retry-node"))
            .branch((s, transferResponse) -> transferResponse.getCoreGatewayTimeout(), Branched.withConsumer(ks -> retryMechanism(ks,retryNodes)))
            .defaultBranch(Branched.withConsumer(ks->retryNodes.put("final"+atomicInteger.decrementAndGet(),ks)));
        }else{
            KStream<String, TransferResponse> endOfRetries = kStream.mapValues(transferResponse -> {
                transferResponse.addResponseStatus(ResponseStatus.FAILURE);
                return transferResponse;
            },Named.as("LastRetryNode-addFailureStatus"));
            retryNodes.put("final"+atomicInteger.get(),endOfRetries);
        }
        return retryNodes;
    }
    @Bean
    public Void withdrawFinished(@Qualifier("withdrawProcessing") Map<String,KStream<String,TransferResponse>> splattedKStreamWithdraw,
                                 @Qualifier("reverseProcessing") Map<String,KStream<String,TransferResponse>> splattedKStreamReverse,
                                 @Qualifier("retryNodes") Map<String,KStream<String,TransferResponse>> retryNodes){
        Optional<KStream<String, TransferResponse>> retryNodesMerged = retryNodes.values().stream().reduce((kStream1, kStream2) -> kStream1.merge(kStream2));
        KStream<String, TransferResponse> reverseFinal = splattedKStreamReverse.get("final");
        KStream<String, TransferResponse> withdrawFinal = splattedKStreamWithdraw.get("final");
        KStream<String, TransferResponse> allKStreamMerged=retryNodesMerged.isPresent()?withdrawFinal.merge(reverseFinal,Named.as("WITHDRAW_REVERSE_SUCCESS_MERGE")).merge(retryNodesMerged.get(),Named.as("RETRIES_MERGE"))
                                                                                       :withdrawFinal.merge(reverseFinal);
        allKStreamMerged.to(PropertyConstants.getNormalOutputTopic(),Produced.with(stringSerde,transferResponseSerde));
        allKStreamMerged.filter((s, transferResponse) -> transferResponse.getRequestType()!=RequestType.REVERSE,Named.as("filterOut-REVERSE"))
                .mapValues(transferResponse -> TransferResponse.buildNotification(transferResponse),Named.as("build-locale-dependent-message"))
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
        return null;
    }

    private Materialized<String,StringBuilder, SessionStore<Bytes,byte[]>> materializedStateStoreForNotification(){
        Materialized<String, StringBuilder, SessionStore<Bytes,byte[]>> notificationSerde = Materialized.as("notificationSerde");
        notificationSerde.withKeySerde(stringSerde);
        notificationSerde.withValueSerde(stringBuilderSerde);
        notificationSerde.withCachingEnabled(); //in-memory cache, cause record compaction
        return notificationSerde;
    }
}
