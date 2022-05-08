package ir.sadeqcloud.processor.service.processorTopology;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.model.ResponseStatus;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.TransferResponse;
import ir.sadeqcloud.processor.service.gateway.CoreGateway;
import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.gateway.dto.TrackIssueDTO;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class CoreGatewayStatusChecker implements ValueMapper<TransferResponse,TransferResponse> {
    private CoreGateway coreGateway;
    private ExecutorService executorService;
    @Autowired
    public CoreGatewayStatusChecker(CoreGateway coreGateway,
                                    ExecutorService executorService){
        this.coreGateway=coreGateway;
        this.executorService=executorService;
    }
    @Override
    public TransferResponse apply(TransferResponse transferResponse) {
        if (transferResponse==null)
        return null;
        TransferResponse newTransferResponse = TransferResponse.builderFactory(transferResponse);//key rule of Functional programming
        GatewayCaller gatewayCaller = new GatewayCaller(newTransferResponse, coreGateway);
        Future<TrackIssueDTO> trackIssueDTOFuture = executorService.submit(gatewayCaller);
        try {
            TrackIssueDTO trackIssueDTO = trackIssueDTOFuture.get(PropertyConstants.getTimeOutInMili(), TimeUnit.MILLISECONDS);
            newTransferResponse.addResponseStatus(ResponseStatus.SUCCESS);
            newTransferResponse.setCoreGatewayTimeout(false);
            return newTransferResponse;
        } catch (InterruptedException |ExecutionException e) {// in case of 4xx or 5xx this block will be executed
            newTransferResponse.addResponseStatus(ResponseStatus.FAILURE);
            newTransferResponse.setCoreGatewayTimeout(false);
            return newTransferResponse;
        } catch (TimeoutException e) {
            newTransferResponse.setCoreGatewayTimeout(true);
            newTransferResponse.incrementRetry();
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
            TrackIssueDTO trackIssueDTO = coreGateway.trackStatus(transferRequest.getCorrelationId());
            return trackIssueDTO;
        }
    }

}
