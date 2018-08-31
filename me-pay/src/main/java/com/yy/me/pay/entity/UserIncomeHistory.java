package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/4/5.
 */
public class UserIncomeHistory {
    private long recId;
    private double beans;
    private double cash;
    private long recTime;

    public long getRecId() {
        return recId;
    }

    public void setRecId(long recId) {
        this.recId = recId;
    }

    public double getBeans() {
        return beans;
    }

    public void setBeans(double beans) {
        this.beans = beans;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public long getRecTime() {
        return recTime;
    }

    public void setRecTime(long recTime) {
        this.recTime = recTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
