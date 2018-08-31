package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/4/5.
 */
public class UserExchangeHistory {
    private long recId;
    private long amount;
    private int status = 1; // 1 -已到账， 2 - 购买失败
    private long buyTime;

    public long getRecId() {
        return recId;
    }

    public void setRecId(long recId) {
        this.recId = recId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(long buyTime) {
        this.buyTime = buyTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
