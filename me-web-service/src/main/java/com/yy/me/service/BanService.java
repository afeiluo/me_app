package com.yy.me.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.yy.me.enums.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.mongodb.DBObject;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.bs2.Bs2Service;
import com.yy.me.dao.HeaderReviewMongoDBMapper;
import com.yy.me.dao.PunishHistoryMongoDBMapper;
import com.yy.me.dao.ReportCountMongoDBMapper;
import com.yy.me.dao.UserBanMongoDBMapper;
import com.yy.me.entity.PunishHistory;
import com.yy.me.entity.RuleDesc;
import com.yy.me.liveshow.client.entity.LsAction;
import com.yy.me.liveshow.client.mq.LsBroadcastAloProducer;
import com.yy.me.liveshow.client.util.LsHeaderChecker;
import com.yy.me.liveshow.enums.StatisticsType;
import com.yy.me.liveshow.thrift.LsRequestHeader;
import com.yy.me.liveshow.util.GeneralUtil;
import com.yy.me.message.FormatMessage;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.message.MsgDataType;
import com.yy.me.service.inner.MessageService;
import com.yy.me.thread.ThreadUtil;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserInfoUtil;
import com.yy.me.util.search.HiddoSearch;

@Service
public class BanService {
    private static Logger logger = LoggerFactory.getLogger(BanService.class);

    @Autowired
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private LiveShowService liveShowService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    private UserBanMongoDBMapper userBanMongoDBMapper;

    @Autowired
    private PunishHistoryMongoDBMapper punishHistoryMapper;

    @Autowired
    private HeaderReviewMongoDBMapper headerReviewMongoDBMapper;

    @Autowired
    private LsBroadcastAloProducer lsBroadcastAloProducer;

    public final static FastDateFormat dateFormatter = FastDateFormat.getInstance("yyyy年MM月dd日  HH:mm:ss");

    private ScheduledExecutorService executor= Executors.newSingleThreadScheduledExecutor(ThreadUtil.buildThreadFactory("banService-unregistUser",true));

    @Autowired
    private ReportCountMongoDBMapper reportCountMongoDBMapper;

    private void sendBannedMsg(long uid, ViolationMsgType violationMsgType, long actionTime, long bannedEndTime, PunishSource punishSource,
                               String reason) {
        LsRequestHeader header = LsHeaderChecker.checkHeader(null);
        String transId = GeneralUtil.findTransId(header);
        LsAction lsAction = LsAction.start(transId, StatisticsType.USER_BANNED, messageService.genPartialOrder()).initByHeader(header).setActUid(uid)
                .setBannedType(violationMsgType.getValue()).setActionTime(actionTime).setBannedEndTime(bannedEndTime)
                .setPunishSource(punishSource.getSource()).setPunishReason(reason);
        lsBroadcastAloProducer.sendLsActMsg(lsAction);
    }

    private void sendBannedMsg(long uid, MsgDataType dataType, long actionTime, long bannedEndTime, PunishSource punishSource, String reason) {
        LsRequestHeader header = LsHeaderChecker.checkHeader(null);
        String transId = GeneralUtil.findTransId(header);
        LsAction lsAction = LsAction.start(transId, StatisticsType.USER_BANNED, messageService.genPartialOrder()).initByHeader(header).setActUid(uid)
                .setBannedType(dataType.getValue()).setActionTime(actionTime).setBannedEndTime(bannedEndTime)
                .setPunishSource(punishSource.getSource()).setPunishReason(reason);
        lsBroadcastAloProducer.sendLsActMsg(lsAction);
    }


    /**
     * "ruleDesc": "A-2-2.2.2"
     * A 对应的是严重  B-一般   C-轻微
     * 第2条，第2.2.2章节
     */
    private RuleDesc parseRuleDesc(String ruleDesc) {
        if (StringUtils.isNotBlank(ruleDesc)) {
            String[] items = ruleDesc.split("-");
            if (items.length == 3) {
                RuleDesc rule = new RuleDesc();
                String tempDesc = items[0];
                if (tempDesc.equals("A")) {
                    rule.setDesc("严重");
                } else if (tempDesc.equals("B")) {
                    rule.setDesc("一般");
                } else if (tempDesc.equals("C")) {
                    rule.setDesc("轻微");
                }
                rule.setItem(items[1]);
                rule.setSubItem(items[2]);
                return rule;
            } else {
                logger.error("parse error,ruleDesc:{}", ruleDesc);
            }
        } else {
            logger.error("ruleDesc blank:{}", ruleDesc);
        }
        return null;
    }

    public void headViolate(PunishSource punishSource, long uid, String reason, long actionTime) throws Exception {
        // 计算此用户已经被处罚了第几次
        int violateCount = userBanMongoDBMapper.incUserHeadBan(uid);
        ViolationType violationType = null;
        ViolationMsgType violationMsgType =  ViolationMsgType.U_BANED_TIPS_HEAD_ONE_TIME;;
        long banedEndTime = System.currentTimeMillis();
        violationType = ViolationType.U_BANED_HEAD_ONE_TIME;

        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);
        if (!UserInfoUtil.DEFAULT_PIC.equals(userInfo.getHeaderUrl())) {
            Bs2Service.deletePicUrl(userInfo.getHeaderUrl());
            UserInfo update = new UserInfo();
            update.setUid(uid);
            update.setHeaderUrl(UserInfoUtil.DEFAULT_PIC);
            userHessianService.getClient().updateByUid(update);// 删除并恢复默认头像
            headerReviewMongoDBMapper.remove(uid);
            userHessianService.getClient().removeUserMedal(uid,1);
        }
        logger.info("Ban User Head! uid:{}, violationMsgType:{}, violateCount:{}, actionTime:{}, banedEndTime:{}, userInfo:{}", uid,
                violationMsgType.getValue(), violateCount, actionTime, banedEndTime, userInfo);


        // 不封号惩罚，不退出登录
//        messageService.insertUserViolationMsg(violationType.getValue(), actionTime, uid, false);// 存用户可见信箱
//        messageService.sendUserViolationTips(violationMsgType.getValue(), actionTime, banedEndTime, userInfo, false);// 仅发送弹窗处罚消息
        String tips = FormatMessage.HEAD_PUNISH_TIPS;
        messageService.baseInsertUserViolationMsg(tips,uid);
//        messageService.baseSendUserViolationTips(tips,userInfo);
        messageService.sendPushAndInsertCtrlMsg(uid,tips,MsgDataType.S_HEAD_PUNISH);//V3.1

        String msgStr = messageMongoDBMapper.fm(violationMsgType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime));

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);
        punishHistory.setType(PunishType.HEAD.getType());
        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date(actionTime));

        punishHistoryMapper.create(punishHistory);
    }

    public void textViolate(PunishSource punishSource, PunishType punishType, final long uid, String reason, long actionTime, boolean extraPunish, HiddoSearch hiddoSearch) throws Exception {
        ViolationType violationType =null;
        ViolationMsgType violationMsgType =null;
        if(punishType==PunishType.NICK){
            violationType=ViolationType.U_BANED_NICK;
            violationMsgType=ViolationMsgType.U_BANED_TIPS_NICK;
        }else if(punishType==PunishType.SIGNATURE){
            violationType=ViolationType.U_BANED_SIGNATURE;
            violationMsgType=ViolationMsgType.U_BANED_TIPS_SIGNATURE;
        }else {
            return;
        }
        long banedEndTime = -1;
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);

        logger.info("Ban User Head! uid:{}, violationMsgType:{}, actionTime:{}, banedEndTime:{}, userInfo:{}", uid,
                violationMsgType.getValue(), actionTime, banedEndTime, userInfo);

//        liveShowService.closeViolatedLiveShowByOwner(uid);
//        liveShowService.guestLeaveAllLsThrift(uid, null);
//
//        UserInfo setUserInfo  = new UserInfo();
//        setUserInfo.setUid(uid);
//        setUserInfo.setBaned(true);
//        setUserInfo.setBanedType(violationMsgType.getValue());
//        setUserInfo.setBanedActionTime(new Date(actionTime));
//        setUserInfo.setBanedEndTime(new Date(banedEndTime));
//        userHessianService.getClient().updateByUid(setUserInfo);

        sendBannedMsg(uid, violationMsgType, actionTime, banedEndTime, punishSource, reason);

//        messageService.sendUserQuitNew(uid, violationMsgType.getValue(), actionTime, banedEndTime);// 退出登录

        messageService.insertUserViolationMsg(violationType.getValue(), actionTime, uid, true);// 存用户可见信箱
//        messageService.sendUserViolationTips(violationMsgType.getValue(), actionTime, banedEndTime, userInfo, true);// 发送弹窗处罚消息
        String tips = "";
        if(punishType==PunishType.NICK){
            tips= FormatMessage.NICK_PUNISH_TIPS;
        }
        else if(punishType==PunishType.SIGNATURE){
            tips=FormatMessage.SIGNATURE_PUNISH_TIPS;
        }
        messageService.baseSendUserViolationTips(tips,userInfo);
//        executor.schedule(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    messageMongoDBMapper.unregisterUserPush(uid);
//                } catch (Exception e) {
//                    logger.info("Ban User unregisterUserPush fail! uid:{}",uid,e);
//                }
//            }
//        },5,TimeUnit.SECONDS);



        String msgStr = messageMongoDBMapper.fm(violationMsgType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime));

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);
        punishHistory.setType(punishType.getType());
        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date(actionTime));

        punishHistoryMapper.create(punishHistory);

        if(extraPunish){
            if(punishType==PunishType.NICK){
                //3.1版本改昵称
                String nick="ME用户";
                UserInfo tmp=new UserInfo();
                tmp.setUid(uid);
                tmp.setNick(nick);
                tmp.setPinyinNick(PinyinHelper.convertToPinyinString(nick, "", PinyinFormat.WITHOUT_TONE));
                tmp.setShortPyNick(PinyinHelper.getShortPinyin(nick));
                userHessianService.getClient().updateByUidAndInvalidCache(tmp);
                messageService.sendPushAndInsertCtrlMsg(uid,tips,MsgDataType.S_NICK_PUNISH);//V3.1
            }else if(punishType==PunishType.SIGNATURE){
                //清空签名
                UserInfo tmp=new UserInfo();
                tmp.setUid(uid);
                tmp.setSignature("");
                userHessianService.getClient().updateByUidAndInvalidCache(tmp);
                messageService.sendPushAndInsertCtrlMsg(uid,tips,MsgDataType.S_SIGNATURE_PUNISH);//V3.1
            }
            try {
                userHessianService.getClient().insertHiidoUserUpdate(uid);
                hiddoSearch.addOrUpdateUserInfo(userInfo, 0, null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private String getRandomMachineNick(int sex) throws Exception {
        long uid;
        if(sex==1){
            //男用户
            uid=ThreadLocalRandom.current().nextLong(UserInfoUtil.minChannelBoyMachineManUid,UserInfoUtil.maxChannelBoyMachineManUid);
        }else {
            uid=ThreadLocalRandom.current().nextLong(UserInfoUtil.minChannelGirlMachineManUid,UserInfoUtil.maxChannelGirlMachineManUid);
        }
        UserInfo machineUserInfo=userHessianService.getClient().getUserByUid(uid,false);
        if(machineUserInfo==null){
            return "佚名";
        }
        return machineUserInfo.getNick();
    }

    public void lsAViolate(PunishSource punishSource, long uid, String lid, String reason, long actionTime, Integer osType, String deviceId, Boolean isLinkGuest, String rawRuleDesc) throws Exception {
        logger.info("lsAViolate punishSource:{},uid:{},lid:{},reason:{},actionTime:{},osType:{},deviceId:{},isLinkGuest:{},rawRuleDesc:{}",punishSource,uid,lid,reason,actionTime,osType,deviceId,isLinkGuest,rawRuleDesc);
        RuleDesc ruleDesc = parseRuleDesc(rawRuleDesc);
        // 封号惩罚
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);
        liveShowService.closeViolatedLiveShowByOwner(uid);
        liveShowService.guestLeaveAllLsThrift(uid, null);;


        sendBannedMsg(uid, ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2, actionTime, -1, punishSource, reason);

        userBanMongoDBMapper.banUserLs(uid, ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2.getValue(), actionTime, -1, "",ruleDesc);
        ViolationType violationType = ViolationType.U_BANED_LS_ACCOUNT_2;
        ViolationMsgType violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2;
        Long banedEndTime = null;
        ViolationMsgType deviceViolationMsgType =  ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2;
        if (osType != null && deviceId != null) {
            // 计算此用户已经被处罚了第几次
            int violateCount = userBanMongoDBMapper.incDeviceLsBan(osType, deviceId);
            switch (violateCount) {
                case 1:
                    banedEndTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
                    violationType = ViolationType.U_BANED_LS_DEVICE_2;
                    deviceViolationMsgType = ViolationMsgType.U_BANED_TIPS_LS_DEVICE_3;
                    break;
                default:
                    if (violateCount >= 2) {
                        banedEndTime = -1L;
                        violationType = ViolationType.U_BANED_LS_DEVICE_2;
                        deviceViolationMsgType = ViolationMsgType.U_BANED_TIPS_LS_DEVICE_4;
                    }
                    break;
            }

            logger.info(
                    "Ban User A Class(With DeviceId)! uid:{}, osType:{}, deviceId:{}, deviceViolationMsgType:{}, violateCount:{}, actionTime:{}, banedEndTime:{}, userInfo:{}",
                    uid, osType, deviceId, deviceViolationMsgType.getValue(), violateCount, actionTime, banedEndTime, userInfo);
            userBanMongoDBMapper.banDevice(uid, lid, osType, deviceId, deviceViolationMsgType.getValue(), actionTime, banedEndTime,ruleDesc);
        } else {
            logger.info("Ban User A Class(No DeviceId)! uid:{}, deviceViolationMsgType:{}, actionTime:{}, banedEndTime:{}, userInfo:{}", uid,
                    deviceViolationMsgType, actionTime, banedEndTime, userInfo);
        }
        long paramBanedEndTime = -1L;
        // 账号永久封禁
        userBanMongoDBMapper.banUser(uid, violationMsgType.getValue(), actionTime,paramBanedEndTime,ruleDesc);
        UserInfo setUserInfo  = new UserInfo();
        setUserInfo.setUid(uid);
        setUserInfo.setBaned(true);
        setUserInfo.setBanedType(violationMsgType.getValue());
        setUserInfo.setBanedActionTime(new Date(actionTime));
        setUserInfo.setBanedEndTime(new Date(paramBanedEndTime));
        if (ruleDesc != null) {
            setUserInfo.setBanedItem(ruleDesc.getItem());
            setUserInfo.setBanedSubItem(ruleDesc.getSubItem());
            setUserInfo.setBanedDesc(ruleDesc.getDesc());
        }
        userHessianService.getClient().updateByUidAndInvalidCache(setUserInfo);

        messageService.sendUserQuitLsViolate(uid, deviceViolationMsgType.getValue(), actionTime, banedEndTime,ruleDesc);// 退出登录

        messageService.insertUserLsViolationMsgNew(violationType.getValue(), actionTime, banedEndTime, "", uid, true,ruleDesc);// 存用户可见信箱
        messageService.sendUserLsViolationTipsNew(deviceViolationMsgType.getValue(), actionTime, banedEndTime, "", userInfo, true,ruleDesc);// 发送弹窗处罚消息
        messageMongoDBMapper.unregisterUserPush(uid);

        String msgStr = messageMongoDBMapper.fm(violationMsgType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime), "");

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);
        punishHistory.setLid(lid);
        if (isLinkGuest != null) {
            if (isLinkGuest) {
                punishHistory.setType(PunishType.LIVESHOW_LINK_A.getType());
            } else {
                punishHistory.setType(PunishType.LIVESHOW_A.getType());
            }
        } else {
            punishHistory.setType(PunishType.LIVESHOW_A.getType());
        }
        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date(actionTime));

        punishHistoryMapper.create(punishHistory);
    }

    public void lsBViolate(PunishSource punishSource, long uid, String lid, String reason, long actionTime, Boolean isLinkGuest, String rawRuleDesc) throws Exception {
        logger.info("lsBViolate punishSource:{},uid:{},lid:{},reason:{},actionTime:{},isLinkGuest:{},rawRuleDesc:{}",punishSource,uid,lid,reason,actionTime,isLinkGuest,rawRuleDesc);
        RuleDesc ruleDesc = parseRuleDesc(rawRuleDesc);
        // 计算此用户已经被处罚了第几次
        int violateCount = userBanMongoDBMapper.incUserLsBanB(uid);
        ViolationType violationType = null;
        ViolationMsgType violationMsgType = null;
        String banedDays = "";
        long banedEndTime = 0L;
        switch (violateCount) {
            case 1:
                banedDays = "3小时";
                banedEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3);
                violationType = ViolationType.U_BANED_LS_STOP_2;
                violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_STOP_2;
                break;
            case 2:
                banedDays = "6小时";
                banedEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6);
                violationType = ViolationType.U_BANED_LS_STOP_2;
                violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_STOP_2;
                break;
            case 3:
                banedDays = "12小时";
                banedEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12);
                violationType = ViolationType.U_BANED_LS_STOP_2;
                violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_STOP_2;
                break;
            default:
                if (violateCount >= 4) {
                    banedDays = "24小时";
                    banedEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24);
                    violationType = ViolationType.U_BANED_LS_STOP_2;
                    violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_STOP_2;
                    break;
                }
                return;
        }
        ;
        // 封号惩罚
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);
        logger.info("Ban User B Class! uid:{}, violationMsgType:{}, violateCount:{}, actionTime:{}, banedEndTime:{}, userInfo:{}", uid,
                violationMsgType.getValue(), violateCount, actionTime, banedEndTime, userInfo);
        liveShowService.closeViolatedLiveShowByOwner(uid);
        liveShowService.cancelLinkThrift(uid);
        // 账号封禁直播权限
        userBanMongoDBMapper.banUserLs(uid, violationMsgType.getValue(), actionTime, banedEndTime, banedDays,ruleDesc);

        messageService.insertUserLsViolationMsgNew(violationType.getValue(), actionTime, banedEndTime, banedDays, uid, true, ruleDesc);// 存用户可见信箱
        messageService.sendUserLsViolationTipsNew(violationMsgType.getValue(), actionTime, banedEndTime, banedDays, userInfo, true,ruleDesc);// 发送弹窗处罚消息

        String msgStr = messageMongoDBMapper.fm(violationMsgType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime), banedDays);

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);
        punishHistory.setLid(lid);
        if (isLinkGuest != null) {
            if (isLinkGuest) {
                punishHistory.setType(PunishType.LIVESHOW_LINK_B.getType());
            } else {
                punishHistory.setType(PunishType.LIVESHOW_B.getType());
            }
        } else {
            punishHistory.setType(PunishType.LIVESHOW_B.getType());
        }
        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date(actionTime));

        punishHistoryMapper.create(punishHistory);
    }

    public void lsLinkUserViolate(PunishSource punishSource, long uid, String lid, String reason, long actionTime, Integer osType, String deviceId) throws Exception {
        RuleDesc ruleDesc =null;
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);
        liveShowService.closeViolatedLiveShowByOwner(uid);
        liveShowService.guestLeaveAllLsThrift(uid, null);;

        ViolationType violationType = ViolationType.U_BANED_LS_LINK_ACCOUNT;
        ViolationMsgType violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_LINK_ACCOUNT_1;
        Long banedEndTime = null;
        String banedDays = "";
        // 计算此用户已经被处罚了第几次
        int violateCount = userBanMongoDBMapper.incLinkUserBan(uid);
        switch (violateCount) {
            case 1:
                banedDays = "3天";
                banedEndTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3);
                violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_LINK_ACCOUNT_1;
                userBanMongoDBMapper.banUserLs(uid, violationMsgType.getValue(), actionTime, banedEndTime, banedDays,ruleDesc);
                break;
            default:
                if (violateCount >= 2) {
                    banedEndTime = -1L;
                    if (violateCount >= 3 && osType != null && deviceId != null) {
                        // 可以罚设备
                        violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_LINK_ACCOUNT_3;
                        logger.info(
                                "Ban Link User(With DeviceId)! uid:{}, osType:{}, deviceId:{}, violationMsgType:{}, violateCount:{}, actionTime:{}, banedEndTime:{}, userInfo:{}",
                                uid, osType, deviceId, violationMsgType.getValue(), violateCount, actionTime, banedEndTime, userInfo);
                        userBanMongoDBMapper.banDevice(uid, lid, osType, deviceId, violationMsgType.getValue(), actionTime, banedEndTime, ruleDesc);
                    } else {
                        // 只能罚账户
                        violationMsgType = ViolationMsgType.U_BANED_TIPS_LS_LINK_ACCOUNT_2;
                        logger.info("Ban Link User(No DeviceId)! uid:{}, violationMsgType:{}, actionTime:{}, banedEndTime:{}, userInfo:{}", uid,
                                violationMsgType.getValue(), actionTime, banedEndTime, userInfo);
                    }
                    // 账号永久封禁
                    long paramBanedEndTime = -1L;
                    userBanMongoDBMapper.banUser(uid, violationMsgType.getValue(), actionTime, paramBanedEndTime, ruleDesc);
                    UserInfo setUserInfo  = new UserInfo();
                    setUserInfo.setUid(uid);
                    setUserInfo.setBaned(true);
                    setUserInfo.setBanedType(violationMsgType.getValue());
                    setUserInfo.setBanedActionTime(new Date(actionTime));
                    setUserInfo.setBanedEndTime(new Date(paramBanedEndTime));
                    if (ruleDesc != null) {
                        setUserInfo.setBanedItem(ruleDesc.getItem());
                        setUserInfo.setBanedSubItem(ruleDesc.getSubItem());
                        setUserInfo.setBanedDesc(ruleDesc.getDesc());
                    }
                    sendBannedMsg(uid, violationMsgType, actionTime, -1, punishSource, reason);
                    messageService.sendUserQuitNew(uid, violationMsgType.getValue(), actionTime, banedEndTime);// 退出登录
                    messageMongoDBMapper.unregisterUserPush(uid);
                }
                break;
        }

        messageService.insertUserLsViolationMsg(violationType.getValue(), actionTime, banedEndTime, banedDays, uid, true);// 存用户可见信箱
        messageService.sendUserLsViolationTips(violationMsgType.getValue(), actionTime, banedEndTime, banedDays, userInfo, true);// 发送弹窗处罚消息

        String msgStr = messageMongoDBMapper.fm(violationMsgType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime), banedDays);

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);
        punishHistory.setLid(lid);
        punishHistory.setType(PunishType.LIVESHOW_A.getType());
        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date(actionTime));

        punishHistoryMapper.create(punishHistory);
    }

    /**
     * 老封禁逻辑
     * 违规次数加一，并且进行封号惩罚
     *
     * @param uid
     * @return 是否为封号惩罚（若为是，则已在方法内处理了惩罚的逻辑，并在内部停止所有该uid的直播和连麦信息）
     * @throws Exception
     */
    public boolean banUser(PunishSource punishSource, long uid, String lid, String reason, MsgDataType violateType) throws Exception {
        int violateCount;
        if(violateType==MsgDataType.S_OP_VIOLATION_SIGNATURE||violateType==MsgDataType.S_OP_VIOLATION_NICK){
            violateCount=3;
        }else {
            violateCount=userBanMongoDBMapper.incUserBan(uid);
        }
        boolean shouldBan = false;
        MsgDataType dataType = null;
        long banedEndTime = 0L;
        int banedDays = 0;
        switch (violateCount) {
            case 1:
                banedDays = 3;
                banedEndTime = System.currentTimeMillis() + banedDays * 24 * 3600 * 1000;
                dataType = MsgDataType.U_BANED_1;
                shouldBan = true;
                break;
            case 2:
                banedDays = 10;
                banedEndTime = System.currentTimeMillis() + banedDays * 24 * 3600 * 1000;
                dataType = MsgDataType.U_BANED_2;
                shouldBan = true;
                break;
            default:
                if (violateCount >= 3) {
                    banedDays = -1;
                    banedEndTime = -1;
                    dataType = MsgDataType.U_BANED_FOREVER;
                    shouldBan = true;
                }
                break;
        }

        long actionTime = System.currentTimeMillis();
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,false);

        boolean result = false;
        if (shouldBan) {// 封号惩罚
            liveShowService.closeViolatedLiveShowByOwner(uid);
            liveShowService.guestLeaveAllLsThrift(uid, null);
            messageService.sendUserQuit(uid, dataType, actionTime, banedEndTime);
            messageService.sendSysOpViolation(violateType, userInfo, actionTime, banedDays, true);// 发送处罚消息
            userBanMongoDBMapper.banUser(uid, dataType.getValue(), actionTime, banedEndTime, null);
            UserInfo setUserInfo  = new UserInfo();
            setUserInfo.setUid(uid);
            setUserInfo.setBaned(true);
            setUserInfo.setBanedType(dataType.getValue());
            setUserInfo.setBanedActionTime(new Date(actionTime));
            setUserInfo.setBanedEndTime(new Date(banedEndTime));
            sendBannedMsg(uid, dataType, actionTime, banedEndTime, punishSource, reason);
            messageMongoDBMapper.unregisterUserPush(uid);
            result = false;
        } else {
            messageService.sendSysOpViolation(violateType, userInfo, actionTime, banedDays, false);// 仅发送处罚消息
            result = true;// 不是封号惩罚
        }

        String msgStr = messageMongoDBMapper.fm(violateType.getValue(), dateFormatter.format(new Date()), banedDays);

        PunishHistory punishHistory = new PunishHistory();
        punishHistory.setSource(punishSource.getSource());
        punishHistory.setUid(uid);

        if (violateType == MsgDataType.S_OP_VIOLATION_HEAD) {
            punishHistory.setType(PunishType.HEAD.getType());
        } else if (violateType == MsgDataType.S_OP_VIOLATION_LS) {
            punishHistory.setLid(lid);
            punishHistory.setType(PunishType.LIVESHOW.getType());
        } else if (violateType == MsgDataType.S_OP_VIOLATION_NICK) {
            punishHistory.setType(PunishType.NICK.getType());
        } else if (violateType == MsgDataType.S_OP_VIOLATION_SIGNATURE) {
            punishHistory.setType(PunishType.SIGNATURE.getType());
        }

        punishHistory.setReason(reason);
        punishHistory.setMessage(msgStr);
        punishHistory.setPunishDate(new Date());

        punishHistoryMapper.create(punishHistory);

        return result;
    }

    /**
     * 警告，一级封禁，二级封禁
     *  @param result
     * @param fromUid
     * @param toUid
     * @param deviceId
     * @param lid
     * @param snapshotUrl
     * @param reason
     * @param gongPing
     */
    public void handleReport(ReportResult result, long fromUid, long toUid, String deviceId, String lid, String snapshotUrl, String reason, String gongPing) throws Exception {
        UserInfo userInfo = userHessianService.getClient().getUserByUid(toUid, false);
        logger.info("handleReport,result:{},fromUid:{},toUid:{},deviceId:{},lid:{},reason:{}",result,fromUid,toUid,deviceId,lid,reason);
        if (result == ReportResult.WARN) {//警告
            String bandType = ViolationMsgType.U_BANED_REPORT_WARN.getValue();
            String tips = reportCountMongoDBMapper.findTips(result, toUid);
            String content = messageMongoDBMapper.fm(bandType, tips);
            messageService.insertReportPunish(content, toUid);
            UserInfo setUserInfo = new UserInfo();
            setUserInfo.setUid(toUid);
            setUserInfo.setBanedType(bandType);
            userHessianService.getClient().updateByUidAndInvalidCache(setUserInfo);
            //发警告push。写入ctrl消息；用户若在app内收到，则可以调用deleteCtrlMsg删掉消息。
            messageService.sendPushAndInsertCtrlMsg(toUid,content,MsgDataType.S_REPORT_WARN);
        } else if (result == ReportResult.FIRST_FORBID) {//一级封禁
            String bandType = ViolationMsgType.U_BANED_REPORT_FIRST_FORBID.getValue();
            String banDesc = reportCountMongoDBMapper.findTips(result, toUid);
            String content = messageMongoDBMapper.fm(bandType, banDesc);
            // 封禁惩罚
            doBandUser(toUid, bandType,banDesc);
            messageService.sendReportBanReward(fromUid,FormatMessage.REPORT_BAN_REWARD);
            messageService.sendReportPunish("", MsgDataType.S_REPORT_BAN, userInfo,bandType);
        } else if (result == ReportResult.SECOND_FORBID) {//二级封禁
            String bandType = ViolationMsgType.U_BANED_REPORT_SECOND_FORBID.getValue();
            String banDesc = reportCountMongoDBMapper.findTips(result, toUid);
            String content = messageMongoDBMapper.fm(bandType, banDesc);
            // 封禁惩罚
            doBandUser(toUid, bandType,banDesc);
            messageService.sendReportBanReward(fromUid,FormatMessage.REPORT_BAN_REWARD);
            messageService.sendReportPunish("", MsgDataType.S_REPORT_BAN, userInfo, bandType);
            UserBanMongoDBMapper.LoginDevice loginDevice = userBanMongoDBMapper.getUserLoginDevice(toUid);
            //封禁最近登录设备
            if (loginDevice != null) {
                userBanMongoDBMapper.banDevice(toUid, lid, loginDevice.getOsType(), loginDevice.getDeviceId(), ViolationMsgType.U_BANED_TIPS_LS_DEVICE_5.getValue(), System.currentTimeMillis(), -1, new RuleDesc());
            } else {
                logger.info("deviceId is null,fromUid:{},toUid:{},lid:{},deviceId:{}",fromUid,toUid,lid,deviceId);
            }
        }
    }

    /**
     * 对观众的举报处理，设定不违规，或者不违规
     * @param objectId
     */
    private void processReport(String objectId, int status) throws Exception {
        reportCountMongoDBMapper.doneProcess(objectId);
        if (status == ReportStatus.ILLEGAL.getStatus()) {//默认就是违规的，不用继续处理
            return;
        }
        DBObject history = reportCountMongoDBMapper.unViolateReportHistory(objectId);
        Long toUid =(Long) history.get(ReportCountMongoDBMapper.FIELD_TO_UID);
        logger.info("reportUnViolate objectId:{},toUid:{}", objectId, toUid);
        UserInfo toUser = userHessianService.getClient().getUserByUid(toUid, false);
        reportCountMongoDBMapper.descReportCount(toUid,(Double)history.get(ReportCountMongoDBMapper.FIELD_INCR_COUNT));
        if (toUser.getBaned() != null && toUser.getBaned() &&
                ViolationMsgType.U_BANED_REPORT_FIRST_FORBID.getValue().equals(toUser.getBanedType())) {//一级封禁状态
            List<Long> cancelUids = new ArrayList<>();
            cancelUids.add(toUser.getUid());
            userHessianService.getClient().cancelBanUser(cancelUids);
            messageService.sendReportCancelBan(toUid, FormatMessage.REPORT_CANCEL_BAN);
            logger.info("reportUnViolate cancel first forbid,objectId:{},toUid:{}", objectId, toUid);
        }
        if (toUser.getBaned() != null && toUser.getBaned() &&
                ViolationMsgType.U_BANED_REPORT_SECOND_FORBID.getValue().equals(toUser.getBanedType())) {//二级封禁状态
            List<Long> cancelUids = new ArrayList<>();
            cancelUids.add(toUser.getUid());
            userHessianService.getClient().cancelBanUser(cancelUids);
            messageService.sendReportCancelBan(toUid, FormatMessage.REPORT_CANCEL_BAN);
            logger.info("reportUnViolate cancel second forbid,objectId:{},toUid:{}", objectId, toUid);
        }
    }


    /**
     * 封禁惩罚
     *
     * @param uid
     * @param bandType
     * @param banDesc
     * @throws Exception
     */
    private void doBandUser(long uid, String bandType, String banDesc) throws Exception {
        liveShowService.closeViolatedLiveShowByOwner(uid);
        liveShowService.guestLeaveAllLsThrift(uid, null);
        long paramBanedEndTime = -1L;
        UserInfo setUserInfo = new UserInfo();
        setUserInfo.setUid(uid);
        setUserInfo.setBaned(true);
        setUserInfo.setBanedType(bandType);
        setUserInfo.setBanedDesc(banDesc);
        setUserInfo.setBanedActionTime(new Date());
        setUserInfo.setBanedEndTime(null);
        userHessianService.getClient().updateByUidAndInvalidCache(setUserInfo);
    }

}
