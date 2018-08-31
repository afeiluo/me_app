package com.yy.me.entity;

import com.google.common.base.MoreObjects;

import java.util.Date;
import java.util.List;

/**
 * 冲榜活动
 */
public class RankActivity extends Activity{
    private String waterMarkUrl;//水印url
    private String anchorRanklistMedalUrl; //主播榜徽章URL
    private String userRanklistMedalUrl; //用户榜徽章url
    private Date medalShowEndTime; //勋章显示结束时间
    private List<String> medalUrls;//勋章地址列表
    private Integer size;//得奖排行榜数量

    private Integer anchorRanklistSize;//主播榜徽章数量
    private Integer userRanklistSize;//用户榜徽章数量

    public Integer getAnchorRanklistSize() {
        return anchorRanklistSize;
    }

    public void setAnchorRanklistSize(Integer anchorRanklistSize) {
        this.anchorRanklistSize = anchorRanklistSize;
    }

    public Integer getUserRanklistSize() {
        return userRanklistSize;
    }

    public void setUserRanklistSize(Integer userRanklistSize) {
        this.userRanklistSize = userRanklistSize;
    }

    public String getAnchorRanklistMedalUrl() {
        return anchorRanklistMedalUrl;
    }

    public void setAnchorRanklistMedalUrl(String anchorRanklistMedalUrl) {
        this.anchorRanklistMedalUrl = anchorRanklistMedalUrl;
    }

    public String getUserRanklistMedalUrl() {
        return userRanklistMedalUrl;
    }

    public void setUserRanklistMedalUrl(String userRanklistMedalUrl) {
        this.userRanklistMedalUrl = userRanklistMedalUrl;
    }

    public Date getMedalShowEndTime() {
        return medalShowEndTime;
    }

    public void setMedalShowEndTime(Date medalShowEndTime) {
        this.medalShowEndTime = medalShowEndTime;
    }

    public String getWaterMarkUrl() {
        return waterMarkUrl;
    }

    public void setWaterMarkUrl(String waterMarkUrl) {
        this.waterMarkUrl = waterMarkUrl;
    }

    public List<String> getMedalUrls() {
        return medalUrls;
    }

    public void setMedalUrls(List<String> medalUrls) {
        this.medalUrls = medalUrls;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("waterMarkUrl", waterMarkUrl)
                .add("anchorRanklistMedalUrl", anchorRanklistMedalUrl)
                .add("userRanklistMedalUrl", userRanklistMedalUrl)
                .add("medalShowEndTime", medalShowEndTime)
                .add("medalUrls", medalUrls)
                .add("size", size)
                .toString();
    }
}