package ir.sadeqcloud.gateway.customExc;

import ir.sadeqcloud.gateway.model.ResponseStatus;

public class ClientResponseException extends RuntimeException  {
    private String correlationId;
    private ResponseStatus[] failureReasons;

    public ClientResponseException(String message){
        super(message);
    }
    public ClientResponseException(String message,String correlationId,ResponseStatus[] failureReasons){
        super(message);
        this.correlationId=correlationId;
        this.failureReasons=failureReasons;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public ResponseStatus[] getFailureReasons() {
        return failureReasons;
    }
}
