package ir.sadeqcloud.processor.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessException extends RuntimeException{
    private static Logger logger= LoggerFactory.getLogger(BusinessException.class);
    private String message;
    private String correlationId;
    public BusinessException(String message,String correlationId){
        super(message);
        this.message=message;
        this.correlationId=correlationId;
        logger.error("correlationId: {} goes wrong, message is {}",correlationId,message);
    }
    public String getCustomMessage(){
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
