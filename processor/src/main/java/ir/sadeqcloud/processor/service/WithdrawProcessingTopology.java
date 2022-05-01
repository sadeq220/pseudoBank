package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.TransferRequest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaStreamBrancher;
import org.springframework.stereotype.Service;

@Service
public class WithdrawProcessingTopology {
    private AccountOperations accountOperations;
    private Serde<TransferRequest> transferRequestSerde;
    @Autowired
    public WithdrawProcessingTopology(Serde<TransferRequest> transferRequestSerde,
                                      AccountOperations accountOperations){
        this.transferRequestSerde=transferRequestSerde;
        this.accountOperations=accountOperations;
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
        return withdrawBranch.filter((k,v)->accountOperations.checkWithdrawLimitationThresholdNotPassed(v));
    }
    public void reverseProcessing(KStream<String,TransferRequest> reverseBranch){

    }
}
