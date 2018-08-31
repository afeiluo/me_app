package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TUserAccount")
public class TUserAccount
{
    public TUserAccount() {
    }

    private long uid;

    @ThriftField(value=1, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private TCurrencyType currencyType;

    @ThriftField(value=2, name="currencyType")
    public TCurrencyType getCurrencyType() { return currencyType; }

    @ThriftField
    public void setCurrencyType(final TCurrencyType currencyType) { this.currencyType = currencyType; }

    private long amount;

    @ThriftField(value=3, name="amount")
    public long getAmount() { return amount; }

    @ThriftField
    public void setAmount(final long amount) { this.amount = amount; }

    private long freezed;

    @ThriftField(value=4, name="freezed")
    public long getFreezed() { return freezed; }

    @ThriftField
    public void setFreezed(final long freezed) { this.freezed = freezed; }

    private int appid;

    @ThriftField(value=5, name="appid")
    public int getAppid() { return appid; }

    @ThriftField
    public void setAppid(final int appid) { this.appid = appid; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("uid", uid)
            .add("currencyType", currencyType)
            .add("amount", amount)
            .add("freezed", freezed)
            .add("appid", appid)
            .toString();
    }
}
