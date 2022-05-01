package ir.sadeqcloud.processor.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("constants")
public class PropertyConstants {
    private static String inputTopic;
    public PropertyConstants(@Value("${kafka.input.topic}")String inputTopic){
        PropertyConstants.inputTopic=inputTopic;
    }
    public static String getInputTopic(){
        return inputTopic;
    }
}
