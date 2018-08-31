package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TRevenueRecord")
public class TRevenueRecord
{
    public TRevenueRecord() {
    }

    private long id;

    @ThriftField(value=1, name="id")
    public long getId() { return id; }

    @ThriftField
    public void setId(final long id) { this.id = id; }

    private long uid;

    @ThriftField(value=2, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private long contributeUid;

    @ThriftField(value=3, name="contributeUid")
    public long getContributeUid() { return contributeUid; }

    @ThriftField
    public void setContributeUid(final long contributeUid) { this.contributeUid = contributeUid; }

    private long sid;

    @ThriftField(value=4, name="sid")
    public long getSid() { return sid; }

    @ThriftField
    public void setSid(final long sid) { this.sid = sid; }

    private double income;

    @ThriftField(value=5, name="income")
    public double getIncome() { return income; }

    @ThriftField
    public void setIncome(final double income) { this.income = income; }

    private double incomeRate;

    @ThriftField(value=6, name="incomeRate")
    public double getIncomeRate() { return incomeRate; }

    @ThriftField
    public void setIncomeRate(final double incomeRate) { this.incomeRate = incomeRate; }

    private double realIncome;

    @ThriftField(value=7, name="realIncome")
    public double getRealIncome() { return realIncome; }

    @ThriftField
    public void setRealIncome(final double realIncome) { this.realIncome = realIncome; }

    private long optTime;

    @ThriftField(value=8, name="optTime")
    public long getOptTime() { return optTime; }

    @ThriftField
    public void setOptTime(final long optTime) { this.optTime = optTime; }

    private long revenueDate;

    @ThriftField(value=9, name="revenueDate")
    public long getRevenueDate() { return revenueDate; }

    @ThriftField
    public void setRevenueDate(final long revenueDate) { this.revenueDate = revenueDate; }

    private int revenueType;

    @ThriftField(value=10, name="revenueType")
    public int getRevenueType() { return revenueType; }

    @ThriftField
    public void setRevenueType(final int revenueType) { this.revenueType = revenueType; }

    private int exchageLevel;

    @ThriftField(value=11, name="exchageLevel")
    public int getExchageLevel() { return exchageLevel; }

    @ThriftField
    public void setExchageLevel(final int exchageLevel) { this.exchageLevel = exchageLevel; }

    private int appid;

    @ThriftField(value=12, name="appid")
    public int getAppid() { return appid; }

    @ThriftField
    public void setAppid(final int appid) { this.appid = appid; }

    private int srcType;

    @ThriftField(value=13, name="srcType")
    public int getSrcType() { return srcType; }

    @ThriftField
    public void setSrcType(final int srcType) { this.srcType = srcType; }

    private int additionRate;

    @ThriftField(value=14, name="additionRate")
    public int getAdditionRate() { return additionRate; }

    @ThriftField
    public void setAdditionRate(final int additionRate) { this.additionRate = additionRate; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("id", id)
            .add("uid", uid)
            .add("contributeUid", contributeUid)
            .add("sid", sid)
            .add("income", income)
            .add("incomeRate", incomeRate)
            .add("realIncome", realIncome)
            .add("optTime", optTime)
            .add("revenueDate", revenueDate)
            .add("revenueType", revenueType)
            .add("exchageLevel", exchageLevel)
            .add("appid", appid)
            .add("srcType", srcType)
            .add("additionRate", additionRate)
            .toString();
    }
}
