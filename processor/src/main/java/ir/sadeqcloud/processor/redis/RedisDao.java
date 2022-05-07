package ir.sadeqcloud.processor.redis;

import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.WithdrawLimitation;
import ir.sadeqcloud.processor.util.LimitationJsonSerde;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RedisDao {
    private StringRedisTemplate stringRedisTemplate;
    private LimitationJsonSerde limitationJsonSerde;

    /**
     * In Redis, a list is a collection of strings sorted by insertion order, similar to linked lists.
     *  Redis reads lists from left to right,
     *  and you can add new list elements to the head of a list (the “left” end) with the lpush command or the tail (the “right” end) with rpush
     */
    private ListOperations<String, String> stringListOperations;
    @Autowired
    public RedisDao(StringRedisTemplate stringRedisTemplate,
                    LimitationJsonSerde limitationJsonSerde){
        this.stringRedisTemplate=stringRedisTemplate;
        stringListOperations = stringRedisTemplate.opsForList();
        this.limitationJsonSerde=limitationJsonSerde;

    }

    /**
     * add string json to head of the redis list
     */
    private void addWithdrawLimitation(RedisLimitationKeyPrefix keyPrefix,TransferRequest transferRequest,String keyIdentifier){
        String withdrawLimitModelAsString = limitationJsonSerde.serializeWithdrawModel(transferRequest.buildLimitationModel());
        stringListOperations.leftPush(keyPrefix.getKeyPrefix()+keyIdentifier,withdrawLimitModelAsString);
    }
    public void addAccountWithdrawLimitation(RedisLimitationKeyPrefix keyPrefix,TransferRequest transferRequest){
        addWithdrawLimitation(keyPrefix,transferRequest,transferRequest.getAccountNo());
    }

    /**
     * -1 index represents final element
     */
    public List<WithdrawLimitation> getWithdraws(RedisLimitationKeyPrefix keyPrefix,String keyIdentifier){
        List<String> withdrawAsStringList = stringListOperations.range(keyPrefix.getKeyPrefix()+ keyIdentifier, 0, -1);
        return withdrawAsStringList.stream().map(limitationJsonSerde::deserializeWithdrawModel).collect(Collectors.toList());
    }

    /**
     * reverse on Account
     */
    public Boolean removeWithdrawLimitation(RedisLimitationKeyPrefix keyPrefix,String keyIdentifier,WithdrawLimitation withdrawLimitation){
        String withdrawModelAsString = limitationJsonSerde.serializeWithdrawModel(withdrawLimitation);
        Long removedElements = stringListOperations.remove(keyPrefix.getKeyPrefix() + keyIdentifier, 1, withdrawModelAsString);
        return removedElements>0;
    }
}
