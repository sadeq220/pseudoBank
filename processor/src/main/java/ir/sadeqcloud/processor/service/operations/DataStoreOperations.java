package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.model.TransferRequest;

public interface DataStoreOperations {
    Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest);
    void reverseWithdraw(TransferRequest transferRequest);
    void addSuccessfulWithdrawLimitation(TransferRequest transferRequest);
}
