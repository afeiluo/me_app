package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("TMonthSettleApply")
public class TMonthSettleApply
{
    public TMonthSettleApply() {
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

    private long settleDate;

    @ThriftField(value=3, name="settleDate")
    public long getSettleDate() { return settleDate; }

    @ThriftField
    public void setSettleDate(final long settleDate) { this.settleDate = settleDate; }

    private long applyTime;

    @ThriftField(value=4, name="applyTime")
    public long getApplyTime() { return applyTime; }

    @ThriftField
    public void setApplyTime(final long applyTime) { this.applyTime = applyTime; }

    private double applyRealAmount;

    @ThriftField(value=5, name="applyRealAmount")
    public double getApplyRealAmount() { return applyRealAmount; }

    @ThriftField
    public void setApplyRealAmount(final double applyRealAmount) { this.applyRealAmount = applyRealAmount; }

    private double exchangeSalaryAmount;

    @ThriftField(value=6, name="exchangeSalaryAmount")
    public double getExchangeSalaryAmount() { return exchangeSalaryAmount; }

    @ThriftField
    public void setExchangeSalaryAmount(final double exchangeSalaryAmount) { this.exchangeSalaryAmount = exchangeSalaryAmount; }

    private int result;

    @ThriftField(value=7, name="result")
    public int getResult() { return result; }

    @ThriftField
    public void setResult(final int result) { this.result = result; }

    private int appid;

    @ThriftField(value=8, name="appid")
    public int getAppid() { return appid; }

    @ThriftField
    public void setAppid(final int appid) { this.appid = appid; }

    private int destCurrencyType;

    @ThriftField(value=9, name="destCurrencyType")
    public int getDestCurrencyType() { return destCurrencyType; }

    @ThriftField
    public void setDestCurrencyType(final int destCurrencyType) { this.destCurrencyType = destCurrencyType; }

    private TUserType userType;

    @ThriftField(value=10, name="userType")
    public TUserType getUserType() { return userType; }

    @ThriftField
    public void setUserType(final TUserType userType) { this.userType = userType; }

    private double compensationAmount;

    @ThriftField(value=11, name="compensationAmount")
    public double getCompensationAmount() { return compensationAmount; }

    @ThriftField
    public void setCompensationAmount(final double compensationAmount) { this.compensationAmount = compensationAmount; }

    private int reOrderFlag;

    @ThriftField(value=12, name="reOrderFlag")
    public int getReOrderFlag() { return reOrderFlag; }

    @ThriftField
    public void setReOrderFlag(final int reOrderFlag) { this.reOrderFlag = reOrderFlag; }

    private int settleType;

    @ThriftField(value=13, name="settleType")
    public int getSettleType() { return settleType; }

    @ThriftField
    public void setSettleType(final int settleType) { this.settleType = settleType; }

    private int withdrawAccountType;

    @ThriftField(value=14, name="withdrawAccountType")
    public int getWithdrawAccountType() { return withdrawAccountType; }

    @ThriftField
    public void setWithdrawAccountType(final int withdrawAccountType) { this.withdrawAccountType = withdrawAccountType; }

    private String withdrawAccount;

    @ThriftField(value=15, name="withdrawAccount")
    public String getWithdrawAccount() { return withdrawAccount; }

    @ThriftField
    public void setWithdrawAccount(final String withdrawAccount) { this.withdrawAccount = withdrawAccount; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("id", id)
            .add("uid", uid)
            .add("settleDate", settleDate)
            .add("applyTime", applyTime)
            .add("applyRealAmount", applyRealAmount)
            .add("exchangeSalaryAmount", exchangeSalaryAmount)
            .add("result", result)
            .add("appid", appid)
            .add("destCurrencyType", destCurrencyType)
            .add("userType", userType)
            .add("compensationAmount", compensationAmount)
            .add("reOrderFlag", reOrderFlag)
            .add("settleType", settleType)
            .add("withdrawAccountType", withdrawAccountType)
            .add("withdrawAccount", withdrawAccount)
            .toString();
    }
}
