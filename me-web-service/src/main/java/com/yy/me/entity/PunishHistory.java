package com.yy.me.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * Created by Chris on 16/6/15.
 */
public class PunishHistory {
    private String punishId;
    private int source;
    private long uid;
    private String lid;
    private int type;
    private String reason;
    private String message;
    private Date punishDate;

    public String getPunishId() {
        return punishId;
    }

    public void setPunishId(String punishId) {
        this.punishId = punishId;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getLid() {
        return lid;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getPunishDate() {
        return punishDate;
    }

    public void setPunishDate(Date punishDate) {
        this.punishDate = punishDate;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
