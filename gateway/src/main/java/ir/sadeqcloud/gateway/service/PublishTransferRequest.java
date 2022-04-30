package ir.sadeqcloud.gateway.service;

import ir.sadeqcloud.gateway.constants.PropertyConstants;
import ir.sadeqcloud.gateway.model.TransferRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class PublishTransferRequest {
    private KafkaTemplate<String, TransferRequest> kafkaTemplate;
    private SendCallback sendCallback;
    @Autowired
    public PublishTransferRequest(KafkaTemplate<String,TransferRequest> kafkaTemplate,
                                  SendCallback sendCallback){
        this.sendCallback=sendCallback;
        this.kafkaTemplate=kafkaTemplate;
    }
    public void publishTransferMessage(TransferRequest transferRequest){
        /**
         * assume AccountNo as key to avoid parallel processing on consumer side
         * this ensures multiple transfer on single Account are always on same partition
         */
        ProducerRecord<String,TransferRequest> transferReq = new ProducerRecord(PropertyConstants.getProducerTopic(), transferRequest.getAccountNo(), transferRequest);
        /**
         * producer.send() is asynchronous by it's nature
         */
        ListenableFuture<SendResult<String, TransferRequest>> listenableFuture = kafkaTemplate.send(transferReq);
        listenableFuture.addCallback(sendCallback);

    }
}
