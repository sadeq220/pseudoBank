package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.LimitationKeyPrefix;

/**
 * common base class for caching purposes
 * it can be implemented with redis , kafka stream state store , or even JPA
 */
public interface DataStoreOperations {
    Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
    void reverseWithdraw(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
    void addSuccessfulWithdrawLimitation(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
}
