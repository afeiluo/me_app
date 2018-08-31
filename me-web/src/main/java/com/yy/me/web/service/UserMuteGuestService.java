package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.me.message.BroadcastBuilder;
import com.yy.me.message.LsEventType;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.service.inner.MessageService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.util.GeneralUtil;
import com.yy.me.dao.UserMuteGuestMongoDBMapper;

/**
 * 永久禁言
 * @author JCY
 * 
 */
@Service
public class UserMuteGuestService {

    private static Logger logger = LoggerFactory.getLogger(UserMuteGuestService.class);

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private UserMuteGuestMongoDBMapper userMuteGuestMongoDBMapper;

    @Autowired
    private MessageService messageService;

    public void mute(UserInfo userInfo, long guestUid, String lid, HttpServletRequest request, HttpServletResponse response) {
        UserInfo guestInfo = null;
        try {
            if (userInfo.getUid() == guestUid) {
                sendResponseAuto(request, response, genMsgObj(FAILED, "Not allow mute self!"));
                return;
            }
            guestInfo = userHessianService.getClient().getUserByUid(guestUid,false);
            if (guestInfo == null) {
                sendResponseAuto(request, response, genMsgObj(FAILED, "guestUid Not Exist!"));
                return;
            }
            userMuteGuestMongoDBMapper.mute(userInfo.getUid(), guestUid, lid);
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
            if (StringUtils.isNotBlank(lid)) {
                LiveShowDto ls = GeneralLiveShowClient.getLsByLid(lid);
                if (ls != null && ls.getUid().equals(userInfo.getUid())) {// 只有主播有权力发禁言广播
                    // 发广播
                    UserInfo tmp = new UserInfo();
                    tmp.setUid(guestUid);
                    long order = messageService.genPartialOrder();
                    BroadcastBuilder broadcastBuilder = BroadcastBuilder.start().setLid(lid).setActUserInfo(tmp).setPartialOrder(order)
                            .setLsEventType(LsEventType.MUTE_GUEST).setMuteGuestForever(true);
                    messageService.sendUserLsUpdate(lid, broadcastBuilder);
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() + ", " + GeneralUtil.genMethodParamStr(userInfo.getUid(), guestUid, lid);
            logger.error(msg, e);
            sendResponseAuto(request, response, genMsgObj(FAILED));
        }
    }

    public void cancelMute(UserInfo userInfo, long guestUid, HttpServletRequest request, HttpServletResponse response) {
        UserInfo guestInfo = null;
        try {
            if (userInfo.getUid() == guestUid) {
                sendResponseAuto(request, response, genMsgObj(FAILED, "Not allow cancel mute self!"));
                return;
            }
            guestInfo = userHessianService.getClient().getUserByUid(guestUid,false);
            if (guestInfo == null) {
                sendResponseAuto(request, response, genMsgObj(FAILED, "guestUid Not Exist!"));
                return;
            }
            userMuteGuestMongoDBMapper.cancelMute(userInfo.getUid(), guestUid);
            sendResponseAuto(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            String msg = e.getMessage() + ", " + GeneralUtil.genMethodParamStr(userInfo.getUid(), guestUid);
            logger.error(msg, e);
            sendResponseAuto(request, response, genMsgObj(FAILED, "Cancel mute error."));
        }
    }

}
