package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
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
public class WithdrawProcessingTopology {
    private AccountOperations accountOperations;
    private Serde<TransferRequest> transferRequestSerde;
    private Serde<TransferResponse> transferResponseSerde;
    private String outputTopic;
    private Serde<String> stringSerde =Serdes.String();
    @Autowired
    public WithdrawProcessingTopology(Serde<TransferRequest> transferRequestSerde,
                                      AccountOperations accountOperations,
                                      Serde<TransferResponse> transferResponseSerde){
        this.transferRequestSerde=transferRequestSerde;
        this.accountOperations=accountOperations;
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
                 branch((string, transferRequest) -> transferRequest.getRequestType() == RequestType.PROCEED_WITHDRAW, ks -> withdrawBranching(ks))
                 .branch((string,transferRequest)->transferRequest.getRequestType()==RequestType.REVERSE,ks->reverseProcessing(ks)).
                 onTopOf(sourceNode);
    }

    public void withdrawBranching(KStream<String,TransferRequest> withdrawBranch){
        // deprecated branch() method replaced by split() method
         withdrawBranch.
                split(Named.as("withdraw-"))
                .branch((str,transferRequest)-> !accountOperations.checkWithdrawLimitationThresholdNotPassed(transferRequest), Branched.withConsumer(ks->produceFailureDueToAccountLimitation(ks), "FAILURE_ACCOUNT"))
                .defaultBranch(Branched.withConsumer(ks->successfulWithdrawProcessing(ks),"SUCCESS"));
                //filter((k,v)->!accountOperations.checkWithdrawLimitationThresholdNotPassed(v));
    }
    public void reverseProcessing(KStream<String,TransferRequest> reverseBranch){

    }
    private void successfulWithdrawProcessing(KStream<String,TransferRequest> successfulTransfer){
        successfulTransfer.mapValues(transferRequest -> new TransferResponse(transferRequest.getCorrelationId(),ResponseStatus.SUCCESS))
                .to(outputTopic,Produced.with(stringSerde,transferResponseSerde));
    }
    private void produceFailureDueToAccountLimitation(KStream<String,TransferRequest> failureStream){
    failureStream.mapValues(transferRequest -> new TransferResponse(transferRequest.getCorrelationId(), ResponseStatus.FAILURE_ACCOUNT))
            .to(outputTopic, Produced.with(stringSerde,transferResponseSerde));
    }
}
