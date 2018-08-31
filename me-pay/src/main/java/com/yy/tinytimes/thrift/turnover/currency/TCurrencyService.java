package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Map;

@ThriftService("TCurrencyService")
public interface TCurrencyService
{
    @ThriftService("TCurrencyService")
    public interface Async
    {
        @ThriftMethod(value = "ping2")
        ListenableFuture<Void> ping2();

        @ThriftMethod(value = "getExchangeCurrencyConfigList")
        ListenableFuture<List<TExchangeCurrencyConfig>> getExchangeCurrencyConfigList(
                @ThriftField(value = 1, name = "appid") final TAppId appid
        );

        @ThriftMethod(value = "exchangeUserCurrency")
        ListenableFuture<TExchangeCurrencyResult> exchangeUserCurrency(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final TAppId appid,
                @ThriftField(value = 3, name = "amount") final long amount,
                @ThriftField(value = 4, name = "srcCurrencyType") final TCurrencyType srcCurrencyType,
                @ThriftField(value = 5, name = "destCurrencyType") final TCurrencyType destCurrencyType,
                @ThriftField(value = 6, name = "userIp") final String userIp,
                @ThriftField(value = 7, name = "usedChannel") final UsedChannelType usedChannel,
                @ThriftField(value = 8, name = "configId") final long configId
        );

        @ThriftMethod(value = "queryRevenueRecord2")
        ListenableFuture<List<TRevenueRecord>> queryRevenueRecord2(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final TAppId appid,
                @ThriftField(value = 3, name = "pagesize") final int pagesize,
                @ThriftField(value = 4, name = "lastId") final long lastId
        );

        @ThriftMethod(value = "queryUserAccountHistory")
        ListenableFuture<List<TUserAccountHistory>> queryUserAccountHistory(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final TAppId appid,
                @ThriftField(value = 3, name = "currencyType") final TCurrencyType currencyType,
                @ThriftField(value = 4, name = "optType") final TAccountOperateType optType,
                @ThriftField(value = 5, name = "pagesize") final int pagesize,
                @ThriftField(value = 6, name = "lastId") final long lastId
        );

        @ThriftMethod(value = "getUserAccountMapByUidAndAppIdForTinyTime")
        ListenableFuture<Map<TCurrencyType, TUserAccount>> getUserAccountMapByUidAndAppIdForTinyTime(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final TAppId appid
        );

        @ThriftMethod(value = "queryUserMonthSettleApply2")
        ListenableFuture<List<TMonthSettleApply>> queryUserMonthSettleApply2(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final TAppId appid,
                @ThriftField(value = 3, name = "pagesize") final int pagesize,
                @ThriftField(value = 4, name = "lastId") final long lastId
        );

        @ThriftMethod(value = "modifyUserAccountWithSeqId")
        ListenableFuture<Integer> modifyUserAccountWithSeqId(
                @ThriftField(value=1, name="uid") final long uid,
                @ThriftField(value=2, name="appid") final TAppId appid,
                @ThriftField(value=3, name="currencyType") final TCurrencyType currencyType,
                @ThriftField(value=4, name="deltaAmount") final long deltaAmount,
                @ThriftField(value=5, name="operateType") final TAccountOperateType operateType,
                @ThriftField(value=6, name="description") final String description,
                @ThriftField(value=7, name="seqid") final String seqid
        );
    }
    @ThriftMethod(value = "ping2")
    void ping2() throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "getExchangeCurrencyConfigList")
    List<TExchangeCurrencyConfig> getExchangeCurrencyConfigList(
            @ThriftField(value = 1, name = "appid") final TAppId appid
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "exchangeUserCurrency",
                  exception = {
                      @ThriftException(type=TServiceException.class, id=1)
                  })
    TExchangeCurrencyResult exchangeUserCurrency(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final TAppId appid,
            @ThriftField(value = 3, name = "amount") final long amount,
            @ThriftField(value = 4, name = "srcCurrencyType") final TCurrencyType srcCurrencyType,
            @ThriftField(value = 5, name = "destCurrencyType") final TCurrencyType destCurrencyType,
            @ThriftField(value = 6, name = "userIp") final String userIp,
            @ThriftField(value = 7, name = "usedChannel") final UsedChannelType usedChannel,
            @ThriftField(value = 8, name = "configId") final long configId
    ) throws TServiceException, com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "queryRevenueRecord2")
    List<TRevenueRecord> queryRevenueRecord2(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final TAppId appid,
            @ThriftField(value = 3, name = "pagesize") final int pagesize,
            @ThriftField(value = 4, name = "lastId") final long lastId
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "queryUserAccountHistory")
    List<TUserAccountHistory> queryUserAccountHistory(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final TAppId appid,
            @ThriftField(value = 3, name = "currencyType") final TCurrencyType currencyType,
            @ThriftField(value = 4, name = "optType") final TAccountOperateType optType,
            @ThriftField(value = 5, name = "pagesize") final int pagesize,
            @ThriftField(value = 6, name = "lastId") final long lastId
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "getUserAccountMapByUidAndAppIdForTinyTime")
    Map<TCurrencyType, TUserAccount> getUserAccountMapByUidAndAppIdForTinyTime(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final TAppId appid
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "queryUserMonthSettleApply2")
    List<TMonthSettleApply> queryUserMonthSettleApply2(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final TAppId appid,
            @ThriftField(value = 3, name = "pagesize") final int pagesize,
            @ThriftField(value = 4, name = "lastId") final long lastId
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "modifyUserAccountWithSeqId",
            exception = {
                    @ThriftException(type=TServiceException.class, id=1)
            })
    int modifyUserAccountWithSeqId(
            @ThriftField(value=1, name="uid") final long uid,
            @ThriftField(value=2, name="appid") final TAppId appid,
            @ThriftField(value=3, name="currencyType") final TCurrencyType currencyType,
            @ThriftField(value=4, name="deltaAmount") final long deltaAmount,
            @ThriftField(value=5, name="operateType") final TAccountOperateType operateType,
            @ThriftField(value=6, name="description") final String description,
            @ThriftField(value=7, name="seqid") final String seqid
    ) throws TServiceException, com.yy.cs.center.remoting.RemotingException;
}