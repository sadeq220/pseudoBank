package ir.sadeqcloud.processor.model;

public class TransferResponse {

    private String correlationId;
    private ResponseStatus responseStatus;

    protected TransferResponse(){
        //empty constructor to comply with POJO
    }
    public TransferResponse(String correlationId,ResponseStatus responseStatus){
        this.correlationId=correlationId;
        this.responseStatus=responseStatus;
    }
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }
}
