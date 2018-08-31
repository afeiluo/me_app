package com.yy.tinytimes.thrift.turnover.props;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TWeekPropsRecvInfo")
public class TWeekPropsRecvInfo
{
    public TWeekPropsRecvInfo() {
    }

    private long uid;

    @ThriftField(value=1, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private int propId;

    @ThriftField(value=2, name="propId")
    public int getPropId() { return propId; }

    @ThriftField
    public void setPropId(final int propId) { this.propId = propId; }

    private String propName;

    @ThriftField(value=3, name="propName")
    public String getPropName() { return propName; }

    @ThriftField
    public void setPropName(final String propName) { this.propName = propName; }

    private int pricingId;

    @ThriftField(value=4, name="pricingId")
    public int getPricingId() { return pricingId; }

    @ThriftField
    public void setPricingId(final int pricingId) { this.pricingId = pricingId; }

    private double amount;

    @ThriftField(value=5, name="amount")
    public double getAmount() { return amount; }

    @ThriftField
    public void setAmount(final double amount) { this.amount = amount; }

    private long usedTime;

    @ThriftField(value=6, name="usedTime")
    public long getUsedTime() { return usedTime; }

    @ThriftField
    public void setUsedTime(final long usedTime) { this.usedTime = usedTime; }

    private long sid;

    @ThriftField(value=7, name="sid")
    public long getSid() { return sid; }

    @ThriftField
    public void setSid(final long sid) { this.sid = sid; }

    private int propCnt;

    @ThriftField(value=8, name="propCnt")
    public int getPropCnt() { return propCnt; }

    @ThriftField
    public void setPropCnt(final int propCnt) { this.propCnt = propCnt; }

    private String guestUid;

    @ThriftField(value=9, name="guestUid")
    public String getGuestUid() { return guestUid; }

    @ThriftField
    public void setGuestUid(final String guestUid) { this.guestUid = guestUid; }

    private long anchorUid;

    @ThriftField(value=10, name="anchorUid")
    public long getAnchorUid() { return anchorUid; }

    @ThriftField
    public void setAnchorUid(final long anchorUid) { this.anchorUid = anchorUid; }

    private double sumAmount;

    @ThriftField(value=11, name="sumAmount")
    public double getSumAmount() { return sumAmount; }

    @ThriftField
    public void setSumAmount(final double sumAmount) { this.sumAmount = sumAmount; }

    private long id;

    @ThriftField(value=12, name="id")
    public long getId() { return id; }

    @ThriftField
    public void setId(final long id) { this.id = id; }

    private TCurrencyType currencyType;

    @ThriftField(value=13, name="currencyType")
    public TCurrencyType getCurrencyType() { return currencyType; }

    @ThriftField
    public void setCurrencyType(final TCurrencyType currencyType) { this.currencyType = currencyType; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("uid", uid)
            .add("propId", propId)
            .add("propName", propName)
            .add("pricingId", pricingId)
            .add("amount", amount)
            .add("usedTime", usedTime)
            .add("sid", sid)
            .add("propCnt", propCnt)
            .add("guestUid", guestUid)
            .add("anchorUid", anchorUid)
            .add("sumAmount", sumAmount)
            .add("id", id)
            .add("currencyType", currencyType)
            .toString();
    }
}
