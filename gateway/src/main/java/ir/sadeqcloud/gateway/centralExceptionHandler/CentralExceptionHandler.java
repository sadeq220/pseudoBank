package ir.sadeqcloud.gateway.centralExceptionHandler;

import ir.sadeqcloud.gateway.customExc.ClientResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@RestControllerAdvice
public class CentralExceptionHandler extends ResponseEntityExceptionHandler {
   @ExceptionHandler(ClientResponseException.class)
   public ResponseEntity renderClientResponseOnFailure(ClientResponseException clientResponseException){
       if (clientResponseException.getFailureReasons()==null)
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message",clientResponseException.getMessage()));
       Map<String,Object> map=new HashMap<>();
       map.put("correlationId",clientResponseException.getCorrelationId());
       map.put("failureReasons",clientResponseException.getFailureReasons());
       map.put("message",clientResponseException.getMessage());
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
   }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        Properties errors=new Properties();
        for (FieldError fe : fieldErrors){
            String message=fe.getDefaultMessage();
            String fieldName=fe.getField();
            errors.put(fieldName,message);
        }
        return ResponseEntity.status(status).body(errors);
    }
}
