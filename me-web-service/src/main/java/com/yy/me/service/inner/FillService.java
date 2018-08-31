package com.yy.me.service.inner;

import static com.yy.me.http.BaseServletUtil.getLocalObjMapper;
import static com.yy.me.mongo.MongoUtil.FIELD_CREATE_TIME;
import static com.yy.me.mongo.MongoUtil.FIELD_SERVER;
import static com.yy.me.mongo.MongoUtil.FIELD_UPDATE_TIME;
import static com.yy.me.service.inner.ServiceConst.closeLink;
import static com.yy.me.user.UserInfo.Fields.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.GeneralConfService;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.yycloud.ApTokenUtils;
import com.yy.me.user.OnlineStatus;

/**
 * 补充其他非Mysql数据的Service（补充非严格约束的数据）
 * 
 * 注： 针对pipline的操作结果，一定要用如instanceof
 * Long等操作来判断结果，因为结果很可能是一个Exeception对象而不一定是我们想要的
 * 
 * @author JCY
 * 
 */
@Service
public class FillService {

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    @Qualifier("liveShowThriftService")
    private ReferenceFactory<LiveShowThriftService> liveShowThriftService;

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    @Autowired
    private GeneralConfService generalConfService;

    public ObjectNode fillLiveShow(UserInfo userInfo, LiveShowDto liveShow) throws Exception {
        if (liveShow == null) {
            return null;
        }
        if (userInfo.getUid() == liveShow.getUid()) {// 是否为主播
            appendAnchorToken(userInfo.getUid(), liveShow);
        } else {
            appendGuestToken(userInfo.getUid(), liveShow);
        }

        ObjectNode ret = getLocalObjMapper().convertValue(liveShow, ObjectNode.class);

        fillUserInfo(liveShow.getUid(), ret);// 主播个人信息

        return ret;
    }

    public ObjectNode fillUserInfo(long uid, ObjectNode ret) throws Exception {
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, true);// 主播个人信息
        ret.put(FIELD_U_UID, userInfo.getUid());
        ret.put(FIELD_U_USERNAME, userInfo.getUsername());
        ret.put(FIELD_U_NICK, userInfo.getNick() == null ? "" : userInfo.getNick());
        ret.put(FIELD_U_SIGN, userInfo.getSignature() == null ? "" : userInfo.getSignature());
        ret.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
        ret.put(FIELD_U_VERIFIED, userInfo.getVerified());
        if (userInfo.getVerified()) {
            ret.put(FIELD_U_VERIFIED_REASON, userInfo.getVerifiedReason());
        }
        ret.put(FIELD_U_SEX, userInfo.getSex());
        return ret;
    }

    public List<ObjectNode> fillUserInfo(Long uid, List<Long> uids) throws Exception {
        if (uids == null || uids.isEmpty()) {
            return null;
        }
        uids.add(uid);
        Map<Long, UserInfo> users = Maps.uniqueIndex(userHessianService.getClient().findUserListByUids(uids, false), new Function<UserInfo, Long>() {
            @Override
            public Long apply(UserInfo input) {
                return input.getUid();
            }
        });
        UserInfo selfInfo = users.get(uid);
        List<ObjectNode> ret = Lists.newArrayListWithCapacity(uids.size());
        for (int i = 0; i < uids.size() - 1; i++) {
            UserInfo userInfo = users.get(uids.get(i));
            ObjectNode jo = getLocalObjMapper().createObjectNode();
            if (!jo.has(FIELD_U_UID)) {
                jo.put(FIELD_U_UID, uids.get(i));
            }
            if (userInfo != null) {
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                if(userInfo.getTopMedal()!=null) {
                    jo.put(FIELD_U_TOPMEDAL, userInfo.getTopMedal());
                }
                jo.put(FIELD_U_LASTACTIVETIME, userInfo.getLastActiveTime() == null ? "" : userInfo.getLastActiveTime().getTime() + "");// 上次活跃的时间
                jo.put(FIELD_U_ONLINESTATUS, userInfo.getOnlineStatus() == null ? OnlineStatus.OFFLINE.getStatus() : userInfo.getOnlineStatus());
                if (userInfo.getVerified()) {
                    jo.put(FIELD_U_VERIFIED_REASON, userInfo.getVerifiedReason());
                }
                if (userInfo.getLongitude() != null && userInfo.getLatitude() != null && selfInfo.getLongitude() != null
                        && selfInfo.getLatitude() != null) {
                    Double distance = DistIpUtil.calDistance(userInfo.getLongitude(), userInfo.getLatitude(), selfInfo.getLongitude(),
                            selfInfo.getLatitude());
                    jo.put(FIELD_U_DISTANCE, DistIpUtil.getDisStr(distance));
                }
                jo.put(FIELD_U_SEX, userInfo.getSex());
            }
            ret.add(jo);
        }

        return ret;
    }

    public List<ObjectNode> fillUserInfo(List<Long> uids) throws Exception {
        if (uids == null || uids.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Map<Long, UserInfo> users = Maps.uniqueIndex(userHessianService.getClient().findUserListByUids(uids, false), new Function<UserInfo, Long>() {
            @Override
            public Long apply(UserInfo input) {
                return input.getUid();
            }
        });
        List<ObjectNode> ret = Lists.newArrayListWithCapacity(uids.size());
        for (int i = 0; i < uids.size(); i++) {
            UserInfo userInfo = users.get(uids.get(i));
            ObjectNode jo = getLocalObjMapper().createObjectNode();
            if (!jo.has(FIELD_U_UID)) {
                jo.put(FIELD_U_UID, uids.get(i));
            }
            if (userInfo != null) {
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                if(userInfo.getSignature()!=null) {
                    jo.put(FIELD_U_SIGN, userInfo.getSignature());
                }
                if(userInfo.getTopMedal()!=null) {
                    jo.put(FIELD_U_TOPMEDAL, userInfo.getTopMedal());
                }
                if(userInfo.getVerified()!=null) {
                    jo.put(FIELD_U_VERIFIED, userInfo.getVerified());
                }
                jo.put(FIELD_U_SEX, userInfo.getSex());
            }
            ret.add(jo);
        }

        return ret;
    }

    /**
     * 
     * @param uid 查看者uid
     * @param otherUserInfo 被查看用户的个人信息
     * @return
     * @throws Exception
     */
    public ObjectNode fillUserStat(Long uid, UserInfo otherUserInfo) throws Exception {
        try {
            ObjectNode data = getLocalObjMapper().convertValue(otherUserInfo, ObjectNode.class);
            if (!uid.equals(otherUserInfo.getUid())) {
                // 不暴露用户的信息
                data.remove(FIELD_U_USER_SOURCE);// 只有自己可以看到自己的用户来源信息，如用于 提现中
            }
            // 不暴露用户的信息
            data.remove(FIELD_SERVER);
            data.remove(FIELD_U_MY_LOCALE);
            data.remove(FIELD_U_CLIENT_TYPE);
            data.remove(FIELD_U_CLIENT_VER);
            data.remove(FIELD_U_REG_PUSH_ID);
            data.remove(FIELD_CREATE_TIME);
            data.remove(FIELD_UPDATE_TIME);
            data.remove(FIELD_U_ANCHORTYPE);

            return data;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 
     * @param uid
     * @param tids
     * @param objIds
     * @return
     * @throws Exception
     */
    public List<ObjectNode> fillIdolsInfo(long uid, List<Long> tids, List<String> objIds) throws Exception {
        if (tids == null || tids.isEmpty()) {
            return null;
        }
        List<ObjectNode> ret = fillUserInfo(uid, tids);
        for (int i = 0; i < ret.size(); i++) {
            if (objIds != null) {
                ret.get(i).put("rid", objIds.get(i));// 包括正向和反向的关系表ID
            }
        }
        return ret;
    }

    private LiveShowDto appendAnchorToken(long anchorUid, LiveShowDto liveShow) {
        if (liveShow != null) {
            liveShow.setToken(ApTokenUtils.genSidTokenLocal(anchorUid, "LIVE", "{\"bucket\":\"ourtimes\"}", liveShow.getSid(), 86400, true));
        }
        return liveShow;
    }

    private LiveShowDto appendGuestToken(long guestUid, LiveShowDto liveShow) {
        if (liveShow != null) {
            boolean hasWrite = true;
            if (closeLink) {// 关闭连麦
                hasWrite = false;
            }

            liveShow.setToken(ApTokenUtils.genSidTokenLocal(guestUid, "LIVE", "{\"bucket\":\"ourtimes\"}", liveShow.getSid(), 86400, hasWrite));
        }
        return liveShow;
    }

}
