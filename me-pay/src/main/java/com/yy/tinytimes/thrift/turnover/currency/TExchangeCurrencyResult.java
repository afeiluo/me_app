package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TExchangeCurrencyResult")
public class TExchangeCurrencyResult
{
    public TExchangeCurrencyResult() {
    }

    private long uid;

    @ThriftField(value=1, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private TAppId appid;

    @ThriftField(value=2, name="appid")
    public TAppId getAppid() { return appid; }

    @ThriftField
    public void setAppid(final TAppId appid) { this.appid = appid; }

    private TCurrencyType srcCurrencyType;

    @ThriftField(value=3, name="srcCurrencyType")
    public TCurrencyType getSrcCurrencyType() { return srcCurrencyType; }

    @ThriftField
    public void setSrcCurrencyType(final TCurrencyType srcCurrencyType) { this.srcCurrencyType = srcCurrencyType; }

    private TCurrencyType destCurrencyType;

    @ThriftField(value=4, name="destCurrencyType")
    public TCurrencyType getDestCurrencyType() { return destCurrencyType; }

    @ThriftField
    public void setDestCurrencyType(final TCurrencyType destCurrencyType) { this.destCurrencyType = destCurrencyType; }

    private int result;

    @ThriftField(value=5, name="result")
    public int getResult() { return result; }

    @ThriftField
    public void setResult(final int result) { this.result = result; }

    private TUserAccount srcAccount;

    @ThriftField(value=6, name="srcAccount")
    public TUserAccount getSrcAccount() { return srcAccount; }

    @ThriftField
    public void setSrcAccount(final TUserAccount srcAccount) { this.srcAccount = srcAccount; }

    private TUserAccount destAccount;

    @ThriftField(value=7, name="destAccount")
    public TUserAccount getDestAccount() { return destAccount; }

    @ThriftField
    public void setDestAccount(final TUserAccount destAccount) { this.destAccount = destAccount; }

    private long exchangeAmount;

    @ThriftField(value=8, name="exchangeAmount")
    public long getExchangeAmount() { return exchangeAmount; }

    @ThriftField
    public void setExchangeAmount(final long exchangeAmount) { this.exchangeAmount = exchangeAmount; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("uid", uid)
            .add("appid", appid)
            .add("srcCurrencyType", srcCurrencyType)
            .add("destCurrencyType", destCurrencyType)
            .add("result", result)
            .add("srcAccount", srcAccount)
            .add("destAccount", destAccount)
            .add("exchangeAmount", exchangeAmount)
            .toString();
    }
}
