package ir.sadeqcloud.gateway.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("constants")
public class PropertyConstants {
    private static String PRODUCER_TOPIC;
    public PropertyConstants(@Value("${producer.topic}")String producerTopic){
        PropertyConstants.PRODUCER_TOPIC=producerTopic;
    }
    public static String getProducerTopic(){
        return PRODUCER_TOPIC;
    }
}
