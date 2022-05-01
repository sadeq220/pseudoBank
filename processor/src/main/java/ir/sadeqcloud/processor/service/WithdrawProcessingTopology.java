package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.TransferRequest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class WithdrawProcessingTopology {
    private Serde<TransferRequest> transferRequestSerde;
    @Autowired
    public WithdrawProcessingTopology(Serde<TransferRequest> transferRequestSerde){
        this.transferRequestSerde=transferRequestSerde;
    }
    @Bean
    public KStream<String,TransferRequest> sourceProcessing(@Qualifier("streamsBuilderFactoryBean") StreamsBuilder streamsBuilder){
    return streamsBuilder.stream(PropertyConstants.getInputTopic(),
            Consumed.with(Serdes.String(),transferRequestSerde).withName("source-node"));
    }
}
