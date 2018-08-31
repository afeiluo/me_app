package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by Chris on 16/1/21.
 */
public class ReceivedGiftHistory {

    private long bagId; // 礼物记录ID
    private long uid; // 送礼物的用户UID
    private String nick; // 送礼物的用户昵称
    private String headerUrl; // 送礼物的用户头像
    private String medal; // 勋章
    private boolean isFriend; // 是否是好友关系
    private int propId; // 道具ID
    private String propName; // 道具名称
    private String propUrl; // 道具图片
    //    public double amount; // 单价
    private long usedTime; // 收到的时间
    private int propCount; // 道具数量
    //    public long anchorUid; // 主播UID
    private double income; // 获得的米豆
    //    public int currency; // 货币类型

    public long getUid() {
        return uid;
    }

    public long getBagId() {
        return bagId;
    }

    public void setBagId(long bagId) {
        this.bagId = bagId;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getHeaderUrl() {
        return headerUrl;
    }

    public void setHeaderUrl(String headerUrl) {
        this.headerUrl = headerUrl;
    }

    public String getMedal() {
        return medal;
    }

    public void setMedal(String medal) {
        this.medal = medal;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean isFriend) {
        this.isFriend = isFriend;
    }

    public int getPropId() {
        return propId;
    }

    public void setPropId(int propId) {
        this.propId = propId;
    }

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public String getPropUrl() {
        return propUrl;
    }

    public void setPropUrl(String propUrl) {
        this.propUrl = propUrl;
    }

    public long getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(long usedTime) {
        this.usedTime = usedTime;
    }

    public int getPropCount() {
        return propCount;
    }

    public void setPropCount(int propCount) {
        this.propCount = propCount;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
