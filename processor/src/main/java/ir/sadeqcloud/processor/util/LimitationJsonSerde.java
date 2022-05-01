package ir.sadeqcloud.processor.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import ir.sadeqcloud.processor.model.WithdrawLimitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LimitationJsonSerde {
    private ObjectMapper jacksonObjectMapper;
    @Autowired
    public LimitationJsonSerde(ObjectMapper objectMapper){
        this.jacksonObjectMapper=objectMapper;
    }
    public String serializeWithdrawModel(WithdrawLimitation withdrawLimitation){
        if (withdrawLimitation==null)
            return null;
        if (withdrawLimitation.getCorrelationId()==null)
            return "";
        try {
            return jacksonObjectMapper.writeValueAsString(withdrawLimitation);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;//TODO throw serialization exception
        }
    }

    public WithdrawLimitation deserializeWithdrawModel(String withdrawAsString){
        if (withdrawAsString==null)
            return null;
        if (withdrawAsString.isBlank())
            return new WithdrawLimitation();
        try {
            return jacksonObjectMapper.readValue(withdrawAsString.getBytes(StandardCharsets.UTF_8), WithdrawLimitation.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;//TODO throw deserialization exception
        }
    }

}
