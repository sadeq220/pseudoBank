package ir.sadeqcloud.gateway;

import ir.sadeqcloud.gateway.constants.PropertyConstants;
import ir.sadeqcloud.gateway.model.TransferRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;

@SpringBootApplication
public class GatewayApplication {

    @DependsOn("constants")
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public ProducerFactory<String,String> producerFactory(){
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,String.class);
        /**
         * this will use jackson library to serialize
         */
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        /**
         * from docs: it's better to use two option below instead of specifying max.retries
         */
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,15_000);// max inflight time,default is 30s
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,120_000);// max time spent on a single message produce,(include inflightTimeOut and retries)

        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * very thin wrapper around kafkaProducer
     */
    @Bean
    public KafkaTemplate<String, TransferRequest> kafkaTemplate(){
        return new KafkaTemplate(producerFactory());
    }
    @Bean
    public NewTopic createTopic(){
        return TopicBuilder.name(PropertyConstants.getProducerTopic()).partitions(2).replicas(1).build();
    }

}
