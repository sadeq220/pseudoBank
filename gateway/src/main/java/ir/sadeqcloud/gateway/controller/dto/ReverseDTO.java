package ir.sadeqcloud.gateway.controller.dto;

import ir.sadeqcloud.gateway.model.RequestType;
import ir.sadeqcloud.gateway.model.RequesterRole;
import ir.sadeqcloud.gateway.model.TransferRequest;

public class ReverseDTO {
    private String correlationId;
    private RequesterRole requesterRole;
    private String accountNo;

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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    public TransferRequest buildModel(){
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setRequestType(RequestType.REVERSE);
        transferRequest.setCorrelationId(correlationId);
        transferRequest.setAccountNo(accountNo);
        transferRequest.setRequesterRole(requesterRole);
        return transferRequest;
    }
}
