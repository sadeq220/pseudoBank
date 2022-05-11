package ir.sadeqcloud.gateway.sharedResource;

import ir.sadeqcloud.gateway.customExc.ClientResponseException;
import ir.sadeqcloud.gateway.model.ResponseStatus;
import ir.sadeqcloud.gateway.model.TransferResponse;
import ir.sadeqcloud.gateway.model.client.ClientFailureResponse;
import ir.sadeqcloud.gateway.model.client.ClientResponse;
import ir.sadeqcloud.gateway.model.client.ClientSuccessfulResponse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class IntermediaryObject {
    private String accountNo;
    private String correlationId;
    /**
     * because of "spurious wakeup" leads to "while loop condition check" we didn't use wait() and notify()
     *
     * BlockingQueue implementations are designed to be used primarily for producer-consumer queues
     */
    private BlockingQueue<TransferResponse> blockingQueue=new ArrayBlockingQueue<>(1);

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    public void putTransferResponse(TransferResponse transferResponse){
        try {
            blockingQueue.put(transferResponse);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public ClientResponse processTransferResponse(){
        try {
            TransferResponse transferResponseByKafkaConsumer = blockingQueue.poll(3_000, TimeUnit.MILLISECONDS);
            ResponseStatus[] responseStatuses = transferResponseByKafkaConsumer.getResponseStatuses().toArray(new ResponseStatus[]{});
            if (responseStatuses.length == 1 && responseStatuses[0]== ResponseStatus.SUCCESS)
                return new ClientSuccessfulResponse(correlationId,accountNo);
            return new ClientFailureResponse(correlationId,accountNo,responseStatuses);
        } catch (InterruptedException e) {
        throw new ClientResponseException("result not determined.check the status of your CorrelationId:"+correlationId );
        }
    }
}
