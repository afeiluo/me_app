package com.yy.me.web.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.contact.ContactHessianService;
import com.yy.me.contact.entity.UploadContact;
import com.yy.me.contact.entity.ContactFriend;
import com.yy.me.contact.entity.ContactNotFriend;
import com.yy.me.friend.entity.LikeRelation;
import com.yy.me.json.JsonUtil;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.time.MaskClock;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.user.UserInfo.Fields.*;

/**
 * 通讯录服务
 */
@Service
public class ContactService {
    private final static Logger logger = LoggerFactory.getLogger(ContactService.class);
    @Autowired
    private MetricsClient metricsClient;
    @Autowired
    @Qualifier("contactHessianService")
    private ReferenceFactory<ContactHessianService> contactHessianService;
    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    private final static String service = "contact";

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<com.yy.me.friend.FriendService> friendHessianService;

    @Autowired
    private CntConfService cntConfService;

    public void uploadContact(Long uid, String deviceId, int firstBucket, List<UploadContact> contactList, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            logger.info("uploadContact,uid:{},contactList:{}", uid, StringUtils.join(contactList, ","));
            contactHessianService.getClient().uploadContact(deviceId, uid, firstBucket, contactList);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("uploadContact error,uid:{},deviceId:{},firstBucket:{}", uid, deviceId, firstBucket, e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
            return;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "uploadContact",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void findContactFriend(Long uid, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<ContactFriend> ret = contactHessianService.getClient().findContactFriend(deviceId, uid);
            List<Long> uidList = Lists.transform(ret, new Function<ContactFriend, Long>() {
                public Long apply(@Nullable ContactFriend input) {
                    return input.getUid();
                }
            });
            Map<Long, UserInfo> users = Maps.uniqueIndex(userHessianService.getClient().findUserListByUids(uidList, false), new Function<UserInfo, Long>() {
                @Override
                public Long apply(UserInfo input) {
                    return input.getUid();
                }
            });
            Map<Long, LikeRelation> userRelationMap = friendHessianService.getClient().getUserLikeRelations(uid,uidList);

            List<ObjectNode> retList = new ArrayList<>();
            for (ContactFriend friend : ret) {
                ObjectNode node = JsonUtil.createDefaultMapper().convertValue(friend, ObjectNode.class);
                UserInfo user = users.get(friend.getUid());
                node.put(FIELD_U_NICK, user.getNick());
                node.put(FIELD_U_HEADER_URL, user.getHeaderUrl());
                node.put(FIELD_U_SEX, user.getSex());
                LikeRelation likeRelation = userRelationMap.get(friend.getUid());
                node.put("liking", 0);//1我喜欢你，0我没喜欢你
                node.put("liked", 0);//1被喜欢，0没被喜欢
                if (likeRelation != null) {
                    if (LikeRelation.LIKE.equals(likeRelation)) {
                        node.put("liking", 1);
                    } else if (LikeRelation.BE_LIKED.equals(likeRelation)) {
                        node.put("liked", 1);
                    } else if (LikeRelation.FRIEND.equals(likeRelation)) {
                        node.put("liking", 1);
                        node.put("liked", 1);
                    }
                }
                retList.add(node);
            }
            logger.info("findContactFriend,uid:{},deviceId:{},retList size:{}", uid, deviceId, retList.size());
            sendResponse(request, response, genMsgObj(SUCCESS, null, retList));
        } catch (Exception e) {
            logger.error("findContactFriend error,uid:{},deviceId:{}", uid, deviceId, e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "findContactFriend",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void findContactNotFriend(Long uid, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<ContactNotFriend> ret = contactHessianService.getClient().findContactNotFriend(deviceId, uid);
            sendResponse(request, response, genMsgObj(SUCCESS, null, ret));
        } catch (Exception e) {
            logger.error("findContactNotFriend error,uid:{},deviceId:{}", uid, deviceId, e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "findContactNotFriend",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void hideContact(Long uid, Integer status, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            contactHessianService.getClient().hideContact(uid, status);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("hideContact error,uid:{},status:{}", uid, status, e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "hideContact",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void getHideContact(Long uid,  HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("status",contactHessianService.getClient().getHideContact(uid));
            sendResponse(request, response, genMsgObj(SUCCESS,null,map));
        } catch (Exception e) {
            logger.error("getHideContact error,uid:{}", uid,  e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "getHideContact",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void hasUpload(Long uid, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("status",contactHessianService.getClient().hasUpload(uid,deviceId));
            map.put("forceUploadContact",cntConfService.fetchConfInt(CntConfService.DEFAULT_CONF_ID_FORCE_UPLOAD_CONTACT, "open", 0));
            sendResponse(request, response, genMsgObj(SUCCESS,null,map));
        } catch (Exception e) {
            logger.error("hasUpload error,uid:{}，deviceId:{}", uid,deviceId,  e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "hasUpload",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void getFriendTips(Long uid, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            sendResponse(request, response, genMsgObj(SUCCESS,null,contactHessianService.getClient().getFriendTips(uid,deviceId)));
        } catch (Exception e) {
            logger.error("getFriendTips error,uid:{}，deviceId:{}", uid,deviceId,  e);
            resCode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, service, this.getClass(), "getFriendTips",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }
}
