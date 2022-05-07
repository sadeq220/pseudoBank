package ir.sadeqcloud.processor.service.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.service.gateway.dto.Operation;

import java.math.BigDecimal;

public class IssueRequest {

    private String accountNo;
    private BigDecimal amount;
    private Operation operation;
    @JsonIgnore
    private String correlationId;

    protected IssueRequest(){
        //empty constructor to comply with POJO
    }
    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public static IssueRequest builderFactory(TransferRequest transferRequest){
        IssueRequest issueRequest = new IssueRequest();
        issueRequest.accountNo=transferRequest.getAccountNo();
        issueRequest.amount=transferRequest.getAmount();
        issueRequest.correlationId=transferRequest.getCorrelationId();
        issueRequest.operation=Operation.WITHDRAW;//withdraw only supported
        return issueRequest;
    }
}
