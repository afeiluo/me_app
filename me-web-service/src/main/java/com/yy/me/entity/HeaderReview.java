package com.yy.me.entity;

import com.google.common.base.MoreObjects;

import java.util.Date;

/**
 * Created by wangke on 2016/8/19.
 */
public class HeaderReview {
    private long uid;

    private int status;

    private String headerUrl;

    private Date createTime;

    private Date reviewTime;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getHeaderUrl() {
        return headerUrl;
    }

    public void setHeaderUrl(String headerUrl) {
        this.headerUrl = headerUrl;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime) {
        this.reviewTime = reviewTime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uid", uid)
                .add("status", status)
                .add("headerUrl", headerUrl)
                .add("createTime", createTime)
                .add("reviewTime", reviewTime)
                .toString();
    }
}
