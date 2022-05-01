package ir.sadeqcloud.processor.model;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.math.BigDecimal;

public class WithdrawLimitation {
    private String correlationId;
    private BigDecimal amount;
    public WithdrawLimitation(){ // default constructor for deserializer
     }
    public WithdrawLimitation(String correlationId,BigDecimal amount){
        this.amount=amount;
        this.correlationId=correlationId;
    }
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

    @Override
    public boolean equals(Object obj) {
        if (obj==this)
            return true;
        if (!(obj instanceof WithdrawLimitation))
            return false;
        WithdrawLimitation withdrawLimitation = (WithdrawLimitation) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(correlationId,withdrawLimitation.correlationId);
        return equalsBuilder.isEquals();
    }
}
