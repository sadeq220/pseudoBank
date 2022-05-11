package ir.sadeqcloud.processor.service.operations.RedisOperation;

import ir.sadeqcloud.processor.model.LimitationKeyPrefix;
import ir.sadeqcloud.processor.redis.RedisDao;
import ir.sadeqcloud.processor.service.operations.RedisOperation.AbstractRedisDataStoreOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RedisOperations extends AbstractRedisDataStoreOperations {
    private RedisDao redisDao;
    private BigDecimal bankLimitation;
    private BigDecimal branchLimitation;
    private BigDecimal accountLimitation;
    @Autowired
    public RedisOperations(RedisDao redisDao,
                           @Value("${account.limitation}") BigDecimal accountLimitation,
                           @Value("${branch.limitation}") BigDecimal branchLimitation,
                           @Value("${bank.limitation}") BigDecimal bankLimitation){
        this.redisDao=redisDao;
        this.bankLimitation=bankLimitation;
        this.branchLimitation=branchLimitation;
        this.accountLimitation=accountLimitation;
    }

    @Override
    protected RedisDao getRedisDao() {
        return this.redisDao;
    }

    @Override
    protected BigDecimal getLimitation(LimitationKeyPrefix keyPrefix) {
        switch (keyPrefix){
            case ACCOUNT:
                return accountLimitation;
            case BRANCH:
                return branchLimitation;
            case BANK:
                return bankLimitation;
            default:
                throw new RuntimeException("supplied LimitationKeyPrefix not supported"+keyPrefix);//TODO write custom exception
        }
    }
}
