package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TExchangeCurrencyConfig")
public class TExchangeCurrencyConfig
{
    public TExchangeCurrencyConfig() {
    }

    private long id;

    @ThriftField(value=1, name="id")
    public long getId() { return id; }

    @ThriftField
    public void setId(final long id) { this.id = id; }

    private int appid;

    @ThriftField(value=2, name="appid")
    public int getAppid() { return appid; }

    @ThriftField
    public void setAppid(final int appid) { this.appid = appid; }

    private int srcCurrencyType;

    @ThriftField(value=3, name="srcCurrencyType")
    public int getSrcCurrencyType() { return srcCurrencyType; }

    @ThriftField
    public void setSrcCurrencyType(final int srcCurrencyType) { this.srcCurrencyType = srcCurrencyType; }

    private int destCurrencyType;

    @ThriftField(value=4, name="destCurrencyType")
    public int getDestCurrencyType() { return destCurrencyType; }

    @ThriftField
    public void setDestCurrencyType(final int destCurrencyType) { this.destCurrencyType = destCurrencyType; }

    private int srcAmount;

    @ThriftField(value=5, name="srcAmount")
    public int getSrcAmount() { return srcAmount; }

    @ThriftField
    public void setSrcAmount(final int srcAmount) { this.srcAmount = srcAmount; }

    private int destAmount;

    @ThriftField(value=6, name="destAmount")
    public int getDestAmount() { return destAmount; }

    @ThriftField
    public void setDestAmount(final int destAmount) { this.destAmount = destAmount; }

    private int exchangeRate;

    @ThriftField(value=7, name="exchangeRate")
    public int getExchangeRate() { return exchangeRate; }

    @ThriftField
    public void setExchangeRate(final int exchangeRate) { this.exchangeRate = exchangeRate; }

    private int weight;

    @ThriftField(value=8, name="weight")
    public int getWeight() { return weight; }

    @ThriftField
    public void setWeight(final int weight) { this.weight = weight; }

    private long startTime;

    @ThriftField(value=9, name="startTime")
    public long getStartTime() { return startTime; }

    @ThriftField
    public void setStartTime(final long startTime) { this.startTime = startTime; }

    private long endTime;

    @ThriftField(value=10, name="endTime")
    public long getEndTime() { return endTime; }

    @ThriftField
    public void setEndTime(final long endTime) { this.endTime = endTime; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("id", id)
            .add("appid", appid)
            .add("srcCurrencyType", srcCurrencyType)
            .add("destCurrencyType", destCurrencyType)
            .add("srcAmount", srcAmount)
            .add("destAmount", destAmount)
            .add("exchangeRate", exchangeRate)
            .add("weight", weight)
            .add("startTime", startTime)
            .add("endTime", endTime)
            .toString();
    }
}
