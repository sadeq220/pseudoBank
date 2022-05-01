package ir.sadeqcloud.processor.config;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StoreBuilder;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;

public class KafkaStreamInfrastructureConfig implements KafkaStreamsInfrastructureCustomizer {
    @Override
    public void configureBuilder(StreamsBuilder builder) {
    // builder.addStateStore(StoreBuilder);
    }

    @Override
    public void configureTopology(Topology topology) {

    }
}
