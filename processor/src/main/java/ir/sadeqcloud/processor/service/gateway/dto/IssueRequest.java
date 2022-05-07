package ir.sadeqcloud.processor.service.gateway.dto;

import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.service.gateway.dto.Operation;

import java.math.BigDecimal;

public class IssueRequest {

    private String accountNo;
    private BigDecimal amount;
    private Operation operation;

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

    public static IssueRequest builderFactory(TransferRequest transferRequest){
        IssueRequest issueRequest = new IssueRequest();
        issueRequest.accountNo=transferRequest.getAccountNo();
        issueRequest.amount=transferRequest.getAmount();
        issueRequest.operation=Operation.WITHDRAW;//withdraw only supported
        return issueRequest;
    }
}
