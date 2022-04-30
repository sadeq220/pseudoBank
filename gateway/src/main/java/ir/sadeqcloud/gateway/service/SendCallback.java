package ir.sadeqcloud.gateway.service;

import ir.sadeqcloud.gateway.model.TransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
/**
 * this class is free of race conditions so it's thread safe hence
 * singleton scope works fine
 */
public class SendCallback implements ListenableFutureCallback<SendResult<String, TransferRequest>> {
    private Logger logger= LoggerFactory.getLogger(SendCallback.class);
    @Override
    /**
     * when delivery.timeout exceeds or non-transient error happened
     *
     * transient errors or retryable errors are exceptions that child of " RetriableException.class "
     * e.g LeaderNotAvailableException
     */
    public void onFailure(Throwable ex) {
    logger.error("publish Transfer messages (a single batch) failed , exception message is {} , caused by {} ",ex.getMessage(),ex.getCause());
    }

    @Override
    public void onSuccess(SendResult<String, TransferRequest> result) {
    logger.info("publish Transfer message success, correlationID {}",result.getProducerRecord().value().getCorrelationId());
    }
}
