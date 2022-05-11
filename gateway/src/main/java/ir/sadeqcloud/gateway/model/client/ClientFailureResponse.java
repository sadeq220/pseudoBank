package ir.sadeqcloud.gateway.model.client;

import ir.sadeqcloud.gateway.model.ResponseStatus;

public class ClientFailureResponse implements ClientResponse{
    private String correlationId;
    private String accountNo;
    private ResponseStatus[] failureReasons;

    public ClientFailureResponse(String correlationId, String accountNo, ResponseStatus[] failureReasons) {
        this.correlationId = correlationId;
        this.accountNo = accountNo;
        this.failureReasons = failureReasons;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getAccountNo() {
        return accountNo;
    }
    public ResponseStatus[] getFailureReasons(){
        return failureReasons;
    }
}
