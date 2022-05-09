package ir.sadeqcloud.processor.util.kafkaSerde;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringBuilderKafkaSerializer implements Serializer<StringBuilder> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String s, StringBuilder stringBuilder) {
        String immutableString=stringBuilder.toString();
        return immutableString.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, StringBuilder data) {
        return this.serialize(topic, data);
    }

    @Override
    public void close() {

    }
}
