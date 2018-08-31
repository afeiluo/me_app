package com.yy.me.web.entity;

import java.util.List;

public class LiveShowH5DurationDto {
    private String currentMonthLabel;
    private List<LiveShowDuration> LiveShowDurationList;

    public String getCurrentMonth() {
        return currentMonthLabel;
    }

    public void setCurrentMonth(String currentMonthLabel) {
        this.currentMonthLabel = currentMonthLabel;
    }

    public List<LiveShowDuration> getLiveShowDurationList() {
        return LiveShowDurationList;
    }

    public void setLiveShowDurationList(List<LiveShowDuration> liveShowDurationList) {
        LiveShowDurationList = liveShowDurationList;
    }

}
