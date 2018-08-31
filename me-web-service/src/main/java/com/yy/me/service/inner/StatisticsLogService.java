package com.yy.me.service.inner;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.yy.me.enums.StatisticsType;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.user.UserInfo;

@Service
public class StatisticsLogService {

    private static final Logger log = LoggerFactory.getLogger("stat.report.log");

    private static final String PATTERN = "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}";// 13个，type, uid, lid, ...

    private static final String NOT_EXIST = "-1";

    private static FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMdd");
    
    @Scheduled(cron = "1 0 0 * * *")
    public void truncateLog() {
        log.info(PATTERN, StatisticsType.TRUNCATE.getValue());
    }

    private static String filterChar(String str) {
        if (StringUtils.isBlank(str)) {
            str = NOT_EXIST;
        } else {
            str = str.replaceAll("\\t", " ");
            if (StringUtils.isBlank(str)) {
                str = NOT_EXIST;
            }
        }
        return str;
    }

    private static String ts(String str) {
        return filterChar(str);
    }

    private static String tn(Number str) {
        return str == null ? NOT_EXIST : str.toString();
    }

    private static String td(Date str) {
        return str == null ? NOT_EXIST : str.getTime() + "";
    }

    private static String tds(Date d) {
        return d == null ? NOT_EXIST : formatter.format(d.getTime());
    }
    
    public void logMachineLike(long uid, LiveShowDto liveShow) {
        log.info(PATTERN, StatisticsType.MACHINE_LS_LIKE.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()));
    }
    
    public void logLsLike(long uid, LiveShowDto liveShow) {
        log.info(PATTERN, StatisticsType.LS_LIKE.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()));
    }
    
    public void logMachineChat(long uid, LiveShowDto liveShow, String msg) {
        log.info(PATTERN, StatisticsType.MACHINE_LS_CHAT.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()), ts(msg));
    }
    
    public void logLsChat(long uid, LiveShowDto liveShow, String msg) {
        log.info(PATTERN, StatisticsType.LS_CHAT.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()), ts(msg));
    }
    
    public void logMachineShare3p(long uid, LiveShowDto liveShow, int sharePlfType) {
        log.info(PATTERN, StatisticsType.MACHINE_SHARE3P.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()), sharePlfType);
    }
    
    public void logShare3p(long uid, LiveShowDto liveShow, int sharePlfType) {
        log.info(PATTERN, StatisticsType.SHARE3P.getValue(), uid, ts(liveShow.getLid()), liveShow.getUid(), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()), td(liveShow.getStartTime()), sharePlfType);
    }
    
    public void logLsGuestEnter(LiveShowDto liveShow, long guestUid, Long guestRate) {
        log.info(PATTERN, StatisticsType.LS_GUEST_ENTER.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), guestUid, td(new Date()), guestRate);
    }

    public void logLsGuestLeave(LiveShowDto liveShow, long guestUid, long guestEnterTime) {
        log.info(PATTERN, StatisticsType.LS_GUEST_LEAVE.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), guestUid, td(new Date()), tn(guestEnterTime), tn(Math.abs(System.currentTimeMillis() - guestEnterTime)));
    }

    public void logLsHandsUp(LiveShowDto liveShow, long guestUid, long score) {
        log.info(PATTERN, StatisticsType.LS_HANDS_UP.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), tn(liveShow.getHandsUpTotal()), guestUid, td(new Date()), score);
    }

    public void logLsLink(LiveShowDto liveShow, long handsUpUid) {
        log.info(PATTERN, StatisticsType.LS_LINK.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), tn(liveShow.getHandsUpTotal()), handsUpUid, td(new Date()));
    }

    public void logLsLinkCancel(LiveShowDto liveShow, long handsUpUid, String reason) {
        log.info(PATTERN, StatisticsType.LS_LINK_CANCEL.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), tn(liveShow.getHandsUpTotal()), handsUpUid, td(new Date()), ts(reason));
    }

    public void logLsHandsDown(LiveShowDto liveShow, long guestUid) {
        log.info(PATTERN, StatisticsType.LS_HANDS_DOWN.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(liveShow.getGuestCount()), tn(liveShow.getTotalGuestCount()),
                tn(liveShow.getPeakGuestCount()), td(liveShow.getStartTime()), tn(liveShow.getHandsUpTotal()), guestUid, td(new Date()));
    }

    public void logLsStart(LiveShowDto liveShow) {
        log.info(PATTERN, StatisticsType.LS_START.getValue(), liveShow.getUid(), ts(liveShow.getLid()), td(liveShow.getCreateTime()),
                td(liveShow.getStartTime()), ts(liveShow.getLocationCityName()));
    }

    public void logLsStop(LiveShowDto liveShow, int lsStopReason, long peakGuestCountReal) {
        log.info(PATTERN, StatisticsType.LS_STOP.getValue(), liveShow.getUid(), ts(liveShow.getLid()), tn(null),
                tn(null), tn(liveShow.getNewIncome()), tn(liveShow.getDurationInMills()), td(liveShow.getStartTime()),
                td(liveShow.getStopTime()), lsStopReason, tn(0L), tn(peakGuestCountReal), tn(liveShow.getTotalGuestCount()));
    }

    public void logUserRegist(UserInfo userInfo, String clientType, String clientVer, String pushId, String channel) {
        log.info(PATTERN, StatisticsType.USER_REGIST.getValue(), userInfo.getUid(), userInfo.getUsername(), ts(userInfo.getNick()), userInfo.getSex(),
                ts(userInfo.getThirdPartyId()), td(userInfo.getCreateTime()), tds(userInfo.getCreateTime()), tn(userInfo.getUserSource()), ts(clientType), ts(clientVer), ts(channel), ts(pushId));
    }

    public void logUserChangeHeader(UserInfo userInfo) {
        log.info(PATTERN, StatisticsType.USER_CHANGE_HEADER.getValue(), userInfo.getUid(), userInfo.getUsername(), ts(userInfo.getNick()), userInfo.getSex(),
                ts(userInfo.getThirdPartyId()), td(userInfo.getCreateTime()), tds(userInfo.getCreateTime()), tn(userInfo.getUserSource()), tn(null), ts(userInfo.getHeaderUrl()));
    }

}
