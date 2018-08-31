package com.yy.me.open.entity;

import java.util.List;

/**
 * Created by wangke on 2016/4/21.
 */
public class LiveShowWaterMark {

    private String wmId;

    private String name;

    private String img;

    private Long startTime;

    private Long endTime;

    private Long startDate;

    private Long endDate;

    private List<Long> whiteList;

    private Boolean all;

    public String getWmId() {
        return wmId;
    }

    public void setWmId(String wmId) {
        this.wmId = wmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public List<Long> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<Long> whiteList) {
        this.whiteList = whiteList;
    }

    public Boolean getAll() {
        return all;
    }

    public void setAll(Boolean all) {
        this.all = all;
    }
}
