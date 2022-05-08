package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.exception.BusinessException;
import ir.sadeqcloud.processor.model.LimitationKeyPrefix;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.redis.RedisDao;
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
public class WithdrawFinalNode implements ValueMapper<TransferResponse,TransferResponse> {
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
    public WithdrawFinalNode(CoreGateway coreGateway,
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
        gatewayCaller gatewayCaller = new gatewayCaller(transferResponse, coreGateway);
        Future<TrackIssueDTO> trackIssueDTOFuture = executorService.submit(gatewayCaller);
        try {
            TrackIssueDTO trackIssueDTO = trackIssueDTOFuture.get(PropertyConstants.getTimeOutInMili(), TimeUnit.MILLISECONDS);

            addLimitation(transferResponse);

            transferResponse.addResponseStatus(ResponseStatus.SUCCESS);
            return transferResponse;
        } catch (InterruptedException |ExecutionException| TimeoutException e) {
            //publish failure to kafka.failure.output
            throw new BusinessException(e.getCause().getMessage(),transferResponse.getCorrelationId());
        }
    }

    private class gatewayCaller implements Callable<TrackIssueDTO>{
        private TransferRequest transferRequest;
        private CoreGateway coreGateway;
        public gatewayCaller(TransferRequest transferRequest,
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
