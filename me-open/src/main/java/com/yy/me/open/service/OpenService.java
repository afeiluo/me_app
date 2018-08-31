package com.yy.me.open.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yy.cs.base.redis.RedisClientFactory;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.dao.UserMuteGuestMongoDBMapper;
import com.yy.me.enums.PunishSource;
import com.yy.me.http.login.UserLoginUtil;
import com.yy.me.json.JsonUtil;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.thrift.ClientType;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.open.dao.LiveShowMorraMongoDBMapper;
import com.yy.me.open.entity.LiveShowMorraOuter;
import com.yy.me.service.BanService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserSource;
import com.yy.me.yycloud.ApTokenUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.mongo.MongoUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.user.UserInfo.Fields.*;

/**
 * 对外用户个人数据Service
 * 
 * @author JCY
 */
@Service
public class OpenService {

    private static Logger logger = LoggerFactory.getLogger(OpenService.class);


    @Autowired
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private MessageMongoDBMapper messageMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ArrangeAnchorService arrangeAnchorService;

    @Autowired
    private RedisClientFactory cntRedisFactory;

    private static final String bucketStr = "{\"bucket\":\"ourtimes\"}";

    @Autowired
    private BanService banService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    private WaterMarkService waterMarkService;

    @Autowired
    private LiveShowMorraMongoDBMapper liveShowMorraMongoDBMapper;

    @Autowired
    private UserMuteGuestMongoDBMapper userMuteGuestMongoDBMapper;

    public void getUserInfo(String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserInfo userInfo = userHessianService.getClient().getUserByUsername(username);
            if (userInfo != null) {
                Map<String, Object> jo = Maps.newHashMap();
                long uid = userInfo.getUid();
                jo.put(FIELD_U_UID, uid);
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());
                jo.put(FIELD_CREATE_TIME, userInfo.getCreateTime().getTime());
                if (userInfo.getClientType() != null) {
                    jo.put(FIELD_U_CLIENT_TYPE, userInfo.getClientType());
                }
                if (userInfo.getClientVer() != null) {
                    jo.put(FIELD_U_CLIENT_VER, userInfo.getClientVer());
                }
                if (userInfo.getRegPushId() != null) {
                    jo.put(FIELD_U_REG_PUSH_ID, userInfo.getRegPushId());
                } else {
                    List<com.yy.me.message.thrift.push.UserInfo> user = messageMapper.findUserDevices(uid);
                    if (user != null && !user.isEmpty()) {
                        jo.put(FIELD_U_REG_PUSH_ID, user.get(0).getPushId());
                    }
                }
                if (userInfo.getUserSource() != null) {
                    jo.put(FIELD_U_USER_SOURCE, userInfo.getUserSource());
                }
                sendResponse(request, response, genMsgObj(SUCCESS,null, jo));
            } else {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
            }
        } catch (Exception e) {
            logger.error("Get user info error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getUserBasicInfo(String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserInfo userInfo = userHessianService.getClient().getUserByUsername(username);
            if (userInfo != null) {
                Map<String, Object> jo = Maps.newHashMap();
                long uid = userInfo.getUid();
                jo.put(FIELD_U_UID, uid);
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());

                sendResponse(request, response, genMsgObj(SUCCESS,null, jo));
            } else {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
            }
        } catch (Exception e) {
            logger.error("Get user basic info error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public ObjectNode getUserJo(long uid) throws Exception {
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
        if (userInfo != null) {
            ObjectNode jo = JsonUtil.createDefaultMapper().createObjectNode();
            jo.put(FIELD_U_UID, uid);
            jo.put(FIELD_U_USERNAME, userInfo.getUsername());
            jo.put(FIELD_U_NICK, userInfo.getNick());
            jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
            jo.put(FIELD_U_SEX, userInfo.getSex());
            jo.put(FIELD_CREATE_TIME, userInfo.getCreateTime().getTime());
            return jo;
        }
        return null;
    }

    public void getUserBasicInfoByUid(long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            ObjectNode jo = getUserJo(uid);
            if (jo != null) {
                sendResponse(request, response, genMsgObj(SUCCESS,null, jo));
            } else {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
            }
        } catch (Exception e) {
            logger.error("Get user basic info by uid error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void checkUserBan(long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
            if (userInfo != null) {
                if (userInfo.getBaned() instanceof Boolean && userInfo.getBaned()) {
                    Date actionTime = userInfo.getBanedActionTime();
                    if (actionTime == null) {
                        actionTime = userInfo.getBanedEndTime();
                    }
                    String banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), MessageService.dateFormatter.format(actionTime),
                            MessageService.dateFormatter.format(userInfo.getBanedEndTime()));
                    sendResponse(request, response, genMsgObj(USER_BANED, banStr)); // 用户被封禁了
                    return;
                }
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            } else {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
                return;
            }
        } catch (Exception e) {
            logger.error("Check user ban error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void checkUserExistByPhone(String thirdPartyId, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserInfo userInfo = userHessianService.getClient().getUserByThirdPartyId(thirdPartyId);
            if (userInfo != null) {
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());
                sendResponse(request, response, genMsgObj(SUCCESS,null, jo));
            } else {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
            }
        } catch (Exception e) {
            logger.error("Check user info error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    /**
     * 侦测的方法(查一次mongo,链接一次redis)
     *
     */
    public void probeDB(HttpServletRequest request, HttpServletResponse response) {
        try (Jedis redis = cntRedisFactory.getMasterPool().getResource()) {
            userHessianService.getClient().getUserByThirdPartyId("8618565232817");
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Probe error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void batchGetUserBasicInfos(List<String> otherUsernames, List<Long> otherUids, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<UserInfo> userInfoList = Lists.newArrayList();
            if (!otherUsernames.isEmpty()) {
                List<UserInfo> tmp = userHessianService.getClient().findUserListByUsernames(otherUsernames);
                if (tmp != null) {
                    userInfoList.addAll(tmp);
                }
            }
            if (!otherUids.isEmpty()) {
                List<UserInfo> tmp = userHessianService.getClient().findUserListByUids(otherUids, false);
                if (tmp != null) {
                    userInfoList.addAll(tmp);
                }
            }
            if (userInfoList == null || userInfoList.isEmpty()) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }

            List<ObjectNode> joList = Lists.newArrayList();
            Set<Long> uniqueUids = Sets.newHashSet();
            ObjectMapper mapper = getLocalObjMapper();
            for (UserInfo userInfo : userInfoList) {
                long uid = userInfo.getUid();
                if (uniqueUids.contains(uid)) {
                    continue;
                } else {
                    uniqueUids.add(uid);
                }
                ObjectNode jo = mapper.createObjectNode();
                jo.put(FIELD_U_UID, uid);
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());
                joList.add(jo);
            }
            sendResponse(request, response, genMsgObj(SUCCESS, null,joList));
        } catch (Exception e) {
            logger.error("Batch get user basic info error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void batchGetUserBasicInfos4JsonP(List<String> otherUsernames, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<UserInfo> userInfoList = Lists.newArrayList();
            if (!otherUsernames.isEmpty()) {
                List<UserInfo> tmp = userHessianService.getClient().findUserListByUsernames(otherUsernames);
                if (tmp != null) {
                    userInfoList.addAll(tmp);
                }
            }
            if (userInfoList == null || userInfoList.isEmpty()) {
                sendResponseAuto(request, response, genMsgObj(SUCCESS));
                return;
            }

            List<ObjectNode> joList = Lists.newArrayList();
            Set<Long> uniqueUids = Sets.newHashSet();
            ObjectMapper mapper = getLocalObjMapper();
            for (UserInfo userInfo : userInfoList) {
                long uid = userInfo.getUid();
                if (uniqueUids.contains(uid)) {
                    continue;
                } else {
                    uniqueUids.add(uid);
                }
                ObjectNode jo = mapper.createObjectNode();
                jo.put(FIELD_U_UID, uid);
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());
                joList.add(jo);
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS, null,joList));
        } catch (Exception e) {
            logger.error("Batch get user basic info error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }


    public void findUserByUids(List<Long> uidList, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<UserInfo> userInfoList = userHessianService.getClient().findUserListByUids(uidList, false);
            if (userInfoList == null || userInfoList.isEmpty()) {
                sendResponse(request, response, genMsgObj(NOT_EXIST));
                return;
            }

            List<ObjectNode> joList = Lists.newArrayList();
            ObjectMapper mapper = getLocalObjMapper();
            for (UserInfo userInfo : userInfoList) {
                ObjectNode jo = mapper.createObjectNode();
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(FIELD_U_USERNAME, userInfo.getUsername());
                jo.put(FIELD_U_NICK, userInfo.getNick());
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(FIELD_U_SEX, userInfo.getSex());

                joList.add(jo);
            }

            sendResponse(request, response, genMsgObj(SUCCESS,null, joList));
        } catch (Exception e) {
            logger.error("Find user by uids error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void findAllLs(HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowDto> tmp = GeneralLiveShowClient.findAllLs();
            if (tmp == null || tmp.isEmpty()) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            List<ObjectNode> ret = Lists.newArrayListWithExpectedSize(tmp.size());
            Set<Long> anchorUids = arrangeAnchorService.getAllArrangeAnchorWithCache();
            for (LiveShowDto ls : tmp) {
                if (anchorUids.contains(ls.getUid())) {
                    long pcu = ls.getGcrMisaka() == null ? 0L : ls.getGcrMisaka();// 海度用实时真实人气，自己算PCU，所以不用给pcu值
                    ObjectNode jo = getLocalObjMapper().convertValue(ls,ObjectNode.class);
                    jo.put(FIELD_LS_PCU_HIIDO, pcu);
                    jo.put(FIELD_LS_NORMAL_USER_HIIDO, false);

                    ret.add(jo);
                }
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, ret));
        } catch (Exception e) {
            logger.error("Find All LiveShow error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void loadTotalLs(HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowDto> tmp = GeneralLiveShowClient.findAllLs();
            if (tmp == null || tmp.isEmpty()) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            List<ObjectNode> ret = Lists.newArrayListWithExpectedSize(tmp.size());
            for (LiveShowDto ls : tmp) {
                long pcu = ls.getGcrMisaka() == null ? 0L : ls.getGcrMisaka();// 海度用实时真实人气，自己算PCU，所以不用给pcu值
                ObjectNode jo = getLocalObjMapper().convertValue(ls,ObjectNode.class);
                jo.put(FIELD_LS_PCU_HIIDO, pcu);
                ret.add(jo);
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, ret));
        } catch (Exception e) {
            logger.error("Load Total LiveShow error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void queryUserLiveshowStatus(List<Long> uidList, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowDto> liveshows = GeneralLiveShowClient.findLsByUids(uidList);
            List<Map<String, Object>> datas = new ArrayList<>();
            for (LiveShowDto liveshow : liveshows) {
                if (liveshow != null) {
                    Map<String, Object> data = Maps.newHashMap();
                    data.put("uid", liveshow.getUid());
                    data.put("lid", liveshow.getLid());
                    datas.add(data);
                }
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, datas));
        } catch (Exception e) {
            logger.error("Query user liveshow status error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void queryUserLiveshowStatus4JsonP(List<Long> uidList, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowDto> liveshows = GeneralLiveShowClient.findLsByUids(uidList);
            List<Map<String, Object>> datas = Lists.newArrayListWithCapacity(uidList.size());
            for (LiveShowDto liveshow : liveshows) {
                if (liveshow != null) {
                    Map<String, Object> data = Maps.newHashMap();
                    data.put("uid", liveshow.getUid());
                    data.put("lid", liveshow.getLid());
                    datas.add(data);
                }
            }
            sendResponseAuto(request, response, genMsgObj(SUCCESS,null, datas));
        } catch (Exception e) {
            logger.error("Query user liveshow status error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getLiveInfoByLid(String lid, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto dto = GeneralLiveShowClient.getLsByLid(lid);
            if (dto == null) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, getLocalObjMapper().convertValue(dto,JsonNode.class)));
        } catch (Exception e) {
            logger.error("getLiveInfoByLid error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void checkUserToken(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> retMap = Maps.newHashMap();
            retMap.put("uid", -1);
            retMap.put("status", false);
            String[] tmp = token.split(",");
            if (tmp.length >= 2 && !StringUtils.isEmpty(tmp[0]) && !StringUtils.isEmpty(tmp[1])) {
                String cauth = tmp[0];
                String stgt = tmp[1];
                String[] auth = UserLoginUtil.auth4Uid(stgt, cauth);
                if (auth != null) {
                    if (UserLoginUtil.isLegalTime(auth[0], auth[4], auth[5])) {// 部分path可不验证token时间是否过期，其他都需要
                        retMap.put("uid", Long.valueOf(auth[0]));
                        retMap.put("status", true);
                    }
                }
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, retMap));
        } catch (Exception e) {
            logger.error("checkUserToken error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void batchGetUserBasicInfoByUid(List<Long> uidList, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<Long, ObjectNode> retMap = Maps.newHashMap();
            for (Long uid : uidList) {
                ObjectNode jo = getUserJo(uid);
                retMap.put(uid, jo);
            }
            sendResponse(request, response, genMsgObj(SUCCESS, null, retMap));
        } catch (Exception e) {
            logger.error("Batch get user basic info error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getAnchorList(int page, int size, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> retMap = Maps.newHashMap();
            List<Long> uidList = Lists.newArrayListWithExpectedSize(size);

            Set<Long> anchorUidSet = arrangeAnchorService.getAllArrangeAnchorWithCache();

            int total = anchorUidSet.size();

            retMap.put("total", total);
            retMap.put("anchors", uidList);

            int offset = 0;
            if (page > 1) {
                offset = size * (page - 1);
            }

            if (offset >= total) {
                sendResponse(request, response, genMsgObj(SUCCESS,null, retMap));
                return;
            }

            if ((offset + size) > total) {
                size = total - offset;
            }

            int i = 0;
            Iterator<Long> anchorUidIterator = anchorUidSet.iterator();
            while (anchorUidIterator.hasNext()) {
                long uid = anchorUidIterator.next();

                if (i > (offset + size - 1)) {
                    break;
                }

                if (i++ < offset) {
                    continue;
                }

                uidList.add(uid);
            }

            sendResponse(request, response, genMsgObj(SUCCESS,null, retMap));
        } catch (Exception e) {
            logger.error("Get anchor list error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "internal error."));
        }
    }

    public void batchGetLiveshowByUid(long guestUid, List<Long> uidList, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowDto> liveshows = GeneralLiveShowClient.findLsByUids(uidList);
            List<ObjectNode> retList = Lists.newArrayListWithExpectedSize(uidList.size());
            if (liveshows != null) {
                for (LiveShowDto liveshow : liveshows) {
                    if (liveshow != null) {
                        ObjectNode jo =  getLocalObjMapper().convertValue(liveshow,ObjectNode.class);
                        jo.put("token", ApTokenUtils.genSidTokenLocal(guestUid, "LIVE", bucketStr, liveshow.getUid(), 86400, false));

                        // 这里加个status字段，计算方法同
                        // com.yy.tinytimes.liveshow.web.service.LiveShowHtmlService.findOneShareLiveShow()，主要给h5识别
                        int status = liveshow.getStartTime() == null ? 0 : (liveshow.getStartingNow() ? 1 : 2);
                        if (status == 1 && (System.currentTimeMillis() - liveshow.getStartTime().getTime()) < 10 * 1000) {// 10秒以内还算是未开播，因为HLS服务需要时间准备
                            status = 0;
                        }
                        jo.put("startStatus", status);

                        retList.add(jo);

                    }
                }
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, retList));
        } catch (Exception e) {
            logger.error("batchGetLiveshowByUid error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getYYUid(Long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> retObj = Maps.newHashMap();
            retObj.put("yyuid", "0");
            UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
            if (userInfo != null) {
                logger.info("uid:{}'s userSource is {}", uid, userInfo.getUserSource());
                if (UserSource.YY.getValue().equals(userInfo.getUserSource())) {
                    logger.info("uid:{}'s thirdPartyId is {}", uid, userInfo.getThirdPartyId());
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(userInfo.getThirdPartyId()))
                        retObj.put("yyuid", userInfo.getThirdPartyId());
                }
            }
            sendResponse(request, response, genMsgObj(SUCCESS,null, retObj));
        } catch (Exception e) {
            logger.error("get yyuid error");
            sendResponse(request, response, genMsgObj(FAILED, "internal error"));
        }
    }

    public void headViolate(long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            banService.headViolate(PunishSource.OPEN, uid, null, System.currentTimeMillis());
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("headViolate error " + e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, "internal error"));
        }
    }

    public void lsAViolate(long uid, String ruleDesc, HttpServletRequest request, HttpServletResponse response) {
        try {
            Boolean isLinkGuest = false;
            Integer osType = null;
            String deviceId = null;
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
            if (liveShow != null) {
                isLinkGuest = false;
                if (liveShow.getClientType() != null) {
                    osType = "iOS".equals(liveShow.getClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue();
                    deviceId = liveShow.getDeviceId() != null ? liveShow.getDeviceId() : null;
                }
                logger.info("Open punishLive: Close LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
            } else {
                liveShow = GeneralLiveShowClient.getLsByLinkUid(uid);
                if (liveShow != null) {
                    isLinkGuest = true;
                    if (liveShow.getLinkClientType() != null) {
                        osType = "iOS".equals(liveShow.getLinkClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue();
                        deviceId = liveShow.getLinkDeviceId() != null ? liveShow.getLinkDeviceId() : null;
                    }
                    logger.info("Open punishLive: Cancel Link from LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
                }
            }

            String lid = liveShow == null ? null : liveShow.getLid();
            logger.info("Open punishLive lsAViolate,lid:{},uid:{},osType:{},deviceId:{}", lid, uid, osType, deviceId);
            banService.lsAViolate(PunishSource.OPEN, uid, lid, null, System.currentTimeMillis(), osType, deviceId, isLinkGuest, ruleDesc);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("lsAViolate error " + e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, "internal error"));
        }
    }

    public void lsBViolate(long uid, String ruleDesc, HttpServletRequest request, HttpServletResponse response) {
        try {
            Boolean isLinkGuest = false;
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
            if (liveShow != null) {
                isLinkGuest = false;
                logger.info("Open punishLive: Close LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
            } else {
                liveShow = GeneralLiveShowClient.getLsByLinkUid(uid);
                if (liveShow != null) {
                    isLinkGuest = true;
                    logger.info("Open punishLive: Cancel Link from LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
                }
            }
            String lid = liveShow == null ? null : liveShow.getLid();
            logger.info("Open punishLive lsBViolate,uid:{}, lid:{}", uid, lid);
            banService.lsBViolate(PunishSource.OPEN, uid, lid, null, System.currentTimeMillis(), isLinkGuest, ruleDesc);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("lsBViolate error " + e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, "internal error"));
        }
    }

    public void replaceWaterMarkWhiteList(Long[] uids, String id, HttpServletRequest request, HttpServletResponse response) {
        try {
            waterMarkService.replaceWhiteList(id, Arrays.asList(uids));
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("replaceWaterMarkWhiteList error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "replaceWaterMarkWhiteList error"));
        }
    }

    public void addWaterMarkWhiteList(Long[] uids, String id, HttpServletRequest request, HttpServletResponse response) {
        try {
            waterMarkService.addWhiteList(id, Arrays.asList(uids));
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("addWaterMarkWhiteList error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "addWaterMarkWhiteList error"));
        }
    }

    public void delWaterMarkWhiteList(Long[] uids, String id, HttpServletRequest request, HttpServletResponse response) {
        try {
            waterMarkService.delWhiteList(id, Arrays.asList(uids));
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("delWaterMarkWhiteList error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "delWaterMarkWhiteList error"));
        }
    }

    public void turnoverOpPush(long uid, long recvUid, String comment, String img, HttpServletRequest request, HttpServletResponse response) {
        try {
            messageService.pushTurnoverOpMsgRubbish(uid, comment, img, recvUid);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("turnoverOpPush error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "push error"));
        }
    }

    public void turnoverChannelPush(long uid, long recvUid, String comment, String img, HttpServletRequest request, HttpServletResponse response) {
        try {
            LiveShowDto liveShowDto = GeneralLiveShowClient.getLsByUid(recvUid);
            if (liveShowDto != null) {
                messageService.pushTurnoverOpMsgRubbish2Channel(uid, recvUid, comment, img, liveShowDto.getLid());
            } else {
                logger.warn("turnoverChannelPush recvUid not found lid,uid:{},recvUid:{}", uid, recvUid);
            }
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("turnoverOpPush error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "push error"));
        }
    }

    public void getMorraList(long uid, long startTime, long endTime, HttpServletRequest request, HttpServletResponse response) {
        try {
            List<LiveShowMorraOuter> ret = liveShowMorraMongoDBMapper.findUserLiveShow(uid, startTime, endTime);
            sendResponse(request, response, genMsgObj(SUCCESS,null, ret));
        } catch (Exception e) {
            logger.error("getMorraList error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "getMorraList error"));
        }
    }

    // TODO 查询某用户是否在某直播间
    public void checkUserLiveInfo(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        int status = 1;
        Map<String, Integer> retMap = new HashMap<>();
        retMap.put("status", status);
        try {
            sendResponse(request, response, genMsgObj(SUCCESS,null, retMap));
        } catch (Exception e) {
            logger.error("checkUserLiveInfo error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "checkUserLiveInfo error"));
        }
    }

    public void cancelMute(long anchorUid,long guestUid,HttpServletRequest request, HttpServletResponse response) {
        try {
            userMuteGuestMongoDBMapper.cancelMute(anchorUid,guestUid);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("cancelMute error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "cancelMute error"));
        }
    }
}
