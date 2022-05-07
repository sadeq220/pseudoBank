package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.redis.LimitationKeyPrefix;

public interface DataStoreOperations {
    Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
    void reverseWithdraw(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
    void addSuccessfulWithdrawLimitation(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix);
}
