package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/4/5.
 */
public class UserWithdrawHistory {
    private long recId;
    private double amount;
    private int status; // 1 - 提现中， 2 - 已到账， 3 - 提现失败
    private long recvTime;

    public long getRecId() {
        return recId;
    }

    public void setRecId(long recId) {
        this.recId = recId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getRecvTime() {
        return recvTime;
    }

    public void setRecvTime(long recvTime) {
        this.recvTime = recvTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
