package ir.sadeqcloud.gateway.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class TransferResponse {
    private Set<ResponseStatus> responseStatuses=new LinkedHashSet<>();
    private String correlationId;

    public Set<ResponseStatus> getResponseStatuses() {
        return responseStatuses;
    }

    public void setResponseStatuses(Set<ResponseStatus> responseStatuses) {
        this.responseStatuses = responseStatuses;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
