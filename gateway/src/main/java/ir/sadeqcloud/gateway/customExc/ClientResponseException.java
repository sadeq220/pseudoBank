package ir.sadeqcloud.gateway.customExc;
//TODO handle this Exception
public class ClientResponseException extends RuntimeException  {
    public ClientResponseException(String message){
        super(message);
    }
}
