package ir.sadeqcloud.processor.model;

import ir.sadeqcloud.processor.exception.BusinessException;
import ir.sadeqcloud.processor.redis.RedisLimitationKeyPrefix;

import java.math.BigDecimal;

public class TransferRequest {

    private RequesterRole requesterRole;
    private BigDecimal amount;
    private String accountNo;
    private String correlationId;
    private RequestType requestType;
    private String branchNo;
    private String bankNo;

    public RequesterRole getRequesterRole() {
        return requesterRole;
    }

    public void setRequesterRole(RequesterRole requesterRole) {
        this.requesterRole = requesterRole;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

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

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getBranchNo() {
        return branchNo;
    }

    public void setBranchNo(String branchNo) {
        this.branchNo = branchNo;
    }

    public String getBankNo() {
        return bankNo;
    }

    public void setBankNo(String bankNo) {
        this.bankNo = bankNo;
    }

    public String getKeyIdentifier(RedisLimitationKeyPrefix keyPrefix){
        switch (keyPrefix){
            case ACCOUNT:
                return accountNo;
            case BANK:
                return bankNo;
            case BRANCH:
                return branchNo;
            default:
                throw new BusinessException(keyPrefix.getKeyPrefix()+"key not supported",correlationId);
        }
    }
    public WithdrawLimitation buildLimitationModel(){
        return new WithdrawLimitation(correlationId,amount);
    }
}
