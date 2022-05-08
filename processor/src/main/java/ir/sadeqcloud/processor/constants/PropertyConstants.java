package ir.sadeqcloud.processor.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("constants")
public class PropertyConstants {
    private static String inputTopic;
    private static String coreBankAddress;
    private static Long timeOutInMili;
    private static String normalOutputTopic;
    private static Integer maxRetryOnCoreGatewayTimeout;
    public PropertyConstants(@Value("${kafka.input.topic}")String inputTopic,
                             @Value("${core.bank.address}")String coreBankAddress,
                             @Value("${time.out.in.mili}")Long timeOutInMili,
                             @Value("kafka.normal.outptu")String normalOutputTopic,
                             @Value("max.retry.on.core.gateway.timeout")Integer maxRetryOnCoreGatewayTimeout){
        PropertyConstants.inputTopic=inputTopic;
        PropertyConstants.coreBankAddress=coreBankAddress;
        PropertyConstants.timeOutInMili=timeOutInMili;
        PropertyConstants.normalOutputTopic=normalOutputTopic;
        PropertyConstants.maxRetryOnCoreGatewayTimeout=maxRetryOnCoreGatewayTimeout;
    }
    public static String getInputTopic(){
        return inputTopic;
    }

    public static String getCoreBankAddress(){
        return coreBankAddress;}

    public static Long getTimeOutInMili(){
        return timeOutInMili;
    }
    public static String getNormalOutputTopic(){
        return normalOutputTopic;
    }
    public static Integer getMaxRetryOnCoreGatewayTimeout(){
        return maxRetryOnCoreGatewayTimeout;
    }
}
