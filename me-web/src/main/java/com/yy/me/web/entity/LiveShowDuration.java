package com.yy.me.web.entity;

import java.util.Date;
import java.util.List;

public class LiveShowDuration {

    private Long uid;
    private Date liveShowDate;
    private String liveShowDateStr;
    private Long duration;
    private Integer isLive;
    private Integer isValidLive;
    private List<LiveTime> liveTimeList;
    private String minuteDuration;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Date getLiveShowDate() {
        return liveShowDate;
    }

    public void setLiveShowDate(Date liveShowDate) {
        this.liveShowDate = liveShowDate;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getIsLive() {
        return isLive;
    }

    public void setIsLive(Integer isLive) {
        this.isLive = isLive;
    }

    public Integer getIsValidLive() {
        return isValidLive;
    }

    public void setIsValidLive(Integer isValidLive) {
        this.isValidLive = isValidLive;
    }

    public List<LiveTime> getLiveTimeList() {
        return liveTimeList;
    }

    public void setLiveTimeList(List<LiveTime> liveTimeList) {
        this.liveTimeList = liveTimeList;
    }
    
    public String getMinuteDuration() {
        minuteDuration = "";
        Double durationDouble = (double)duration;
        Double minuteDouble = durationDouble / (1000 * 60);
        minuteDuration = String.format("%.2f", minuteDouble);
        return minuteDuration;
    }
    
    public void setMinuteDuration(String minuteDuration) {
        this.minuteDuration = minuteDuration;
    }

    public static void main(String[] args) {
        Long duration = 1806000l; 
        Double durationDouble = (double)duration;
        System.out.println(durationDouble);
        Double minuteDouble = durationDouble / (1000 * 60);
        System.out.println(String.format("%.2f", minuteDouble));
    }
    
    public String getLiveShowDateStr() {
        return liveShowDateStr;
    }

    public void setLiveShowDateStr(String liveShowDateStr) {
        this.liveShowDateStr = liveShowDateStr;
    }

    public class LiveTime {
        private Date startTime;
        private Date stopTime;
        
        public Date getStartTime() {
            return startTime;
        }

        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }

        public Date getStopTime() {
            return stopTime;
        }

        public void setStopTime(Date stopTime) {
            this.stopTime = stopTime;
        }
    }
}
