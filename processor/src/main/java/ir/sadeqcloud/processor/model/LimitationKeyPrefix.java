package ir.sadeqcloud.processor.model;

public enum LimitationKeyPrefix {
    ACCOUNT("Account:"),
    BRANCH("Branch:"),
    BANK("Bank:");
    private String keyPrefix;
    LimitationKeyPrefix(String keyPrefix){
        this.keyPrefix=keyPrefix;
    }
    public String getKeyPrefix(){
        return keyPrefix;
    }
}
