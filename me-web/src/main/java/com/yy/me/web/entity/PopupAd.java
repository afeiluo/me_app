package com.yy.me.web.entity;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PopupAd {
    private String aid;
    private String title;
    private String imgUrl;
    private String jumpUrl;
    private String jumpLid;
    private Long jumpUid;
    private String jumpTopic;
    private int jumpType;
    private ObjectNode liveShow;// 如果跳转类型是直播的话,返回客户端里面包含一个直播的信息
    private Date startTime;
    private Date endTime;

    public enum PopupAdJumpType {
        H5PAGE(1), // H5页面
        LIVEROOM(2), // 直播间
        NULL(3), // 无
        LIVEUIDROOM(4),
        TOPICLIST(5)
        ;
        private int value;

        private PopupAdJumpType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static PopupAdJumpType valueOf(int value) {
            for (PopupAdJumpType target : PopupAdJumpType.values()) {
                if (target.getValue() == value) {
                    return target;
                }
            }
            return null;
        }
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
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
        return ToStringBuilder.reflectionToString(this);
    }

    public int getJumpType() {
        return jumpType;
    }

    public void setJumpType(int jumpType) {
        this.jumpType = jumpType;
    }

    public ObjectNode getLiveShow() {
        return liveShow;
    }

    public void setLiveShow(ObjectNode liveShow) {
        this.liveShow = liveShow;
    }

    public String getJumpLid() {
        return jumpLid;
    }

    public void setJumpLid(String jumpLid) {
        this.jumpLid = jumpLid;
    }

    public Long getJumpUid() {
        return jumpUid;
    }

    public void setJumpUid(Long jumpUid) {
        this.jumpUid = jumpUid;
    }

    public String getJumpTopic() {
        return jumpTopic;
    }

    public void setJumpTopic(String jumpTopic) {
        this.jumpTopic = jumpTopic;
    }
}
