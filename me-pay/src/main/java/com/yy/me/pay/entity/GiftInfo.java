package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by Chris on 16/1/20.
 */
public class GiftInfo {
    private String bagId; // 唯一ID
    private String seq; // 流水号
    private long uid; // 送礼物的用户uid
    private String nick; // 送礼物的用户昵称
    private String headerUrl; // 送礼物的用户头像
    private long recvUid; // 收礼物的用户uid
    private long usedTime; // 礼物送出时间
    private int propId; // 道具Id
    private int propCount; // 合计后的礼物总数量
    private int income; // 主播获得的收益（米豆）
    private String expand; // 透传的扩展信息

    public String getBagId() {
        return bagId;
    }

    public void setBagId(String bagId) {
        this.bagId = bagId;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public long getUid() {
        return uid;
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

    public long getRecvUid() {
        return recvUid;
    }

    public void setRecvUid(long recvUid) {
        this.recvUid = recvUid;
    }

    public long getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(long usedTime) {
        this.usedTime = usedTime;
    }

    public int getPropId() {
        return propId;
    }

    public void setPropId(int propId) {
        this.propId = propId;
    }

    public int getPropCount() {
        return propCount;
    }

    public void setPropCount(int propCount) {
        this.propCount = propCount;
    }

    public int getIncome() {
        return income;
    }

    public void setIncome(int income) {
        this.income = income;
    }
    
    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
