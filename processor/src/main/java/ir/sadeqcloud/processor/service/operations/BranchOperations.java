package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.model.TransferRequest;

public class BranchOperations  implements DataStoreOperations{
    @Override
    public Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest) {
        return null;
    }

    @Override
    public void reverseWithdraw(TransferRequest transferRequest) {

    }

    @Override
    public void addSuccessfulWithdrawLimitation(TransferRequest transferRequest) {

    }
}
