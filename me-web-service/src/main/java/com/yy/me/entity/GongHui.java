package com.yy.me.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/5/7.
 */
public class GongHui {
    private long ghUid;
    private String name;
    private long mibiBalance;
    private double cashBalance;

    public long getGhUid() {
        return ghUid;
    }

    public void setGhUid(long ghUid) {
        this.ghUid = ghUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMibiBalance() {
        return mibiBalance;
    }

    public void setMibiBalance(long mibiBalance) {
        this.mibiBalance = mibiBalance;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
