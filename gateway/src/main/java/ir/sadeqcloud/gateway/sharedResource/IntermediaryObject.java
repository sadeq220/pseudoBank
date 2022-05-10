package ir.sadeqcloud.gateway.sharedResource;
//TODO add wait() and notify()
public class IntermediaryObject {
    private String accountNo;
    private String CorrelationId;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCorrelationId() {
        return CorrelationId;
    }

    public void setCorrelationId(String correlationId) {
        CorrelationId = correlationId;
    }
}
