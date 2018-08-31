package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Gift {
    private String gid;
    private String name;
    private int propId;
    private int price;
    private int intimacy;
    private int income;
    private boolean combo;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPropId() {
        return propId;
    }

    public void setPropId(int propId) {
        this.propId = propId;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getIntimacy() {
        return intimacy;
    }

    public void setIntimacy(int intimacy) {
        this.intimacy = intimacy;
    }

    public int getIncome() {
        return income;
    }

    public void setIncome(int income) {
        this.income = income;
    }

    public boolean isCombo() {
        return combo;
    }

    public void setCombo(boolean combo) {
        this.combo = combo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
