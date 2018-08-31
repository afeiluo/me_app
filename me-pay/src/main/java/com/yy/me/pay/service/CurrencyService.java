package com.yy.me.pay.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.pay.entity.ExchangeConfig;
import com.yy.me.pay.entity.UserExchangeHistory;
import com.yy.me.pay.entity.UserIncomeHistory;
import com.yy.me.pay.entity.UserWithdrawHistory;
import com.yy.tinytimes.thrift.turnover.currency.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by Chris on 16/4/5.
 */
@Service
public class CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    @Autowired
    @Qualifier("turnOverCurrencyThriftClient")
    private ReferenceFactory<TCurrencyService> thriftFactory;

    @Autowired
    private MetricsClient metricsClient;

    private TCurrencyService getClient() {
        return thriftFactory.getClient();
    }

    public Map<TCurrencyType, Long> getUserAccountInfo(long uid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            Map<TCurrencyType, Long> userAccountMap = Maps.newHashMap();

            Map<TCurrencyType, TUserAccount> accountInfoMap = getClient().getUserAccountMapByUidAndAppIdForTinyTime(uid, TAppId.TINY_TIME);
            if (accountInfoMap == null || accountInfoMap.isEmpty()) {
                logger.info("Got empty account info response from turnover system for user: " + uid);
                return userAccountMap;
            }

            // 获取现金账户余额
            TUserAccount account = accountInfoMap.get(TCurrencyType.TINY_TIME__PROFIT);
            if (account != null) {
                userAccountMap.put(TCurrencyType.TINY_TIME__PROFIT, account.getAmount());
            }

            // 获取累计E豆收益
            account = accountInfoMap.get(TCurrencyType.TINY_TIME__EDOU);
            if (account != null) {
                userAccountMap.put(TCurrencyType.TINY_TIME__EDOU, account.getAmount());
            }

            // 获取M币余额
            account = accountInfoMap.get(TCurrencyType.TINY_TIME__MI_BI);
            if (account != null) {
                userAccountMap.put(TCurrencyType.TINY_TIME__MI_BI, account.getAmount());
            }

            // 获取累计现金收益
            account = accountInfoMap.get(TCurrencyType.TINY_TIME__MI_DOU);
            if (account != null) {
                userAccountMap.put(TCurrencyType.TINY_TIME__MI_DOU, account.getAmount());
            }

            logger.info("Got user account info from turnover system. uid: {}, account: {}", uid, StringUtils.join(userAccountMap, ", "));

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getUserAccountInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return userAccountMap;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getUserAccountInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover get user account info request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public List<UserIncomeHistory> queryIncomeHistory(long uid, int limit, long lastId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<TRevenueRecord> revenueRecords = getClient().queryRevenueRecord2(uid, TAppId.TINY_TIME, limit, lastId);

            List<UserIncomeHistory> historyList = Lists.newArrayList();
            if (revenueRecords != null && !revenueRecords.isEmpty()) {
                for (TRevenueRecord record : revenueRecords) {
                    UserIncomeHistory history = new UserIncomeHistory();
                    history.setRecId(record.getId());
                    history.setBeans(record.getIncome());

                    double cashIncome = record.getRealIncome();
                    if (cashIncome > 0) {
                        BigDecimal decimal = BigDecimal.valueOf(cashIncome / 100.0);
                        decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
                        cashIncome = decimal.doubleValue();
                    }
                    history.setCash(cashIncome);

                    history.setRecTime(record.getRevenueDate());

                    historyList.add(history);
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryIncomeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return historyList;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryIncomeHistory", System.currentTimeMillis() -  start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover income history request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public List<UserWithdrawHistory> queryWithdrawHistory(long uid, int limit, long lastId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<TMonthSettleApply> withdrawRecords = getClient().queryUserMonthSettleApply2(uid, TAppId.TINY_TIME, limit, lastId);

            List<UserWithdrawHistory> historyList = Lists.newArrayList();
            if (withdrawRecords != null && !withdrawRecords.isEmpty()) {
                for (TMonthSettleApply record : withdrawRecords) {
                    UserWithdrawHistory history = new UserWithdrawHistory();
                    history.setRecId(record.getId());
                    history.setAmount(record.getExchangeSalaryAmount());
                    history.setRecvTime(record.getApplyTime());
                    if (record.getResult() == 0) {
                        history.setStatus(1);
                    } else if (record.getResult() == 1) {
                        history.setStatus(2);
                    } else if (record.getResult() == 2) {
                        history.setStatus(3);
                    } else {
                        history.setStatus(3);
                    }

                    historyList.add(history);
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryWithdrawHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return historyList;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryWithdrawHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover withdraw history request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public List<UserExchangeHistory> queryExchangeHistory(long uid, int limit, long lastId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<TUserAccountHistory> exchangeRecords = getClient().queryUserAccountHistory(uid, TAppId.TINY_TIME, TCurrencyType.TINY_TIME__MI_BI, TAccountOperateType.EXCHANGE, limit, lastId);

            List<UserExchangeHistory> historyList = Lists.newArrayList();
            if (exchangeRecords != null && !exchangeRecords.isEmpty()) {
                for (TUserAccountHistory record : exchangeRecords) {
                    UserExchangeHistory history = new UserExchangeHistory();
                    history.setRecId(record.getId());
                    history.setAmount(record.getAmountChange());
                    history.setBuyTime(record.getOptTime());

                    historyList.add(history);
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryExchangeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return historyList;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryExchangeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover exchange history request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public List<ExchangeConfig> queryExchangeConfigList() throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<TExchangeCurrencyConfig> configRecords = getClient().getExchangeCurrencyConfigList(TAppId.TINY_TIME);

            List<ExchangeConfig> configList = Lists.newArrayList();
            if (configRecords != null && !configRecords.isEmpty()) {
                for (TExchangeCurrencyConfig record : configRecords) {
                    ExchangeConfig config = new ExchangeConfig();
                    config.setItemId(record.getId());
                    config.setMbi(record.getDestAmount());
                    config.setCash(record.getSrcAmount() / 100.0);

                    configList.add(config);
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryExchangeConfigList", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return configList;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryExchangeConfigList", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover exchange config list request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public boolean exchange(long uid, long configId, String userIp, UsedChannelType channel) throws Exception {
        long start = System.currentTimeMillis();

        try {
            TExchangeCurrencyResult exchangeResult = getClient().exchangeUserCurrency(uid, TAppId.TINY_TIME, 0, TCurrencyType.TINY_TIME__PROFIT, TCurrencyType.TINY_TIME__MI_BI, userIp, channel, configId);

            if (exchangeResult == null) {
                logger.warn("Fail to exchange to turnover system and got empty response. uid: {}, configId: {}", uid, configId);
                return false;
            }

            logger.info("Exchange result from turnover system: " + exchangeResult);

            if (exchangeResult.getResult() != 1) {
                logger.warn("Fail to exchange to turnover system. uid: {}, configId: {}", uid, configId);
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return true;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover exchange request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    /**
     * M币兑换Y币
     * @param seqId 兑换流水号(流水号不能冲突，否则会抛异常)
     * @param uid 用户UID
     * @param amount 金额
     * @param yy 提取YY号
     * @return 错误码:(只有正常在才返回1，其他错误码都需通过TServiceException的code获取)
     *  1    正常
     *  -500 服务端异常
     *  -400 参数错误
     *  -405 流水号已存在
     *  -22  帐号不存在
     *  -23  余额不足
     */
    public int m2y(String seqId, long uid, long amount, String yy) throws Exception {
        long start = System.currentTimeMillis();
        int result = 0;
        String message = "success";
        try {
            result = getClient().modifyUserAccountWithSeqId(uid, TAppId.TINY_TIME, TCurrencyType.TINY_TIME__MI_BI, -amount, TAccountOperateType.PAY_VIP_ROOM, yy, seqId);

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "m2y", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return result;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "m2y", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            if(e instanceof TServiceException){
                result = ((TServiceException) e).getCode();
                message = e.getMessage();

                return result;
            }

            throw e;
        } finally {
            logger.info("Processed turnover m2y request. result: {}, message:{}, uid: {}, amount: {}, seqId: {}, cost time: {}ms", result, message, uid, amount, seqId, (System.currentTimeMillis() - start));
        }
    }
}
