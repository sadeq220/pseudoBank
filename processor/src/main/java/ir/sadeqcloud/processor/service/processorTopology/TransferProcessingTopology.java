package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.model.LimitationKeyPrefix;
import ir.sadeqcloud.processor.service.operations.DataStoreOperations;
import ir.sadeqcloud.processor.service.operations.RedisOperation.RedisOperations;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
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
    private WithdrawFinalNode withdrawFinalNode;

    @Autowired
    public TransferProcessingTopology(Serde<TransferRequest> transferRequestSerde,
                                      RedisOperations redisOperations,
                                      Serde<TransferResponse> transferResponseSerde,
                                      WithdrawFinalNode withdrawFinalNode){
        this.withdrawFinalNode=withdrawFinalNode;
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
         withdrawBranch.
                mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest),Named.as("mapperToResponse"))
                .mapValues(new ThresholdLimitCheckerValueMapper(dataStoreOperations),Named.as("ThresholdLimitChecker"))
                .mapValues(withdrawFinalNode,Named.as("gatewayCaller"))
                .to(PropertyConstants.getNormalOutputTopic(),Produced.with(stringSerde,transferResponseSerde));

    }
    public void reverseProcessing(KStream<String,TransferRequest> reverseBranch){

    }
}
