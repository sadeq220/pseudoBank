package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.LimitationKeyPrefix;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.gateway.CoreGateway;
import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.gateway.dto.TrackIssueDTO;
import ir.sadeqcloud.processor.service.operations.DataStoreOperations;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class CoreGatewayReverseIssue implements ValueMapper<TransferResponse,TransferResponse> {
    private ExecutorService executorService;
    private DataStoreOperations dataStoreOperations;
    private CoreGateway coreGateway;
    @Autowired
    public CoreGatewayReverseIssue(DataStoreOperations dataStoreOperations,
                                   CoreGateway coreGateway,
                                   ExecutorService executorService){
        this.executorService=executorService;
        this.dataStoreOperations=dataStoreOperations;
        this.coreGateway=coreGateway;
    }
    @Override
    public TransferResponse apply(TransferResponse transferResponse) {
        if (transferResponse==null)
            return null;
        TransferResponse newTransferResponse = TransferResponse.builderFactory(transferResponse);//key rule of Functional programming
        GatewayCaller gatewayCaller = new GatewayCaller(newTransferResponse, coreGateway);
        Future<TrackIssueDTO> trackIssueDTOFuture = executorService.submit(gatewayCaller);
        try {
            trackIssueDTOFuture.get(PropertyConstants.getTimeOutInMili(), TimeUnit.MILLISECONDS);
            this.removeLimitation(newTransferResponse);
            newTransferResponse.addResponseStatus(ResponseStatus.SUCCESS);
            return newTransferResponse;
        } catch (InterruptedException |ExecutionException e) {
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE);
            newTransferResponse.setCoreGatewayTimeout(false);
            return newTransferResponse;
        } catch (TimeoutException e) {
            newTransferResponse.setCoreGatewayTimeout(true);
            return newTransferResponse;
        }
    }
    private class GatewayCaller implements Callable<TrackIssueDTO> {
        private TransferRequest transferRequest;
        private CoreGateway coreGateway;
        public GatewayCaller(TransferRequest transferRequest,
                             CoreGateway coreGateway){
            this.transferRequest=transferRequest;
            this.coreGateway=coreGateway;
        }
        @Override
        public TrackIssueDTO call() throws Exception {
            TrackIssueDTO trackIssueDTO = coreGateway.reverseWithdraw(transferRequest.getCorrelationId());
            return trackIssueDTO;
        }
    }
    private void removeLimitation(TransferRequest transferRequest){
        try {
            dataStoreOperations.reverseWithdraw(transferRequest, LimitationKeyPrefix.ACCOUNT);
            dataStoreOperations.reverseWithdraw(transferRequest,LimitationKeyPrefix.BRANCH);
            dataStoreOperations.reverseWithdraw(transferRequest,LimitationKeyPrefix.BANK);
        }catch (Exception e){
            e.printStackTrace();
            /**
             * don't care if dataStore operations go crazy
             */
        }
    }
}
