package ir.sadeqcloud.processor.redis;

import ir.sadeqcloud.processor.model.WithdrawLimitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDao {
    private StringRedisTemplate stringRedisTemplate;
    /**
     * In Redis, a list is a collection of strings sorted by insertion order, similar to linked lists.
     *  Redis reads lists from left to right,
     *  and you can add new list elements to the head of a list (the “left” end) with the lpush command or the tail (the “right” end) with rpush
     */
    private ListOperations<String, String> stringListOperations;
    @Autowired
    public RedisDao(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
        stringListOperations = stringRedisTemplate.opsForList();
    }
    public void addAccountWithdrawLimitation(WithdrawLimitation withdrawLimitation){

    }
}
