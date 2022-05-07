package ir.sadeqcloud.processor.service.operations;

import ir.sadeqcloud.processor.exception.BusinessException;
import ir.sadeqcloud.processor.model.RequestType;
import ir.sadeqcloud.processor.model.TransferRequest;
import ir.sadeqcloud.processor.model.WithdrawLimitation;
import ir.sadeqcloud.processor.redis.RedisDao;
import ir.sadeqcloud.processor.redis.LimitationKeyPrefix;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public abstract class AbstractRedisDataStoreOperations implements DataStoreOperations {

    protected abstract RedisDao getRedisDao();
    protected abstract BigDecimal getLimitation();

    public Boolean checkWithdrawLimitationThresholdNotPassed(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix) {
        if (!(transferRequest.getRequestType()== RequestType.PROCEED_WITHDRAW))
            throw new BusinessException("this transferRequest is not for WITHDRAW",transferRequest.getCorrelationId());

        WithdrawLimitation recentWithdrawLimitation = transferRequest.buildLimitationModel();
        List<WithdrawLimitation> accountWithdraws = getRedisDao().getWithdraws(keyPrefix,transferRequest.getKeyIdentifier(keyPrefix));
        //add recent withdraw
        accountWithdraws.add(recentWithdrawLimitation);

        Optional<BigDecimal> optionalAggregatedAmountOfWithdraws = accountWithdraws.stream().map(WL -> WL.getAmount()).reduce((fA, sA) -> fA.add(sA));
        // return true if there is no transaction on keyIdentifier
        if (!optionalAggregatedAmountOfWithdraws.isPresent())
            return true;
        // if limitation > aggregatedAmountOfWithdraws return true
        return getLimitation().compareTo(optionalAggregatedAmountOfWithdraws.get())>0;
    }

    public void reverseWithdraw(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix) {
        if (!(transferRequest.getRequestType() == RequestType.REVERSE))
            throw new BusinessException("this transferRequest in not for REVERSE ",transferRequest.getCorrelationId());

        String keyIdentifier = transferRequest.getKeyIdentifier(keyPrefix);

        List<WithdrawLimitation> recentAccountWithdraws = getRedisDao().getWithdraws(keyPrefix, keyIdentifier);
        WithdrawLimitation withdrawLimitation = transferRequest.buildLimitationModel();

        if (!recentAccountWithdraws.contains(withdrawLimitation))
            throw new BusinessException("reverse transferRequest ,provided correlationId doesNot exist",transferRequest.getCorrelationId());
        // we must get actual withdrawLimitation from redis cause our provided withdrawLimitation has amount of null
        int indexOfActualWithdrawLimitation = recentAccountWithdraws.indexOf(withdrawLimitation);
        WithdrawLimitation actualWithdrawLimitation = recentAccountWithdraws.get(indexOfActualWithdrawLimitation);
        // delete WithdrawLimitation from redis
        getRedisDao().removeWithdrawLimitation(keyPrefix, keyIdentifier,actualWithdrawLimitation);
    }

    public void addSuccessfulWithdrawLimitation(TransferRequest transferRequest, LimitationKeyPrefix keyPrefix) {
        getRedisDao().addWithdrawLimitation(keyPrefix,transferRequest);
    }
}
