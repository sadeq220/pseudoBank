package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.operations.AccountOperations;
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
                 branch((string, transferRequest) -> transferRequest.getRequestType() == RequestType.PROCEED_WITHDRAW, ks -> withdrawProcessing(ks))
                 .branch((string,transferRequest)->transferRequest.getRequestType()==RequestType.REVERSE,ks->reverseProcessing(ks)).
                 onTopOf(sourceNode);
    }

    public KStream withdrawProcessing(KStream<String,TransferRequest> withdrawBranch){
        return withdrawBranch.
                mapValues(transferRequest -> TransferResponse.builderFactory(transferRequest),Named.as("mapperToResponse"))
                .mapValues(transferResponse -> {
                    if (accountOperations.checkWithdrawLimitationThresholdNotPassed(transferResponse))
                        return transferResponse;
                    transferResponse.addResponseStatus(ResponseStatus.FAILURE_ACCOUNT);
                    return transferResponse;
                },Named.as("CHECK_ACCOUNT_FAILURE"))
                .mapValues(transferResponse -> {
                    return transferResponse;
                },Named.as("CHECK_BRANCH_FAILURE"));
    }
    public void reverseProcessing(KStream<String,TransferRequest> reverseBranch){

    }
    private void successfulWithdrawProcessing(KStream<String,TransferRequest> successfulTransfer){
        successfulTransfer.mapValues(transferRequest ->{
            accountOperations.addSuccessfulWithdrawLimitation(transferRequest);// add successful withdraw to the list of Account successful withdraws
            return TransferResponse.builderFactory(transferRequest);
        })
                .to(outputTopic,Produced.with(stringSerde,transferResponseSerde));
    }
}
