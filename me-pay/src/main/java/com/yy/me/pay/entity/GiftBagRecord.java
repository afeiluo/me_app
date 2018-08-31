package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * Created by Chris on 16/1/20.
 */
public class GiftBagRecord {

    private String bagId; // 唯一ID
    private String seq; // 流水号
    private long uid; // 送礼物的用户uid
    private long recvUid; // 收礼物的用户uid
    private String lid; // 直播id
    private Date usedTime; // 礼物送出时间
    private int propId; // 道具Id
    private int propCount; // 合计后的礼物总数量
    private int amount; // 合计后的礼物金额,当前为米币
    private int income; // 主播获得的收益（米豆）
    private int intimacy; // 礼物的亲密度值
    private int platform; // 送出礼物的平台
    private String expand; // 透传的扩展信息
    private String comboId; // 礼物连送Id

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

    public long getRecvUid() {
        return recvUid;
    }

    public void setRecvUid(long recvUid) {
        this.recvUid = recvUid;
    }

    public String getLid() {
        return lid;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public Date getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(Date usedTime) {
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

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getIncome() {
        return income;
    }

    public void setIncome(int income) {
        this.income = income;
    }

    public int getIntimacy() {
        return intimacy;
    }

    public void setIntimacy(int intimacy) {
        this.intimacy = intimacy;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
