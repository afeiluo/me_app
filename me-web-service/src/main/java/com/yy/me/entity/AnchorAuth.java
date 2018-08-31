package com.yy.me.entity;

import com.google.common.base.MoreObjects;

/**
 * Created by wangke on 2016/8/19.
 */
public class AnchorAuth {
    private long uid;

    private int status;

    private String reason;

    private int result1;

    private int result2;

    private String phone;

    private String idCard;

    private String name;

    private String image1;

    private String image2;

    private int authSend;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getResult1() {
        return result1;
    }

    public void setResult1(int result1) {
        this.result1 = result1;
    }

    public int getResult2() {
        return result2;
    }

    public void setResult2(int result2) {
        this.result2 = result2;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage1() {
        return image1;
    }

    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public String getImage2() {
        return image2;
    }

    public void setImage2(String image2) {
        this.image2 = image2;
    }

    public int getAuthSend() {
        return authSend;
    }

    public void setAuthSend(int authSend) {
        this.authSend = authSend;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uid", uid)
                .add("status", status)
                .add("reason", reason)
                .add("result1", result1)
                .add("result2", result2)
                .add("phone", phone)
                .add("idCard", idCard)
                .add("name", name)
                .add("image1", image1)
                .add("image2", image2)
                .add("authSend", authSend)
                .toString();
    }
}
