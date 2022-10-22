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
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
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
    public ProducerFactory<String,TransferRequest> producerFactory(Jackson2JavaTypeMapper javaTypeMapper){
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        /**
         * from docs: it's better to use two option below instead of specifying max.retries
         */
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,15_000);// max inflight time,default is 30s
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,120_000);// max time spent on a single message produce,(include inflightTimeOut and retries)

        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        JsonSerializer<TransferRequest> valueJsonSerializer = new JsonSerializer<>();
        valueJsonSerializer.setTypeMapper(javaTypeMapper);
        return new DefaultKafkaProducerFactory(properties,new StringSerializer(),valueJsonSerializer);
    }

    /**
     * very thin wrapper around kafkaProducer
     */
    @Bean
    public KafkaTemplate<String, TransferRequest> kafkaTemplate(ProducerFactory<String,TransferRequest> producerFactory){
        KafkaTemplate kafkaTemplate = new KafkaTemplate(producerFactory);
        kafkaTemplate.setDefaultTopic(PropertyConstants.getProducerTopic());
        return kafkaTemplate;
    }

    @Bean
    public Jackson2JavaTypeMapper javaTypeMapper(){
        DefaultJackson2JavaTypeMapper defaultJackson2JavaTypeMapper = new DefaultJackson2JavaTypeMapper();
        HashMap<String, Class<?>> idClassMapper = new HashMap<>();
        idClassMapper.put("transferRequest",TransferRequest.class);
        defaultJackson2JavaTypeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        defaultJackson2JavaTypeMapper.setIdClassMapping(idClassMapper);
        return defaultJackson2JavaTypeMapper;
    }
    @Bean
    public NewTopic createTopic(){
        return TopicBuilder.name(PropertyConstants.getProducerTopic()).partitions(2).replicas(1).build();
    }
    @Bean
    public ConsumerFactory<String,TransferResponse> consumerFactory(Jackson2JavaTypeMapper javaTypeMapper){
        Map<String, Object> config=new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG,"transfer-group");

        JsonDeserializer<TransferResponse> transferResponseJsonDeserializer = new JsonDeserializer<>(TransferResponse.class);
        transferResponseJsonDeserializer.setTypeMapper(javaTypeMapper);
        return new DefaultKafkaConsumerFactory<>(config,new StringDeserializer(),transferResponseJsonDeserializer);
    }
    @Bean
    /**
     * This factory is primarily for building containers for KafkaListener annotated methods but can also be used to create any container.
     * Only containers for KafkaListener annotated methods are added to the KafkaListenerEndpointRegistry(a container).
     */
    public ConcurrentKafkaListenerContainerFactory<String, TransferResponse> kafkaListenerContainer(ConsumerFactory<String,TransferResponse> consumerFactory){
        ConcurrentKafkaListenerContainerFactory<String, TransferResponse> listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }
}
