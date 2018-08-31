package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/4/5.
 */
public class ExchangeConfig {
    private long itemId;
    private int mbi;
    private double cash;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public int getMbi() {
        return mbi;
    }

    public void setMbi(int mbi) {
        this.mbi = mbi;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
