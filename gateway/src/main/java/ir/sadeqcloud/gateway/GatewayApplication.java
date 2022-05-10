package ir.sadeqcloud.gateway;

import ir.sadeqcloud.gateway.constants.PropertyConstants;
import ir.sadeqcloud.gateway.model.TransferRequest;
import ir.sadeqcloud.gateway.model.TransferResponse;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableKafka //to register @KafkaListener
public class GatewayApplication {

    @DependsOn("constants")
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public ProducerFactory<String,String> producerFactory(){
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
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
    @Bean
    public ConsumerFactory<String,TransferResponse> consumerFactory(){
        Map<String, Object> config=new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG,"transfer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config,new StringDeserializer(),new JsonSerde<TransferResponse>().deserializer());
    }
    @Bean
    /**
     * This factory is primarily for building containers for KafkaListener annotated methods but can also be used to create any container.
     * Only containers for KafkaListener annotated methods are added to the KafkaListenerEndpointRegistry(a container).
     */
    public ConcurrentKafkaListenerContainerFactory<String, TransferResponse> kafkaListenerContainer(){
        ConcurrentKafkaListenerContainerFactory<String, TransferResponse> listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        listenerContainerFactory.setConsumerFactory(consumerFactory());
        return listenerContainerFactory;
    }
}
