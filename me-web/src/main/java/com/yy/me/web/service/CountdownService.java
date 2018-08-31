package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.DBObject;
import com.yy.me.enums.CountDownOperType;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.service.inner.MessageService;
import com.yy.me.web.dao.CountdownMongoDBMapper;

/**
 * Created by ben on 16/7/19.
 */
@Service
public class CountdownService {
    private static Logger logger = LoggerFactory.getLogger(CountdownService.class);

    @Autowired
    private MessageService messageService;
    @Autowired
    private CountdownMongoDBMapper countdownMongoDBMapper;

    public void setCountdown(Long uid, String lid, Long duration, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveshow != null) {
                messageService.pushCountDownMsg(lid, duration, CountDownOperType.SET.getValue());
                countdownMongoDBMapper.storeCountdownInfo(uid, lid, duration, CountDownOperType.SET.getValue());
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Set count down error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveshow != null) {
                DBObject obj = countdownMongoDBMapper.getDBRecord(uid, lid);
                if (obj != null) {
                    obj.removeField("_id");
                    long duration = (Long) obj.get(CountdownMongoDBMapper.FIELD_DURATION);
                    long updateTime = (Long) obj.get(CountdownMongoDBMapper.FIELD_UPDATETIME);
                    Object leftTimeObj = obj.get(CountdownMongoDBMapper.FIELD_LEFTTIME);
                    int type = (Integer) obj.get(CountdownMongoDBMapper.FIELD_UPDATETIME);
                    if (type != CountDownOperType.STOP.getValue()) {// 没有结束
                        long validTime = duration;
                        if (type == CountDownOperType.START.getValue()) {// 已经开始
                            validTime = leftTimeObj == null ? duration - (System.currentTimeMillis() - updateTime) : (Long) leftTimeObj
                                    - (System.currentTimeMillis() - updateTime);
                        }
                        if (type == CountDownOperType.PAUSE.getValue()) {// 或者暂停
                            validTime = (Long) leftTimeObj;
                        }

                        if (validTime > 0) {
                            obj.put("duration", validTime);
                            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, obj));
                            return;
                        }
                    }

                }
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Get count down error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void startCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveshow != null) {
                DBObject obj = countdownMongoDBMapper.getDBRecord(uid, lid);
                if (obj != null) {
                    if ((int) obj.get(CountdownMongoDBMapper.FIELD_TYPE) == CountDownOperType.SET.getValue()) {// 设置倒计时后开始
                        Long duration = (Long) obj.get(CountdownMongoDBMapper.FIELD_DURATION);
                        countdownMongoDBMapper.updateCountdownInfo(uid, lid, CountDownOperType.START.getValue(), null);
                        messageService.pushCountDownMsg(lid, duration, CountDownOperType.START.getValue());
                    } else if ((int) obj.get(CountdownMongoDBMapper.FIELD_TYPE) == CountDownOperType.PAUSE.getValue()) {// 暂停过后重新开始
                        Long duration = (Long) obj.get(CountdownMongoDBMapper.FIELD_DURATION);
                        Long leftTime = obj.get(CountdownMongoDBMapper.FIELD_LEFTTIME) == null ? duration : (Long) obj
                                .get(CountdownMongoDBMapper.FIELD_LEFTTIME);// 还剩下的时间
                        countdownMongoDBMapper.updateCountdownInfo(uid, lid, CountDownOperType.START.getValue(), null);
                        messageService.pushCountDownMsg(lid, leftTime, CountDownOperType.START.getValue());
                    }
                }
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Start count down error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void stopCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveshow != null) {
                DBObject obj = countdownMongoDBMapper.getDBRecord(uid, lid);
                if (obj != null) {
                    countdownMongoDBMapper.updateCountdownInfo(uid, lid, CountDownOperType.STOP.getValue(), null);
                    messageService.pushCountDownMsg(lid, (Long) obj.get(CountdownMongoDBMapper.FIELD_DURATION), CountDownOperType.STOP.getValue());
                }
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Stop count down error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }

    }

    public void pauseCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveshow != null) {
                DBObject obj = countdownMongoDBMapper.getDBRecord(uid, lid);
                if (obj != null) {
                    if ((int) obj.get(CountdownMongoDBMapper.FIELD_TYPE) == CountDownOperType.START.getValue()) {// 是已经开始的状态
                        long updateTime = (Long) obj.get(CountdownMongoDBMapper.FIELD_UPDATETIME);// 上次更新的时间
                        long duration = (Long) obj.get(CountdownMongoDBMapper.FIELD_DURATION);
                        Long leftTime = obj.get(CountdownMongoDBMapper.FIELD_LEFTTIME) == null ? duration - (System.currentTimeMillis() - updateTime)
                                : (Long) obj.get(CountdownMongoDBMapper.FIELD_LEFTTIME) - (System.currentTimeMillis() - updateTime);// 还剩下的时间
                        if (leftTime < 0) {
                            logger.error("countdown lefttime {} is not correct uid:{} lid:{} leftTimeInDB:{} updateTime:{} duration:{} curTime:{}",
                                    leftTime, uid, lid, obj.get(CountdownMongoDBMapper.FIELD_LEFTTIME), updateTime, duration,
                                    System.currentTimeMillis());
                        } else {
                            countdownMongoDBMapper.updateCountdownInfo(uid, lid, CountDownOperType.PAUSE.getValue(), leftTime);
                            messageService.pushCountDownMsg(lid, leftTime, CountDownOperType.PAUSE.getValue());
                        }
                    }

                }
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Pause count down error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }
}
