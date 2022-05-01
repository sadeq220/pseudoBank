package ir.sadeqcloud.processor.service;

import ir.sadeqcloud.processor.exception.BusinessException;
import ir.sadeqcloud.processor.model.RequestType;
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
       if (!(transferRequest.getRequestType()==RequestType.PROCEED_WITHDRAW))
           throw new BusinessException("this transferRequest is not for WITHDRAW",transferRequest.getCorrelationId());
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
    public void reverseWithdraw(TransferRequest transferRequest){
    if (!(transferRequest.getRequestType() == RequestType.REVERSE))
        throw new BusinessException("this transferRequest in not for REVERSE ",transferRequest.getCorrelationId());
        List<WithdrawLimitation> recentAccountWithdraws = redisDao.getAccountWithdraws(transferRequest.getAccountNo());
        WithdrawLimitation withdrawLimitation = transferRequest.buildLimitationModel();
        if (!recentAccountWithdraws.contains(withdrawLimitation))
            throw new BusinessException("reverse transferRequset ,provided correlationId doesNot exist",transferRequest.getCorrelationId());
        // we must get actual withdrawLimitation from redis cause our provided withdrawLimitation has amount of null
        int indexOfActualWithdrawLimitation = recentAccountWithdraws.indexOf(withdrawLimitation);
        WithdrawLimitation actualWithdrawLimitation = recentAccountWithdraws.get(indexOfActualWithdrawLimitation);
        // delete WithdrawLimitation from redis
        redisDao.removeAccountWithdrawLimitation(transferRequest.getAccountNo(),actualWithdrawLimitation);
        //TODO request gateway to do reverse on core module
    }
}
