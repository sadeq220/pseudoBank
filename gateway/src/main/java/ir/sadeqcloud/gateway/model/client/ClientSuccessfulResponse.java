package ir.sadeqcloud.gateway.model.client;

public class ClientSuccessfulResponse implements ClientResponse{

    private String correlationId;
    private String accountNo;

    public ClientSuccessfulResponse(String correlationId, String accountNo) {
        this.correlationId = correlationId;
        this.accountNo = accountNo;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getAccountNo() {
        return accountNo;
    }
}
