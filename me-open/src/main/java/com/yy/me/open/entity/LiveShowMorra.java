package com.yy.me.open.entity;

import com.google.common.base.MoreObjects;

import java.util.Date;

/**
 * Created by wangke on 2016/8/16.
 */
public class LiveShowMorra {
    private String gid;

    private String lid;

    private Date createTime;

    private Long anchorUid;

    private Long guestUid;

    private Integer anchorMorra;

    private Integer guestMorra;

    private Date anchorTime;

    private Date guestTime;

    private int morraCount;

    private boolean cancel;

    private Integer result;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getLid() {
        return lid;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getAnchorUid() {
        return anchorUid;
    }

    public void setAnchorUid(Long anchorUid) {
        this.anchorUid = anchorUid;
    }

    public Long getGuestUid() {
        return guestUid;
    }

    public void setGuestUid(Long guestUid) {
        this.guestUid = guestUid;
    }

    public Integer getAnchorMorra() {
        return anchorMorra;
    }

    public void setAnchorMorra(Integer anchorMorra) {
        this.anchorMorra = anchorMorra;
    }

    public Integer getGuestMorra() {
        return guestMorra;
    }

    public void setGuestMorra(Integer guestMorra) {
        this.guestMorra = guestMorra;
    }

    public Date getAnchorTime() {
        return anchorTime;
    }

    public void setAnchorTime(Date anchorTime) {
        this.anchorTime = anchorTime;
    }

    public Date getGuestTime() {
        return guestTime;
    }

    public void setGuestTime(Date guestTime) {
        this.guestTime = guestTime;
    }

    public int getMorraCount() {
        return morraCount;
    }

    public void setMorraCount(int morraCount) {
        this.morraCount = morraCount;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gid", gid)
                .add("lid", lid)
                .add("createTime", createTime)
                .add("anchorUid", anchorUid)
                .add("guestUid", guestUid)
                .add("anchorMorra", anchorMorra)
                .add("guestMorra", guestMorra)
                .add("anchorTime", anchorTime)
                .add("guestTime", guestTime)
                .add("morraCount", morraCount)
                .add("cancel", cancel)
                .add("result", result)
                .toString();
    }
}
