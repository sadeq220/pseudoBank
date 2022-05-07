package ir.sadeqcloud.processor.redis;

public enum RedisLimitationKeyPrefix {
    ACCOUNT("Account:"),
    BRANCH("Branch:"),
    BANK("Bank:");
    private String keyPrefix;
    RedisLimitationKeyPrefix(String keyPrefix){
        this.keyPrefix=keyPrefix;
    }
    public String getKeyPrefix(){
        return keyPrefix;
    }
}
