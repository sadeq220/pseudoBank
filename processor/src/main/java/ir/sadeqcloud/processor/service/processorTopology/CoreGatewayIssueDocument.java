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

@Component("WithdrawFinalNode")
/**
 * create bean to populate collaborators
 */
public class CoreGatewayIssueDocument implements ValueMapper<TransferResponse,TransferResponse> {
    private CoreGateway coreGateway;
    private DataStoreOperations dataStoreOperations;
    private ExecutorService executorService;// you can use spring TaskExecutor wrapper

    /**
     * @param executorService
     * cause we don't want to involve ourselves with states of the 'circuit breaker' and it's overhead
     * and just simple timeout catcher we didn't use a Resilience4j
     * of course we can set 'Disable' state
     */
    @Autowired
    public CoreGatewayIssueDocument(CoreGateway coreGateway,
                                    DataStoreOperations dataStoreOperations,
                                    ExecutorService executorService){
        this.coreGateway=coreGateway;
        this.dataStoreOperations=dataStoreOperations;
        this.executorService=executorService;
    }

    @Override
    public TransferResponse apply(TransferResponse transferResponse) {
        if (!transferResponse.getResponseStatuses().isEmpty())
            return transferResponse;
        TransferResponse newTransferResponse = TransferResponse.builderFactory(transferResponse);//key rule of Functional programming

        GatewayCaller gatewayCaller = new GatewayCaller(newTransferResponse, coreGateway);
        Future<TrackIssueDTO> trackIssueDTOFuture = executorService.submit(gatewayCaller);
        try {
            TrackIssueDTO trackIssueDTO = trackIssueDTOFuture.get(PropertyConstants.getTimeOutInMili(), TimeUnit.MILLISECONDS);

            addLimitation(newTransferResponse);

            newTransferResponse.addResponseStatus(ResponseStatus.SUCCESS);
            return newTransferResponse;
        } catch (InterruptedException |ExecutionException e) { // in case of 4xx or 5xx this block will be executed
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE);
            newTransferResponse.setCoreGatewayTimeout(false);
            return newTransferResponse;
        }catch (TimeoutException timeout){
            newTransferResponse.setCoreGatewayTimeout(true);
            return newTransferResponse;
        }
    }

    private class GatewayCaller implements Callable<TrackIssueDTO>{
        private TransferRequest transferRequest;
        private CoreGateway coreGateway;
        public GatewayCaller(TransferRequest transferRequest,
                             CoreGateway coreGateway){
            this.transferRequest=transferRequest;
            this.coreGateway=coreGateway;
        }
        @Override
        public TrackIssueDTO call() throws Exception {
            TrackIssueDTO trackIssueDTO = coreGateway.issueDocument(IssueRequest.builderFactory(transferRequest));
            return trackIssueDTO;
        }
    }


    private void addLimitation(TransferRequest transferRequest){
        try {
            dataStoreOperations.addSuccessfulWithdrawLimitation(transferRequest,LimitationKeyPrefix.ACCOUNT);
            dataStoreOperations.addSuccessfulWithdrawLimitation(transferRequest,LimitationKeyPrefix.BRANCH);
            dataStoreOperations.addSuccessfulWithdrawLimitation(transferRequest,LimitationKeyPrefix.BANK);
        }catch (Exception e){
            e.printStackTrace();
            /**
             * don't care if dataStore operations go crazy
             */
        }
    }

}
