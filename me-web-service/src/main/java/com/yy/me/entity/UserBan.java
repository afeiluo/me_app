package com.yy.me.entity;

import java.util.Date;

public class UserBan {

    private Long uid;
    
    /**
     * 违规次数
     */
    private Integer violateCount;
    
    /**
     * 用户若被封禁，则为封禁类型（U_BANED_24("u_baned_24"), U_BANED_72("u_baned_72"), U_BANED_FOREVER("u_baned_forever")）
     */
    private String banedType;
    
    /**
     * 用户若被封禁，则记录其解封时间（毫秒）
     */
    private Long banedEndTime;

    private Date createTime;

    private Date updateTime;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getViolateCount() {
        return violateCount;
    }

    public void setViolateCount(Integer violateCount) {
        this.violateCount = violateCount;
    }

    public String getBanedType() {
        return banedType;
    }

    public void setBanedType(String banedType) {
        this.banedType = banedType;
    }

    public Long getBanedEndTime() {
        return banedEndTime;
    }

    public void setBanedEndTime(Long banedEndTime) {
        this.banedEndTime = banedEndTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "UserBan [" + (uid != null ? "uid=" + uid + ", " : "") + (violateCount != null ? "violateCount=" + violateCount + ", " : "")
                + (banedType != null ? "banedType=" + banedType + ", " : "") + (banedEndTime != null ? "banedEndTime=" + banedEndTime + ", " : "")
                + (createTime != null ? "createTime=" + createTime + ", " : "") + (updateTime != null ? "updateTime=" + updateTime : "") + "]";
    }

}