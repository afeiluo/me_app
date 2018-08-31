package com.yy.me.open.entity;

import com.google.common.base.MoreObjects;

import java.util.Date;

/**
 * Created by wangke on 2016/8/16.
 */
public class LiveShowMorraOuter {
    private String gid;

    private Date createTime;

    private Long anchorUid;

    private Long guestUid;

    private Integer result;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
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
                .add("createTime", createTime)
                .add("anchorUid", anchorUid)
                .add("guestUid", guestUid)
                .add("result", result)
                .toString();
    }
}
