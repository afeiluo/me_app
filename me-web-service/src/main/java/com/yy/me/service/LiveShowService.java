package com.yy.me.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.enums.LsStopReason;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.client.util.LiveShowConverter;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.liveshow.thrift.LsRequestHeader;
import com.yy.me.liveshow.thrift.LsResponse;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.metrics.MetricsClient.ProtocolType;
import com.yy.me.redis.RedisUtil;
import com.yy.me.time.MaskClock;

@Service
public class LiveShowService {
    private static Logger logger = LoggerFactory.getLogger(LiveShowService.class);

    @Autowired
    @Qualifier("liveShowThriftService")
    private ReferenceFactory<LiveShowThriftService> liveShowThriftService;
    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private CntConfService cntConfService;

    public void refreshAllLsStatus() throws Exception {
        List<LiveShowDto> all = GeneralLiveShowClient.findAllLs();
        if (all == null) {
            return;
        }
        for (LiveShowDto liveShow : all) {
            liveShowThriftService.getClient().syncLsStatus2All(null, liveShow.getLid());
        }
    }

    public void cancelLinkThrift(long guestUid) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            LiveShowDto ls = GeneralLiveShowClient.getLsByLinkUid(guestUid);
            if (ls != null) {
                liveShowThriftService.getClient().cancelLink(null, guestUid, ls.getLid(), guestUid);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(ProtocolType.INNER, "innerServer", this.getClass(), "cancelLinkThrift", MaskClock.getCurtime() - start, rescode);
        }
    }

    public void guestLeaveAllLsThrift(long guestUid, String pushId) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            liveShowThriftService.getClient().guestLeaveAllLs(null, guestUid, pushId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient
                    .report(ProtocolType.INNER, "innerServer", this.getClass(), "guestLeaveAllLsThrift", MaskClock.getCurtime() - start, rescode);
        }
    }

    /**
     * 部分直播的Redis数据并入MongoDB（新增粉丝数、点赞总数、观看总数），去掉stopped字段————但像“当前观众数量、最终观众总数”等值都是需要uid去重的，因而还是放入redis，
     * 在最终stop的时候统计完并入MongoDB后再删除Redis中的数据
     * 
     * @return
     * @throws Exception
     */
    public LiveShowDto stop(final LiveShowDto liveShow, final boolean violate, final int lsStopReason) throws Exception {
        LsResponse lsResponse = liveShowThriftService.getClient().stop(null, liveShow.getLid(), violate, lsStopReason);
        LiveShowDto ret = LiveShowConverter.parseLsResponse2Dto(lsResponse);
        if (ret == null) {
            logger.info("******LiveShow is alread stop by other Server! {}", liveShow);
            return liveShow;
        }
        liveShow.setGuestTotalWatchTimeInSec(ret.getGuestTotalWatchTimeInSec());
        liveShow.setSnapshotUrl(null);
        liveShow.setStartingNow(false);
        liveShow.setLikePersonCount(ret.getLikePersonCount());
        liveShow.setStopTime(ret.getStopTime());
        liveShow.setDurationInMills(ret.getDurationInMills());
        liveShow.setGuestTotalWatchTimeInSec(ret.getGuestTotalWatchTimeInSec());
        return liveShow;
    }

    public void closeLiveShowByOwnerAndPushId(long uid, String pushId, RedisUtil recordMasterRedisUtil) throws Exception {
        if (StringUtils.isBlank(pushId)) {
            return;
        }
        LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
        if (liveShow == null) {
            return;
        }
        logger.error("Misaka stop ls:{}", liveShow);
        stop(liveShow, false, LsStopReason.CHANG_LIAN_JIE.getValue());
    }

    public void closeViolatedLiveShowByOwner(long uid) throws Exception {
        LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
        if (liveShow == null) {
            return;
        }
        stop(liveShow, true, LsStopReason.VIOLATE.getValue());
    }

    public void sendLiveShowStatus(String lid, long uid, String pushId) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            if (uid == 0 || StringUtils.isBlank(lid)) {
                return;
            }
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveShow != null && uid != liveShow.getUid()) {// 自己千万不能发给自己！因为会有数据库同步延时！导致获取的startingNow为false
                liveShowThriftService.getClient().syncLsStatus(null, lid, uid, pushId);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(ProtocolType.INNER, "innerServer", this.getClass(), "sendLiveShowStatus", MaskClock.getCurtime() - start, rescode);
        }
    }

    public void updateLocationToLs(LsRequestHeader header, long uid, String lid, double longitude, double latitude, String city, String ip) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            LiveShowDto dto = new LiveShowDto();
            dto.setLocationX(latitude);
            dto.setLocationY(longitude);
            dto.setIp(ip);
            dto.setLocationCityName(city);
            liveShowThriftService.getClient().updateLsLocation(header, uid, lid, LiveShowConverter.liveShowDto2Pb(dto, null).build().toByteArray());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(ProtocolType.INNER, "innerServer", this.getClass(), "updateLocationToLs", MaskClock.getCurtime() - start, rescode);
        }
    }
}
