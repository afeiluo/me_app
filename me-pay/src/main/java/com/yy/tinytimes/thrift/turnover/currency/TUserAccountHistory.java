package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TUserAccountHistory")
public class TUserAccountHistory
{
    public TUserAccountHistory() {
    }

    private long uid;

    @ThriftField(value=1, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private long accountId;

    @ThriftField(value=2, name="accountId")
    public long getAccountId() { return accountId; }

    @ThriftField
    public void setAccountId(final long accountId) { this.accountId = accountId; }

    private TCurrencyType currencyType;

    @ThriftField(value=3, name="currencyType")
    public TCurrencyType getCurrencyType() { return currencyType; }

    @ThriftField
    public void setCurrencyType(final TCurrencyType currencyType) { this.currencyType = currencyType; }

    private long amountOrig;

    @ThriftField(value=4, name="amountOrig")
    public long getAmountOrig() { return amountOrig; }

    @ThriftField
    public void setAmountOrig(final long amountOrig) { this.amountOrig = amountOrig; }

    private long amountChange;

    @ThriftField(value=5, name="amountChange")
    public long getAmountChange() { return amountChange; }

    @ThriftField
    public void setAmountChange(final long amountChange) { this.amountChange = amountChange; }

    private long freezedOrig;

    @ThriftField(value=6, name="freezedOrig")
    public long getFreezedOrig() { return freezedOrig; }

    @ThriftField
    public void setFreezedOrig(final long freezedOrig) { this.freezedOrig = freezedOrig; }

    private long freezedChange;

    @ThriftField(value=7, name="freezedChange")
    public long getFreezedChange() { return freezedChange; }

    @ThriftField
    public void setFreezedChange(final long freezedChange) { this.freezedChange = freezedChange; }

    private long optTime;

    @ThriftField(value=8, name="optTime")
    public long getOptTime() { return optTime; }

    @ThriftField
    public void setOptTime(final long optTime) { this.optTime = optTime; }

    private String description;

    @ThriftField(value=9, name="description")
    public String getDescription() { return description; }

    @ThriftField
    public void setDescription(final String description) { this.description = description; }

    private TAccountOperateType optType;

    @ThriftField(value=10, name="optType")
    public TAccountOperateType getOptType() { return optType; }

    @ThriftField
    public void setOptType(final TAccountOperateType optType) { this.optType = optType; }

    private int appid;

    @ThriftField(value=11, name="appid")
    public int getAppid() { return appid; }

    @ThriftField
    public void setAppid(final int appid) { this.appid = appid; }

    private long id;

    @ThriftField(value=12, name="id")
    public long getId() { return id; }

    @ThriftField
    public void setId(final long id) { this.id = id; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("uid", uid)
            .add("accountId", accountId)
            .add("currencyType", currencyType)
            .add("amountOrig", amountOrig)
            .add("amountChange", amountChange)
            .add("freezedOrig", freezedOrig)
            .add("freezedChange", freezedChange)
            .add("optTime", optTime)
            .add("description", description)
            .add("optType", optType)
            .add("appid", appid)
            .add("id", id)
            .toString();
    }
}
