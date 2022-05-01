package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class WithdrawProcessingTopology {
    @Bean
    public KStream sourceProcessing(@Qualifier("streamsBuilderFactoryBean") StreamsBuilder streamsBuilder){
    return streamsBuilder.stream(PropertyConstants.getInputTopic());
    }
}
