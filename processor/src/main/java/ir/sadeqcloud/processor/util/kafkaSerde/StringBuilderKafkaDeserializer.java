package ir.sadeqcloud.processor.util.kafkaSerde;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class StringBuilderKafkaDeserializer implements Deserializer<StringBuilder> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public StringBuilder deserialize(String s, byte[] bytes) {
        String immutableString = new String(bytes);
        return new StringBuilder(immutableString);
    }

    @Override
    public StringBuilder deserialize(String topic, Headers headers, byte[] data) {
        return this.deserialize(topic, data);
    }

    @Override
    public void close() {

    }
}
