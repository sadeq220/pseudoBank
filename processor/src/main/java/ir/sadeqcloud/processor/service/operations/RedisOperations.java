package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.redis.RedisDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RedisOperations extends AbstractRedisDataStoreOperations {
    private RedisDao redisDao;
    private BigDecimal limitation;
    @Autowired
    public RedisOperations(RedisDao redisDao){
        this.redisDao=redisDao;
    }

    @Override
    protected RedisDao getRedisDao() {
        return this.redisDao;
    }

    @Override
    protected BigDecimal getLimitation() {
        return this.limitation;
    }
}
