package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.WithdrawLimitation;
import ir.sadeqcloud.processor.redis.RedisDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountOperations {
    private RedisDao redisDao;
    private BigDecimal limitation;
    @Autowired
    public AccountOperations(RedisDao redisDao){
        this.redisDao=redisDao;
    }
    public Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest){
        WithdrawLimitation recentWithdrawLimitation = transferRequest.buildLimitationModel();
        List<WithdrawLimitation> accountWithdraws = redisDao.getAccountWithdraws(transferRequest.getAccountNo());
        //add recent withdraw
        accountWithdraws.add(recentWithdrawLimitation);

        Optional<BigDecimal> optionalAggregatedAmountOfWithdraws = accountWithdraws.stream().map(WL -> WL.getAmount()).reduce((fA, sA) -> fA.add(sA));
        // return true if there is no transaction on accountNo
        if (!optionalAggregatedAmountOfWithdraws.isPresent())
            return true;
        // if limitation > aggregatedAmountOfWithdraws return true
        return limitation.compareTo(optionalAggregatedAmountOfWithdraws.get())>0;
    }

}
