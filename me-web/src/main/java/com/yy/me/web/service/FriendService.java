package com.yy.me.web.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yy.cs.base.redis.RedisClientFactory;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.HeaderReviewMongoDBMapper;
import com.yy.me.friend.bean.UserFriendMsg;
import com.yy.me.friend.entity.*;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.service.inner.FillService;
import com.yy.me.time.MaskClock;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_CREATETIME;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_UID;

/**
 * 好友关系Service
 *
 * @author Phil
 * @version 2016-10-13
 */
@Service
public class FriendService {

    private static Logger logger = LoggerFactory.getLogger(FriendService.class);

    //private static Cache<String, List<Map<String, Object>>> chatCache = CacheBuilder.newBuilder().maximumSize(100000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    public static int BE_FRIEND = 2;//like后成为好友
    public static int LIKE_COUNT_LIMIT = -100;//每个用户一天内喜欢超过100个人
    public static int LIKE_TIME_LIMIT = -101;//再次喜欢7天限制
    public static int SUPERLIKE_COUNT_LIMIT = -100;//一天只能超喜欢100人
    public static int SUPERLIKE_TIME_LIMIT = -102;//一天对一个用户只能超喜欢一次
    //public static int ALREADY_FRIEND = -100;//已经是好友了
    //public static int ALREADY_OVER_COUNT = -101;//每日好友申请超过100次
    //public static int HEARDURL_NEED_CHECK = -102;//头像审核不合格
    public static int ALREADY_BE_APPLIED = -103;//对方已经发出申请

    private static final String APPLY_COUNT_JEDIS_KEY = "fac_%s_%s";//Redis 好友申请数key模板
    private static final String LIKE_COUNT_JEDIS_KEY = "like_%s_%s";//Redis 喜欢数key模板
    private static final String APPLY_HEADURL_LIMIT_JEDIS_KEY = "fhl_%s_%s";//Redis 头像未审核限制申请数key模板
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private RedisClientFactory cntRedisFactory;

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    private FillService fillService;

    @Autowired
    private HeaderReviewMongoDBMapper headerReviewMongoDBMapper;

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<com.yy.me.friend.FriendService> hessianFactory;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    private com.yy.me.friend.FriendService getClient() {
        return hessianFactory.getClient();
    }

    public void applyMarkRead(Long uid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Response res = getClient().updateUserViewApplyListTime(uid);
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend apply markread exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend apply markread exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend apply markread success. uid:{}, spend time:{}", uid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "markRead",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void applyCount(Long uid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Long applyCount = getClient().getNewAppliedFriendCount(uid);

            Map<String, Object> res = Maps.newLinkedHashMap();

            getFriendCount(uid, res);

            res.put("count", applyCount);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend apply count exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend apply count exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend apply count success. uid:{}, spend time:{}", uid, MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "applyCount",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void applyReply(String aid, Long uid, Long friendUid, String content, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            //Response res = getClient().reply(aid, uid, friendUid, content, null);
            Response res = null;
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend apply reply exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend apply reply exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend apply reply success. aid:{}, uid:{}, friendUid:{}, spend time:{}", aid, uid, friendUid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "applyReply",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void cardList(Long uid, Long lastUid, Integer size, Long tid, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Pair<List<UserFriendMsg>,Long> pair = getClient().getUserFriendMsg(uid, tid, lastUid, size);
            List<UserFriendMsg> userFriendMsgs = pair.getLeft();
            Long total = pair.getRight();
            List<Long> uids = Lists.transform(userFriendMsgs, new Function<UserFriendMsg, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable UserFriendMsg input) {
                    return input.getUid();
                }
            });
            List<Long> allUids = Lists.newArrayListWithCapacity(uids.size() + 1);
            allUids.addAll(uids);
            allUids.add(uid);
            List<UserInfo> userInfos = userHessianService.getClient().findUserListByUids(allUids, true);
            Map<Long, UserInfo> userInfoMaps = Maps.uniqueIndex(userInfos, new Function<UserInfo, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable UserInfo input) {
                    return input.getUid();
                }
            });
            UserInfo myUserInfo = userInfoMaps.get(uid);
            List<Map<String, Object>> friends = Lists.newArrayList();
            int year=new DateTime().getYear();
            for (UserFriendMsg userFriendMsg : userFriendMsgs) {
                Map<String, Object> friend = Maps.newLinkedHashMap();
                if (userInfoMaps != null) {
                    UserInfo info = userInfoMaps.get(userFriendMsg.getUid());
                    if (info != null) {
                        friend.put("uid", userFriendMsg.getUid());
                        friend.put("nick", info.getNick());
                        friend.put("headerUrl", info.getHeaderUrl());
                        friend.put("age", getAge(info.getYear(),year));
                        friend.put("sex", info.getSex());
                        friend.put("type", userFriendMsg.getType());
                        if(userFriendMsg.getMsg()!=null){
                            friend.put("msg",userFriendMsg.getMsg());
                        }
                        if(userFriendMsg.getName()!=null){
                            friend.put("name",userFriendMsg.getName());
                        }
                        friend.put("lastActiveTime",
                                info.getLastActiveTime() == null ? 0 : info.getLastActiveTime().getTime());
                        friend.put("distance", getDistance(myUserInfo, info));
                        friends.add(friend);
                    }
                }
            }

            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("list", friends);
            res.put("total",total);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend cardList exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend cardList exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend cardList success. uid:{}, spend time {}", uid, MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "cardList",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void card(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            UserFriendMsg userFriendMsg = getClient().getSpecificUserFriendMsg(uid, tid);

            List<Long> allUids = Lists.newArrayListWithCapacity(2);
            allUids.add(tid);
            allUids.add(uid);
            List<UserInfo> userInfos = userHessianService.getClient().findUserListByUids(allUids, true);
            Map<Long, UserInfo> userInfoMaps = Maps.uniqueIndex(userInfos, new Function<UserInfo, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable UserInfo input) {
                    return input.getUid();
                }
            });
            UserInfo myUserInfo = userInfoMaps.get(uid);
            List<Map<String, Object>> friends = Lists.newArrayList();

                Map<String, Object> friend = Maps.newLinkedHashMap();
                if (userInfoMaps != null) {
                    UserInfo info = userInfoMaps.get(userFriendMsg.getUid());
                    if (info != null) {
                        friend.put("uid", userFriendMsg.getUid());
                        friend.put("nick", info.getNick());
                        friend.put("headerUrl", info.getHeaderUrl());
                        friend.put("age", getAge(info.getYear(),new DateTime().getYear()));
                        friend.put("sex", info.getSex());
                        friend.put("type", userFriendMsg.getType());
                        if(userFriendMsg.getMsg()!=null){
                            friend.put("msg",userFriendMsg.getMsg());
                        }
                        if(userFriendMsg.getName()!=null){
                            friend.put("name",userFriendMsg.getName());
                        }
                        friend.put("lastActiveTime",
                                info.getLastActiveTime() == null ? 0 : info.getLastActiveTime().getTime());
                        friend.put("distance", getDistance(myUserInfo, info));
                        friends.add(friend);
                    }
                }
            sendResp(request, response, genMsgObj(SUCCESS, null, friend));
        } catch (Exception ex) {
            logger.error("friend cardList exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend cardList exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend cardList success. uid:{}, spend time {}", uid, MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "cardList",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    private String getDistance(UserInfo userInfo, UserInfo userInfo2) {
        double[] location1 = getUserLocation(userInfo);
        double[] location2 = getUserLocation(userInfo2);
        double distance = DistIpUtil.calDistance(location1[0], location1[1], location2[0], location2[1]);
        return DistIpUtil.getDisStr(distance);
    }

    private double[] getUserLocation(UserInfo userInfo) {
        double[] ret = new double[2];
        ret[0] = DistIpUtil.xinJiangWuLuMuQi[0];
        ret[1] = DistIpUtil.xinJiangWuLuMuQi[1];
        if (DistIpUtil.judgeCoordinates(new Double[] { userInfo.getLongitude(), userInfo.getLatitude() })) {
            ret[0] = userInfo.getLongitude();
            ret[1] = userInfo.getLatitude();
        }
        return ret;
    }

    public void remove(Long uid, Long friendUid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Response res = getClient().delete(uid, friendUid, null);
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend remove exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend remove exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend remove success. uid:{}, friendUid:{}, spend time:{}", uid, friendUid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "remove",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }


    public void pullBlack(Long uid, Long friendUid, String lid, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Response res = getClient().shieldAdv(uid, friendUid, lid);
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend pullBlack exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend pullBlack exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend pullBlack success. uid:{}, friendUid:{}, spend time:{}", uid, friendUid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "pullBlack",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void cancelBlack(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Response res = getClient().deleteShield(uid, tid);
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend cancelBlack exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend cancelBlack exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend cancelBlack success. uid:{}, tid:{}, spend time:{}", uid, tid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "cancelBlack",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void blackList(Long uid, Long lastTime, int limit, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<ShieldUser> list = getClient().getShieldList(uid, lastTime, limit);
            List<ObjectNode> nodes = fillService.fillUserInfo(Lists.transform(list, new Function<ShieldUser, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable ShieldUser input) {
                    return input.getTid();
                }
            }));
            Map<Long, ShieldUser> map = Maps.uniqueIndex(list, new Function<ShieldUser, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable ShieldUser input) {
                    return input.getTid();
                }
            });
            for (ObjectNode node : nodes) {
                node.put(FIELD_U_CREATETIME, map.get(node.get(FIELD_U_UID).asLong()).getCreateTime().getTime());
            }
            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("list", nodes);
            sendResp(request, response, res);
        } catch (Exception ex) {
            logger.error("friend blackList exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend blackList exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend blackList success. uid:{}, lastTime:{}, limit:{}, spend time:{}", uid, lastTime, limit,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "blackList",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void list(Long uid, Integer near, Integer page, Integer pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<Friend> friendList = getClient().getAllFriend(uid);
            logger.debug("rpc:getFriends spend time: {}", MaskClock.getCurtime() - startTime);

            List<Long> uids = Lists.transform(friendList, new Function<Friend, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable Friend input) {
                    return input.getUid();
                }
            });

            List<Long> uidsAll = Lists.newArrayList();
            uidsAll.addAll(uids);
            uidsAll.add(uid);
            List<UserInfo> userInfos = userHessianService.getClient().findUserListByUids(uidsAll, true);

            logger.debug("rpc:findUserListByUids spend time: {}", MaskClock.getCurtime() - startTime);

            Map<Long, UserInfo> userInfoMaps = null;
            if (userInfos != null && userInfos.size() > 0) {
                userInfoMaps = Maps.uniqueIndex(userInfos, new Function<UserInfo, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable UserInfo input) {
                        return input.getUid();
                    }
                });
            }
            List<Map<String, Object>> friends = Lists.newArrayList();
            UserInfo curUser = userInfoMaps.get(uid);

            for (Friend friendEntity : friendList) {
                if (userInfoMaps != null) {
                    UserInfo info = userInfoMaps.get(friendEntity.getUid());
                    if (info != null) {
                        friends.add(getFriendInfo(curUser, info, friendEntity.getRelation()));
                    }
                }
            }

            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("friends", friends);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend list exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend list exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend list success. uid:{}, near:{}, spend time: {}", uid, near,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "list",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void list_3_0(Long uid, Integer near, Integer page, Integer pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<Friend> friendList = Lists.newArrayList();
            /*if(near == null || near == -1){
                friendList = getClient().getFriends(uid, null);
            }else if(near == 1){
                friendList = getClient().getFriends(uid, true);
            }else{
                friendList = getClient().getFriends(uid, false);
            }*/
            friendList = getClient().getAllFriend(uid);
            logger.debug("rpc:getFriends spend time: {}", MaskClock.getCurtime() - startTime);

            List<Long> uids = Lists.transform(friendList, new Function<Friend, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable Friend input) {
                    return input.getUid();
                }
            });
            List<UserInfo> userInfos = userHessianService.getClient().findUserListByUids(uids, true);

            logger.debug("rpc:findUserListByUids spend time: {}", MaskClock.getCurtime() - startTime);

            Map<Long, UserInfo> userInfoMaps = null;
            if (userInfos != null && userInfos.size() > 0) {
                userInfoMaps = Maps.uniqueIndex(userInfos, new Function<UserInfo, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable UserInfo input) {
                        return input.getUid();
                    }
                });
            }
            List<Map<String, Object>> friends = Lists.newArrayList();

            Calendar calendar = Calendar.getInstance();
            int currentYear = calendar.get(Calendar.YEAR);
            int age = 0;

            for (Friend friendEntity : friendList) {
                Map<String, Object> friend = Maps.newLinkedHashMap();
                if (userInfoMaps != null) {
                    UserInfo info = userInfoMaps.get(friendEntity.getUid());
                    if (info != null) {
                        friend.put("uid", info.getUid());
                        friend.put("nick", info.getNick());
                        friend.put("headerUrl", info.getHeaderUrl());
                        friend.put("verified", info.getVerified());
                        friend.put("sex", info.getSex());
                        friend.put("signature", info.getSignature());
                        friend.put("topMedal", info.getTopMedal());

                        if (near == null || near == -1) {
                            friend.put("relation", friendEntity.getRelation()); // 0:亲密 1:非亲密
                        }

                        if (info.getYear() != null) {
                            age = currentYear - info.getYear();
                        } else {
                            age = 0;
                        }

                        friend.put("age", age);
                        friend.put("interact", "");
                        friend.put("interactDate", friendEntity.getLastInteract());

                        friends.add(friend);
                    }
                }
            }

            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("friends", friends);

            getFriendCount(uid, res);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend list exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend list exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend list success. uid:{}, near:{}, spend time: {}", uid, near,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "list",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void check(Long uid, List<Long> friendUids, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<Long> friends = getClient().checkFriends(uid, friendUids);

            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("friends", friends);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend check exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend check exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend check success. uid:{}, friendUids:{}, spend time:{}", uid, friendUids, MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "checkFriends", MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void check_3_0(Long uid, List<Long> friendUids, Integer near, HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Set<Long> friendUidSet = Sets.newHashSet();
            Boolean neared; //是否亲密好友

            if (near == null) {
                neared = null;
            } else if (near == 1) {
                neared = true;
            } else {
                neared = false;
            }

            //friendUidSet = getClient().checkFriend(uid, friendUids, neared);

            Map<String, Object> res = Maps.newLinkedHashMap();
            res.put("friends", friendUidSet);

            sendResp(request, response, genMsgObj(SUCCESS, null, res));
        } catch (Exception ex) {
            logger.error("friend check exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend check exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend check success. uid:{}, friendUids:{}, spend time:{}", uid, friendUids,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "checkFriends",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void like(Long uid, Long friendUid, Boolean cancel, int from, String name,
            HttpServletRequest request,
            HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Response res;
            if (!cancel) {
                int likeCount = cntConfService.fetchConfInt(CntConfService.DEFAULT_CONF_ID_FRIEND_APPLY_LIMIT, "applyCount", 100);
                if(getLikeCount(uid, new Date()) > likeCount){
                    sendResp(request, response, genMsgObj(LIKE_COUNT_LIMIT, String.format("friend like must less than %s everyday.", likeCount)));
                    return;
                }
                LikeType likeType=LikeType.LIKE;
                LikeExtra.Builder likeExtraBuilder=new LikeExtra.Builder();
                if(from==1){
                    likeExtraBuilder.name(name);
                }
                res = getClient().like(uid, friendUid, likeType,likeExtraBuilder.build());

                if(res != null && (res.getResponseCode() == ResponseCode.SUCCESS
                        ||res.getResponseCode() == ResponseCode.BE_FRIEND)){
                    incrLikeCount(uid, new Date());
                }

                if(res.getResponseCode() == ResponseCode.BE_FRIEND){
                    sendResponse(request, response, genMsgObj(BE_FRIEND, null, getFriendData(uid, friendUid, request)));
                }else{
                    sendResp(request, response, res);
                }
            } else {
                res = getClient().disLike(uid, friendUid, null);

                sendResp(request, response, res);
            }
        } catch (Exception ex) {
            logger.error("friend like exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend like exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend like success. uid:{}, friendUids:{}, cancel:{}, spend time:{}", uid, friendUid, cancel,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "likeFriends",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void cardSkip(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            getClient().skipUserFriendMsg(uid,tid);
            sendResp(request, response, genMsgObj(SUCCESS));
        } catch (Exception ex) {
            logger.error("friend like exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend like exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend cardSkip success. uid:{}, tid:{},spend time:{}", uid, tid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "likeFriends",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void canSuperLike(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            int ret=getClient().canSuperLike(uid,tid);
            if(ret>=0) {
                sendResp(request, response, genMsgObj(SUCCESS));
            }else if(ret==-1) {
                sendResp(request, response, genMsgObj(SUPERLIKE_COUNT_LIMIT));
            }else if(ret==-2){
                sendResp(request, response, genMsgObj(SUPERLIKE_TIME_LIMIT));
            }
        } catch (Exception ex) {
            logger.error("friend like exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend like exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend cardSkip success. uid:{}, tid:{},spend time:{}", uid, tid,
                    MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "likeFriends",
                    MaskClock.getCurtime() - startTime, resCode);
        }
    }

    public void info(Long uid, Long friendUid, HttpServletRequest request, HttpServletResponse response) {
        long startTime = MaskClock.getCurtime();
        int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Map<String, Object> data = getFriendData(uid, friendUid, request);

            sendResponse(request, response, genMsgObj(SUCCESS, null, data.get("friend")));
        } catch (Exception ex) {
            logger.error("friend info exception.", ex);
            sendResp(request, response, genMsgObj(FAILED, "friend info exception."));
            resCode = MetricsClient.RESCODE_FAIL;
        } finally {
            logger.debug("friend info success. uid:{}, friendUids:{}, spend time:{}", uid, friendUid, MaskClock.getCurtime() - startTime);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "friend", this.getClass(), "info", MaskClock.getCurtime() - startTime, resCode);
        }
    }

    private Map<String, Object> getFriendData(Long uid, Long friendUid, HttpServletRequest request){
        UserInfo curUser = getUserInfo(request);
        UserInfo info = userHessianService.getClient().getUserByUid(friendUid, true);
        int relation = getClient().getUserInnerRelation(uid, friendUid);

        Map<String, Object> data = Maps.newLinkedHashMap();

        data.put("friend", getFriendInfo(curUser, info, relation));

        return data;
    }

    private Map<String, Object> getFriendInfo(UserInfo myInfo, UserInfo friendInfo, int relation){
        Map<String, Object> friend = Maps.newLinkedHashMap();
        if (friendInfo != null) {
            friend.put("uid", friendInfo.getUid());
            friend.put("nick", friendInfo.getNick());
            friend.put("headerUrl", friendInfo.getHeaderUrl());
            friend.put("verified", friendInfo.getVerified());
            friend.put("userStatus", friendInfo.getUserStatus());
            friend.put("sex", friendInfo.getSex());
            friend.put("signature", friendInfo.getSignature());
            friend.put("topMedal", friendInfo.getTopMedal());
            friend.put("distance", getDistance(myInfo, friendInfo));
            friend.put("relation", relation);
            friend.put("lastActiveTime", friendInfo.getLastActiveTime() != null ? friendInfo.getLastActiveTime().getTime() : 0);
        }
        return friend;
    }

    private void getFriendCount(Long uid, Map<String, Object> res) {
        Long friendCount = getClient().getFriendCount(uid);

        if (friendCount != null) {
            res.put("friendCount", friendCount);
        } else {
            res.put("friendCount", 0);
        }
    }

    public Long getLikeCount(Long uid, Date time) {
        Long count = 0L;
        try (Jedis redis = cntRedisFactory.getMasterPool().getResource()) {
            String key = String.format(LIKE_COUNT_JEDIS_KEY, uid, simpleDateFormat.format(time));
            if (!redis.exists(key)) {
                redis.set(key, "0");
                redis.expire(key, 24 * 60 * 60); // expired after one day
            }

            count = Long.parseLong(redis.get(key));
        } catch (Exception e) {
            logger.error("increase friend apply count from redis error.", e);
        }

        return count;
    }

    public Long incrLikeCount(Long uid, Date time) {
        Long count = 0L;
        try (Jedis redis = cntRedisFactory.getMasterPool().getResource()) {
            String key = String.format(LIKE_COUNT_JEDIS_KEY, uid, simpleDateFormat.format(time));
            if (!redis.exists(key)) {
                redis.set(key, "0");
                redis.expire(key, 24 * 60 * 60); // expired after one day
            }

            count = redis.incr(key);
        } catch (Exception e) {
            logger.error("increase friend apply count from redis error.", e);
        }

        return count;
    }

    public Long getApplyHeadUrlLimit(Long uid, Long friendUid, Date time) {
        Long count = 0L;
        try (Jedis redis = cntRedisFactory.getMasterPool().getResource()) {
            String key = String.format(APPLY_HEADURL_LIMIT_JEDIS_KEY, uid, simpleDateFormat.format(time));

            if (redis.sismember(key, String.valueOf(friendUid))) {
                count = 0l;
            } else {
                count = redis.scard(key);
            }
        } catch (Exception e) {
            logger.error("increase friend apply headUrl limit from redis error.", e);
        }

        return count;
    }

    public Long incrApplyHeadUrlLimit(Long uid, Long friendUid, Date time, int limit) {
        Long count = 0L;
        try (Jedis redis = cntRedisFactory.getMasterPool().getResource()) {
            String key = String.format(APPLY_HEADURL_LIMIT_JEDIS_KEY, uid, simpleDateFormat.format(time));

            count = redis.scard(key);

            if (count < limit) {
                if (!redis.exists(key)) {
                    redis.sadd(key, String.valueOf(friendUid));
                    redis.expire(key, 24 * 60 * 60); // expired after one day

                    count++;
                } else if (!redis.sismember(key, String.valueOf(friendUid))) {
                    redis.sadd(key, String.valueOf(friendUid));

                    count++;
                }
            }
        } catch (Exception e) {
            logger.error("increase friend apply headUrl limit from redis error.", e);
        }

        return count;
    }

    private void sendResp(HttpServletRequest request, HttpServletResponse response, Object obj) {
        RetMsgObj res;
        if (obj instanceof Response) {
            Response resp = (Response) obj;
            if(resp==null){
                res = genMsgObj(FAILED, "friend hessian service response empty.");
            }else if (resp.getResponseCode() == ResponseCode.SUCCESS) {
                res = genMsgObj(SUCCESS, null, null);
            } else if(resp.getResponseCode() == ResponseCode.BE_FRIEND){
                res = genMsgObj(BE_FRIEND, null, null);
            }else if (resp.getResponseCode() == ResponseCode.ALREADY_BE_APPLIED) {
                res = genMsgObj(ALREADY_BE_APPLIED, resp.getMsg(), null);
            } else if (resp.getResponseCode() == ResponseCode.LIKE_TIME_LIMIT) {
                Map<String, Object> data = Maps.newLinkedHashMap();
                String limitDays = resp.getMsg();
                data.put("limit", StringUtils.isEmpty(limitDays) ? 0 : Integer.parseInt(limitDays));
                res = genMsgObj(LIKE_TIME_LIMIT, "friend like time limit.", data);
            } else {
                res = genMsgObj(FAILED, resp.getMsg());
            }
        } else if (obj instanceof RetMsgObj) {
            res = (RetMsgObj) obj;
        } else {
            res = genMsgObj(SUCCESS, null, obj);
        }
        sendResponse(request, response, res);
    }

    public int getFriendRelation(long uid, long toUid) {
        return  getClient().getUserInnerRelation(uid, toUid);
    }

    private int getAge(Integer year,int now){
        if(year==null){
            return 1;
        }
        int age=now-year;
        return age>0?age:1;
    }



}
