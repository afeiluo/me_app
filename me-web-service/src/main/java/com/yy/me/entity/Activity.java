package com.yy.me.entity;

import java.util.Date;

/**
 * 活动
 */
public class Activity {
    String actId;
    String title;
    String title5s;
    String entryIconUrl; //入口图片地址
    String entryIconUrl5s; //iphone 5s 入口图片地址
    String targetUrl; //跳转地址
    Date startTime; //开始时间
    Date endTime; //结束时间
    Integer status; //状态
    Long anchorUid; //主播uid,不为空表示只针对单个主播设置，为空表示全局配置

    public Long getAnchorUid() {
        return anchorUid;
    }

    public void setAnchorUid(Long anchorUid) {
        this.anchorUid = anchorUid;
    }

    public String getTitle5s() {
        return title5s;
    }

    public void setTitle5s(String title5s) {
        this.title5s = title5s;
    }

    public String getEntryIconUrl5s() {
        return entryIconUrl5s;
    }

    public void setEntryIconUrl5s(String entryIconUrl5s) {
        this.entryIconUrl5s = entryIconUrl5s;
    }

    public String getActId() {
        return actId;
    }

    public void setActId(String actId) {
        this.actId = actId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEntryIconUrl() {
        return entryIconUrl;
    }

    public void setEntryIconUrl(String entryIconUrl) {
        this.entryIconUrl = entryIconUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "actId='" + actId + '\'' +
                ", title='" + title + '\'' +
                ", title5s='" + title5s + '\'' +
                ", entryIconUrl='" + entryIconUrl + '\'' +
                ", entryIconUrl5s='" + entryIconUrl5s + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", anchorUid=" + anchorUid +
                '}';
    }
}

