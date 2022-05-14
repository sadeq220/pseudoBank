package ir.sadeqcloud.gateway.service;

import ir.sadeqcloud.gateway.awareClasses.IoCContainerUtil;
import ir.sadeqcloud.gateway.model.TransferResponse;
import ir.sadeqcloud.gateway.sharedResource.IntermediaryObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ConsumeTransferResponse {
    /**
     * The listener container starts a thread for each concurrency (default 1);
     * each thread creates a consumer and interacts with its API to get records and pass them to the listener,
     * either one at a time, or in a batch, depending on the type of listener.
     */
    @KafkaListener(topics = "${consumer.topic}",groupId = "Transfer-response",containerFactory = "kafkaListenerContainer")
    public void consumeTransferResponse(ConsumerRecord<String, TransferResponse> consumerRecord){
        try{
        IntermediaryObject intermediaryObject = IoCContainerUtil.getBean(IntermediaryObject.class, consumerRecord.value().getCorrelationId());
        intermediaryObject.putTransferResponse(consumerRecord.value());
        }catch (NoSuchBeanDefinitionException e){ // in case of timeout

        }
    }

}
