package ir.sadeqcloud.processor.model;

import java.math.BigDecimal;

public class WithdrawLimitation {
    private String correlationId;
    private BigDecimal amount;;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
