package ir.sadeqcloud.processor.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ir.sadeqcloud.processor.exception.BusinessException;
import org.springframework.util.ReflectionUtils;

import java.util.*;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class TransferResponse extends TransferRequest{

    private Set<ResponseStatus> responseStatuses=new LinkedHashSet<>();
    @JsonIgnore
    private Boolean coreGatewayTimeout=false;
    @JsonIgnore
    private Integer timesOnRetry=0;//it don't need AtomicInteger

    protected TransferResponse(){
        //empty constructor to comply with POJO
    }
    @JsonGetter
    public String getCorrelationId() {
        return super.getCorrelationId();
    }
    @JsonGetter
    public Set<ResponseStatus> getResponseStatuses(){
        return Collections.unmodifiableSet(responseStatuses);
    }
    public void setCoreGatewayTimeout(Boolean timeout){
        this.coreGatewayTimeout=timeout;
    }

    public Boolean getCoreGatewayTimeout() {
        return coreGatewayTimeout;
    }
    public void incrementRetry(){
        timesOnRetry++;
    }
    public Integer getTimesOnRetry(){
        return timesOnRetry;
    }
    public void addResponseStatus(ResponseStatus responseStatus){
     responseStatuses.add(responseStatus);
    }
    public static TransferResponse builderFactory(TransferRequest transferRequest){
        if (transferRequest==null)
            return null;
        TransferResponse transferResponse = new TransferResponse();
        ReflectionUtils.shallowCopyFieldState(transferRequest,transferResponse);
        return transferResponse;
    }
}
