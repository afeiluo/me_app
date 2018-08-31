package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 充值活动信息.
 * 
 * @author cuixiang
 * 
 */
public class PaymentActivity {

    private String actId;
    private String name;
    private int position;
    private int propId;
    private String productId; // 苹果专用
    private int buyCountPerUser;
    private long totalCount;
    private long saledCount;
    private long displaySaledCount;
    private Date startTime;
    private Date endTime;

    public String getActId() {
        return actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
    
    public int getPropId() {
        return propId;
    }

    public void setPropId(int propId) {
        this.propId = propId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getBuyCountPerUser() {
        return buyCountPerUser;
    }

    public void setBuyCountPerUser(int buyCountPerUser) {
        this.buyCountPerUser = buyCountPerUser;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getSaledCount() {
        return saledCount;
    }

    public void setSaledCount(long saledCount) {
        this.saledCount = saledCount;
    }

    public long getDisplaySaledCount() {
        return displaySaledCount;
    }

    public void setDisplaySaledCount(long displaySaledCount) {
        this.displaySaledCount = displaySaledCount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
