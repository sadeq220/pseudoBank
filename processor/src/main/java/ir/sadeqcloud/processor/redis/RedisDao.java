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
    private final String ACCOUNT_KEY_PREFIX="Account:";
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
    public void addAccountWithdrawLimitation(TransferRequest transferRequest){
        String withdrawLimitModelAsString = limitationJsonSerde.serializeWithdrawModel(transferRequest.buildLimitationModel());
        stringListOperations.leftPush(ACCOUNT_KEY_PREFIX+transferRequest.getAccountNo(),withdrawLimitModelAsString);
    }
    public List<WithdrawLimitation> getAccountWithdraws(String accountNo){
        List<String> withdrawAsStringList = stringListOperations.range(ACCOUNT_KEY_PREFIX + accountNo, 0, -1);
        return withdrawAsStringList.stream().map(limitationJsonSerde::deserializeWithdrawModel).collect(Collectors.toList());
    }
}
