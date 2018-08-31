package com.yy.me.pay.service;

import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.entity.PaymentAccount;
import com.yy.me.metrics.MetricsClient;
import com.yy.tinytimes.thrift.turnover.settle.TServiceException;
import com.yy.tinytimes.thrift.turnover.settle.TSettleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.yy.me.http.BaseServletUtil.*;

@Service
public class SettleService {
    private static final Logger logger = LoggerFactory.getLogger(SettleService.class);

    private static final int APPID = 21;

    @Autowired
    @Qualifier("settleThriftClient")
    private ReferenceFactory<TSettleService> thriftFactory;

    @Autowired
    private MetricsClient metricsClient;

    private TSettleService getClient() {
        return thriftFactory.getClient();
    }

    /**
     *
     * @param payAccount
     * @param amount 单位为分
     * @return
     * @throws Exception
     */
    public int withdraw(PaymentAccount payAccount, long amount) throws Exception {
        long start = System.currentTimeMillis();

        int result = FAILED;
        try {
            Map<String, String> accountInfo = Maps.newHashMap();
            accountInfo.put("name", payAccount.getName());
            accountInfo.put("idCard", payAccount.getIdCard());

            String expand = getLocalObjMapper().writeValueAsString(accountInfo);

            int code = getClient().userMonthSettle(payAccount.getUid(), APPID, 0, amount, payAccount.getAccount(), 1,
                    expand);

            logger.info("Got withdraw response from turnover system: {}. account: {}, amount: {}", code, payAccount, amount);

            if (code == 1) {
                result = SUCCESS;
            } else if (code == -500) {
                result = FAILED;
            } else if (code == -1) {
                result = NOT_WITHDRAW_TIME;
            } else if (code == -2) {
                result = WITHDRAW_COUNT_LIMIT;
            } else if (code == -3) {
                result = WITHDRAW_LESS;
            } else if (code == -4) {
                result = BALANCE_NOT_ENOUGH;
            } else if (code == -5) {
                result = -16;  // 大于每日提现限制
            }
        } catch (TServiceException e) {
            logger.error("Withdraw error. code: " + e.getCode() + ", message: " + e.getMessage() + ", account: " + payAccount, e);

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "withdraw", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "withdraw", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Process turnover withdraw request. cost time: {}", (System.currentTimeMillis() - start));
        }

        metricsClient.report(MetricsClient.ProtocolType.THRIFT, "withdraw", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

        return result;
    }

}
