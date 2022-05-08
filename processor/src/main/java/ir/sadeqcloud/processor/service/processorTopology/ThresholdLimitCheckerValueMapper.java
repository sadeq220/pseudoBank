package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.model.LimitationKeyPrefix;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.operations.DataStoreOperations;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class ThresholdLimitCheckerValueMapper implements ValueMapper<TransferResponse,TransferResponse> {

    private DataStoreOperations dataStoreOperations;
    @Autowired
    public ThresholdLimitCheckerValueMapper(DataStoreOperations dataStoreOperations){
        this.dataStoreOperations=dataStoreOperations;
    }
    @Override
    public TransferResponse apply(TransferResponse transferResponse) {
        TransferResponse newTransferResponse = TransferResponse.builderFactory(transferResponse);
        if (!dataStoreOperations.checkWithdrawLimitationThresholdNotPassed(transferResponse, LimitationKeyPrefix.ACCOUNT))
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE_ACCOUNT);
        if (!dataStoreOperations.checkWithdrawLimitationThresholdNotPassed(transferResponse,LimitationKeyPrefix.BRANCH))
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE_BRANCH);
        if (!dataStoreOperations.checkWithdrawLimitationThresholdNotPassed(transferResponse,LimitationKeyPrefix.BANK))
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE_BANK);

        return newTransferResponse;
    }
}
