package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.gateway.CoreGateway;
import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.operations.DataStoreOperations;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("WithdrawFinalNode")
/**
 * create bean to apply declarative transaction management
 */
public class WithdrawFinalNode implements ValueMapper<TransferResponse,TransferResponse> {
    private CoreGateway coreGateway;
    private DataStoreOperations dataStoreOperations;
    @Autowired
    public WithdrawFinalNode(CoreGateway coreGateway,
                             DataStoreOperations dataStoreOperations){
        this.coreGateway=coreGateway;
        this.dataStoreOperations=dataStoreOperations;
    }

    @Override
    @Transactional(timeout = 10)
    public TransferResponse apply(TransferResponse transferResponse) {
        if (!transferResponse.getResponseStatuses().isEmpty())
            return transferResponse;
        IssueRequest issueRequest = IssueRequest.builderFactory(transferResponse);
        coreGateway.issueDocument(issueRequest);
        return null;
    }
}
