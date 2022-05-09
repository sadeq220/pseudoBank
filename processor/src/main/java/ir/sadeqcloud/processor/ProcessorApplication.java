package ir.sadeqcloud.processor;

import ir.sadeqcloud.processor.config.KafkaStreamInfrastructureConfig;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.util.kafkaSerde.StringBuilderKafkaDeserializer;
import ir.sadeqcloud.processor.util.kafkaSerde.StringBuilderKafkaSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableKafkaStreams
@EnableTransactionManagement
public class ProcessorApplication {
    private final List<String> brokers;
    private final String applicationId;
    public ProcessorApplication(@Value("${spring.kafka.bootstrap-servers}") List<String> brokers,
                                  @Value("${spring.application.name}") String applicationId){
        this.applicationId=applicationId;
        this.brokers= Collections.unmodifiableList(brokers);
    }
    @DependsOn("constants")
    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration(){
        HashMap<String, Object> kafkaStreamConfigs = new HashMap<>();
        kafkaStreamConfigs.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,brokers);//mandatory
        kafkaStreamConfigs.put(StreamsConfig.APPLICATION_ID_CONFIG,applicationId);//mandatory ,  Each stream processing application must have a unique ID. The same ID must be given to all instances of the application
        /**
         * This ID is used in the following places to isolate resources used by the application from others:
         *
         *     As the default Kafka consumer and producer client.id prefix
         *     As the Kafka consumer group.id for coordination
         *     As the name of the subdirectory in the state directory (cf. state.dir)
         *     As the prefix of internal Kafka topic names
         */
        kafkaStreamConfigs.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,10_000);//define KTable cache interval flush
        kafkaStreamConfigs.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG,1);//default one , use more when subscribing to 2 or more partitions ,for parallelism.
        kafkaStreamConfigs.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG,"TRACE");
        kafkaStreamConfigs.put("consumer."+ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,15_000);
        return new KafkaStreamsConfiguration(kafkaStreamConfigs);
    }
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    /**
     * ease "KafkaStreams" lifecycle management
     *
     * If you would like to control the lifecycle manually (for example, stopping and starting by some condition),
     * you can reference the StreamsBuilderFactoryBean bean directly by using the factory bean (&) prefix.
     * Since StreamsBuilderFactoryBean use its internal KafkaStreams instance, it is safe to stop and restart it again.
     */
    public FactoryBean<StreamsBuilder> streamsBuilderFactoryBean(){
        StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean(kafkaStreamsConfiguration());
        streamsBuilderFactoryBean.setInfrastructureCustomizer(new KafkaStreamInfrastructureConfig());// to add state store to streamsBuilder
        return streamsBuilderFactoryBean;
    }
    @Bean
    /**
     * TODO return new JsonSerde<TransformRequest>();
     */
    public Serde<TransferRequest> inputJsonSerde(){
        JsonSerializer<TransferRequest> transferRequestJsonSerializer = new JsonSerializer<>();
        JsonDeserializer<TransferRequest> transferRequestJsonDeserializer = new JsonDeserializer<>();
        return Serdes.serdeFrom(transferRequestJsonSerializer,transferRequestJsonDeserializer);
    }
    @Bean
    public Serde<TransferResponse> outputJsonSerde(){
        return new JsonSerde<>(TransferResponse.class);
    }
    @Bean
    public Serde<StringBuilder> stringBuilderSerde(){
        StringBuilderKafkaSerializer stringBuilderKafkaSerializer = new StringBuilderKafkaSerializer();
        StringBuilderKafkaDeserializer stringBuilderKafkaDeserializer = new StringBuilderKafkaDeserializer();
        return Serdes.serdeFrom(stringBuilderKafkaSerializer,stringBuilderKafkaDeserializer);
    }
    @Bean
    /**
     * we use stand-alone connections
     * you may use Master/replica connections in production
     */
    public RedisStandaloneConfiguration redisStandaloneConfiguration(){
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName("localhost");
        redisStandaloneConfiguration.setPort(6379);
        return redisStandaloneConfiguration;
    }
    @Bean
    /**
     * jedis connector to provide RedisConnectionFactory
     * if you want reactive api you must use Lettuce connector which is netty-based connector
     */
    public JedisConnectionFactory jedisConnectionFactory(){
        // fluent API
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().
                readTimeout(Duration.ofMillis(500L)).
                usePooling().
                build();
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration(),jedisClientConfiguration);
        return jedisConnectionFactory;
    }
    @Bean
    /**
     * spring Template classes is a abstraction layer over low level api that use -- callback -- approach ,
     * these classes implement 'Template method pattern'
     */
    public StringRedisTemplate redisTemplate(){
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(jedisConnectionFactory());
        stringRedisTemplate.setEnableTransactionSupport(true);
        /**
         *  Doing so forces binding the current RedisConnection to the current Thread that is triggering MULTI
         */
        return stringRedisTemplate;
    }
    @Bean
    public DataSource dataSource(){
        EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
        embeddedDatabaseBuilder.setName("ForTransaction");
        embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2);
        return embeddedDatabaseBuilder.build();
    }

    @Bean
    /**
     * TransactionManager suppose to bind a resource(e.g. JDBC connection,Hibernate Session ) to a thread
     *
     * Transaction management requires a PlatformTransactionManager.
     * Spring Data Redis does not ship with a PlatformTransactionManager implementation.
     * Assuming your application uses JDBC, Spring Data Redis can participate in transactions by using existing transaction managers.
     */
    public PlatformTransactionManager platformTransactionManager(){
        return new DataSourceTransactionManager(dataSource());
    }
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
    @Bean
    public ExecutorService concurrentExecutor(){
        return Executors.newFixedThreadPool(10);
    }
}
