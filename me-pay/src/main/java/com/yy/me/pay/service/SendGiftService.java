package com.yy.me.pay.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.user.UserHessianService;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.pay.entity.GiftBagRecord;
import com.yy.me.message.Message;
import com.yy.me.message.MsgDataType;
import com.yy.me.message.thrift.push.MessageType;
import com.yy.me.user.UserInfo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class SendGiftService {
    private static final Logger logger = LoggerFactory.getLogger(SendGiftService.class);
    
    public static final String RET_LS_GIFT_BATCH_LIST = "giftList";
    public static final String RET_LS_GIFT_SEQ = "seq";
    public static final String RET_LS_GIFT_LID = "lid";
    public static final String RET_LS_GIFT_RECV_UID = "recvUid";
    public static final String RET_LS_GIFT_RECV_NICK = "recvNick";
    public static final String RET_LS_GIFT_USED_TIME = "usedTime";
    public static final String RET_LS_GIFT_PROP_ID = "propId";
    public static final String RET_LS_GIFT_PROP_COUNT = "propCount";
    public static final String RET_LS_GIFT_INCOME = "income";
    public static final String RET_LS_GIFT_AMOUNT = "amount";
    public static final String RET_LS_GIFT_EXPAND = "expand";
    public static final String RET_LS_GIFT_UID = "uid";
    public static final String RET_LS_GIFT_NICK = "nick";
    public static final String RET_LS_GIFT_HEADER_URL = "headerUrl";
    public static final String RET_LS_GIFT_VERIFIED = "verified";
    public static final String RET_LS_GIFT_MEDAL = "medal";

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private MessageMongoDBMapper messageMapper;

    @Value(value = "#{settings['node.productEnv']}")
    private boolean productEnv;

    public void sendGiftBroadcast(GiftBagRecord giftBag) throws Exception {
        List<UserInfo> userInfoList = userHessianService.getClient().findUserListByUids(Lists.newArrayList(giftBag.getUid(), giftBag.getRecvUid()),true);
        if (userInfoList == null || userInfoList.isEmpty() || userInfoList.size() != 2) {
            logger.error("Gift sender uid or receiver uid is not exists. giftBag: {}", giftBag);
            return;
        }
        
        Map<Long, UserInfo> userInfoMap = Maps.uniqueIndex(userInfoList, new Function<UserInfo, Long>() {
            @Override
            public Long apply(UserInfo userInfo) {
                return userInfo.getUid();
            }
        });

        UserInfo userInfo = userInfoMap.get(giftBag.getUid());
        if (userInfo == null) {
            logger.error("Gift sender userInfo is empty. giftBag: {}", giftBag);
            return;
        }
        
        UserInfo recvUser = userInfoMap.get(giftBag.getRecvUid());
        if (recvUser == null) {
            logger.error("Gift receiver userInfo is empty. giftBag: {}", giftBag);
            return;
        }

        int propCount = giftBag.getPropCount();
        int propIncome = giftBag.getIncome();
        if (propCount > 1 && propIncome > 0) {
            propIncome = giftBag.getIncome() / propCount;
        }

        sendJsonMessage(giftBag, userInfo, recvUser, propIncome);
    }

    private void sendJsonMessage(GiftBagRecord giftBag, UserInfo sendUser, UserInfo recvUser, int propIncome) throws Exception {
        Message msg = new Message();

        Map<String, Object> msgData = Maps.newLinkedHashMap();
        msgData.put(MessageMongoDBMapper.FIELD_MSG_DATA_TYPE, MsgDataType.U_LIVE_GIFT.getValue());

        msgData.put(RET_LS_GIFT_UID, sendUser.getUid());
        msgData.put(RET_LS_GIFT_NICK, sendUser.getNick());
        msgData.put(RET_LS_GIFT_HEADER_URL, sendUser.getHeaderUrl());
        msgData.put(RET_LS_GIFT_VERIFIED, sendUser.getVerified());
        msgData.put(RET_LS_GIFT_MEDAL, sendUser.getTopMedal());
        msgData.put(RET_LS_GIFT_SEQ, giftBag.getSeq());
        msgData.put(RET_LS_GIFT_LID, giftBag.getLid());
        msgData.put(RET_LS_GIFT_RECV_UID, giftBag.getRecvUid());

        msgData.put(RET_LS_GIFT_RECV_NICK, recvUser.getNick());
        msgData.put(RET_LS_GIFT_USED_TIME, giftBag.getUsedTime().getTime());
        msgData.put(RET_LS_GIFT_PROP_ID, giftBag.getPropId());
        msgData.put(RET_LS_GIFT_PROP_COUNT, giftBag.getPropCount());
        msgData.put(RET_LS_GIFT_AMOUNT, giftBag.getAmount());
        msgData.put(RET_LS_GIFT_INCOME, propIncome);
        msgData.put(RET_LS_GIFT_EXPAND, giftBag.getExpand());
        msg.setMsgData(msgData);
        msg.setExpiry(TimeUnit.MINUTES.toMillis(3L));

        // 发送广播
        messageMapper.pushTopic(MsgDataType.U_LIVE_DATA.getValue() + ":" + giftBag.getLid(), MessageType.PASS_THROUGH,
                msg);
    }

    public void batchSendGiftBroadcast(String lid, List<GiftBagRecord> giftBagList) throws Exception {
        List<Long> sendUidList = Lists.transform(giftBagList, new Function<GiftBagRecord, Long>() {
            @Override
            public Long apply(GiftBagRecord giftBag) {
                return giftBag.getUid();
            }
        });
        
        List<Long> recvUidList = Lists.transform(giftBagList, new Function<GiftBagRecord, Long>() {
            @Override
            public Long apply(GiftBagRecord giftBag) {
                return giftBag.getRecvUid();
            }
        });
        
        Set<Long> uidSet = Sets.newHashSetWithExpectedSize(sendUidList.size() + recvUidList.size());
        uidSet.addAll(sendUidList);
        uidSet.addAll(recvUidList);
        
        List<UserInfo> userInfoList = userHessianService.getClient().findUserListByUids(Lists.newArrayList(uidSet),true);
        if (userInfoList == null || userInfoList.isEmpty()) {
            logger.error("Gift sender uids is not exists. giftBags: {}", StringUtils.join(giftBagList, ", "));
            return;
        }

        Map<Long, UserInfo> userInfoMap = Maps.uniqueIndex(userInfoList, new Function<UserInfo, Long>() {
            @Override
            public Long apply(UserInfo userInfo) {
                return userInfo.getUid();
            }
        });

        sendBatchJsonMessage(lid, giftBagList, userInfoMap);
    }

    private void sendBatchJsonMessage(String lid, List<GiftBagRecord> giftBagList, Map<Long, UserInfo> userInfoMap)
            throws Exception {
        Message msg = new Message();

        Map<String, Object> msgData = Maps.newLinkedHashMap();
        msgData.put(MessageMongoDBMapper.FIELD_MSG_DATA_TYPE, MsgDataType.U_LIVE_GIFT_BATCH.getValue());
        msgData.put(RET_LS_GIFT_LID, lid);

        List<Map<String, Object>> giftDataList = Lists.newArrayList();
        for (GiftBagRecord giftBag : giftBagList) {
            Map<String, Object> giftData = Maps.newHashMap();

            UserInfo userInfo = userInfoMap.get(giftBag.getUid());
            if (userInfo == null) {
                logger.error("Gift sender userInfo is empty. giftBag: {}", giftBag);
                continue;
            }
            
            UserInfo recvUser = userInfoMap.get(giftBag.getRecvUid());
            if (recvUser == null) {
                logger.error("Gift receiver userInfo is empty. giftBag: {}", giftBag);
                continue;
            }

            int propCount = giftBag.getPropCount();
            int propIncome = giftBag.getIncome();
            if (propCount > 1 && propIncome > 0) {
                propIncome = giftBag.getIncome() / propCount;
            }

            giftData.put(RET_LS_GIFT_UID, userInfo.getUid());
            giftData.put(RET_LS_GIFT_NICK, userInfo.getNick());
            giftData.put(RET_LS_GIFT_HEADER_URL, userInfo.getHeaderUrl());
            giftData.put(RET_LS_GIFT_VERIFIED, userInfo.getVerified());
            giftData.put(RET_LS_GIFT_MEDAL, userInfo.getTopMedal());
            giftData.put(RET_LS_GIFT_SEQ, giftBag.getSeq());
            giftData.put(RET_LS_GIFT_RECV_UID, giftBag.getRecvUid());
            giftData.put(RET_LS_GIFT_RECV_NICK, recvUser.getNick());
            giftData.put(RET_LS_GIFT_USED_TIME, giftBag.getUsedTime().getTime());
            giftData.put(RET_LS_GIFT_PROP_ID, giftBag.getPropId());
            giftData.put(RET_LS_GIFT_PROP_COUNT, giftBag.getPropCount());
            giftData.put(RET_LS_GIFT_AMOUNT, giftBag.getAmount());
            giftData.put(RET_LS_GIFT_INCOME, propIncome);
            giftData.put(RET_LS_GIFT_EXPAND, giftBag.getExpand());

            giftDataList.add(giftData);
        }
        msgData.put(RET_LS_GIFT_BATCH_LIST, giftDataList);

        msg.setMsgData(msgData);
        msg.setExpiry(TimeUnit.MINUTES.toMillis(3L));

        // 发送广播
        messageMapper.pushTopic(MsgDataType.U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, msg);
    }

}
