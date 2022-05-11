package ir.sadeqcloud.gateway.controller.dto;

import ir.sadeqcloud.gateway.model.RequestType;
import ir.sadeqcloud.gateway.model.RequesterRole;
import ir.sadeqcloud.gateway.model.TransferRequest;

import java.math.BigDecimal;

public class WithdrawTransferDTO {
    private String accountNo;
    private String correlationId;
    private RequesterRole requesterRole;
    private BigDecimal amount;
    private String branchNo;
    private String bankNo;

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

    public TransferRequest buildModel(){
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setRequestType(RequestType.PROCEED_WITHDRAW);
        transferRequest.setAccountNo(accountNo);
        transferRequest.setCorrelationId(correlationId);
        transferRequest.setAmount(amount);
        transferRequest.setRequesterRole(requesterRole);
        transferRequest.setBankNo(bankNo);
        transferRequest.setBranchNo(branchNo);
        return transferRequest;
    }
}
