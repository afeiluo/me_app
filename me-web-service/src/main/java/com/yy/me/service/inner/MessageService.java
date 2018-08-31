package com.yy.me.service.inner;

import static com.yy.me.http.BaseServletUtil.getLocalObjMapper;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_BANED;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_DATA_CLIENT_ID;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_DATA_TIME;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_DATA_TYPE;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_ID;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_MSG_DATA;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_OP_BC_URL;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_TEMPLATE_INDEX_URL;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_TEMPLATE_NEAR_URL;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_TO_BL;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_TO_HIDE;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_TO_IMG;
import static com.yy.me.message.MessageMongoDBMapper.FIELD_MSG_WITHDRAW;
import static com.yy.me.message.MsgDataType.*;
import static com.yy.me.service.inner.ServiceConst.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.yy.me.enums.ViolationMsgType;
import com.yy.me.user.entity.Medal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.config.GeneralConfService;
import com.yy.me.entity.RuleDesc;
import com.yy.me.http.LocaleUtil;
import com.yy.me.json.JsonUtil;
import com.yy.me.message.BroadcastBuilder;
import com.yy.me.message.Message;
import com.yy.me.message.Message.TitleAndMsgBuilder;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.message.MsgDataType;
import com.yy.me.message.thrift.push.MessageType;
import com.yy.me.message.thrift.push.PushMessage;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.service.UserListService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.util.VersionUtil;

/**
 * 建立各种格式的消息体，并存储到用户个人信箱，最后利用Push机制发送通知
 * 
 * @author JCY
 */
@Service
public class MessageService {

    private static Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private CntConfService cntConfService;
    @Autowired
    private GeneralConfService generalConfService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;
    @Autowired
    private UserListService userListService;

    public final static FastDateFormat dateFormatter = FastDateFormat.getInstance("yyyy年MM月dd日  HH:mm:ss");

    // 1表示运营后台设置，2表示主播自己设置
    public static final int BILLBOARD_TYPE_OPERATION_VALUE = 1;
    public static final int BILLBOARD_TYPE_ANCHOR_VALUE = 2;

    /**
     * 倒计时插件
     */
    private static final String MSG_F_COUNTDOWN_STARTTIME = "startTime";
    private static final String MSG_F_COUNTDOWN_DURATION = "duration";
    private static final String MSG_F_COUNTDOWN_TYPE = "type";
    private static final String MSG_F_COUNTDOWN_LID = "lid";

    /**
     * 直播间更新距离
     */
    private static final String MSG_U_UPDATE_DISTANCE_LID = "lid";
    private static final String MSG_U_UPDATE_DISTANCE_DISTANCE = "distance";

    /**
     * 消息气泡
     */
    private static final String MSG_U_BUBBLE_UID = "uid";
    private static final String MSG_U_BUBBLE_NICK = "nick";
    private static final String MSG_U_BUBBLE_HEADURL = "headUrl";
    private static final String MSG_U_BUBBLE_TYPE = "type";
    private static final String MSG_U_BUBBLE_MC = "mc";

    /**
     * 生成偏序ID
     * 
     * @return
     * @throws Exception
     */
    public long genPartialOrder() {
        return System.currentTimeMillis();
    }

    public List<ObjectNode> checkoutMyRecentlyCtrlMsg(String appId, UserInfo userInfo, String pushId, String clientStr, String clientVer)
            throws Exception {

        // update by luowenhui 鹿晗模式时，缓存和用户无关，userInfo为null
        List<ObjectNode> ret = messageMongoDBMapper.checkoutMyRecentlyCtrlMsg(userInfo, pushId);
        if (ret == null) {
            ret = Lists.newArrayList();
        }
        ObjectMapper mapper = JsonUtil.createDefaultMapper();

        ObjectNode turnoverImgInfo = new CtrlMsgBuilder(S_TO_IMG).addProperty(FIELD_MSG_TO_IMG, cntConfService.getTurnoverImgJson()).build();
        ret.add(turnoverImgInfo);

        // 模板配置
        ObjectNode templateConfig = new CtrlMsgBuilder(S_TEMPLATE_CONFIG)
                .addProperty(FIELD_MSG_TEMPLATE_INDEX_URL, cntConfService.getTemplateIndexUrl())
                .addProperty(FIELD_MSG_TEMPLATE_NEAR_URL, cntConfService.getTemplateNearUrl()).build();
        ret.add(templateConfig);

        List<Long> toBls = userListService.getUserList(UserListService.TO_BL);
        if (toBls != null && !toBls.isEmpty()) {
            ObjectNode blMsg = new CtrlMsgBuilder(S_TO_BL).addProperty(FIELD_MSG_TO_BL, JsonUtil.createDefaultMapper().valueToTree(toBls)).build();
            ret.add(blMsg);
        }
        List<Long> toHides = userListService.getUserList(UserListService.TO_HIDE);
        if (toHides != null && !toHides.isEmpty()) {
            ObjectNode tmp = new CtrlMsgBuilder(S_TO_HIDE).addProperty(FIELD_MSG_TO_HIDE, JsonUtil.createDefaultMapper().valueToTree(toHides))
                    .build();
            ret.add(tmp);
        }
        // 提现开关
        if (StringUtils.isNotBlank(clientStr) && StringUtils.isNotBlank(clientVer) && "iOS".equals(clientStr)) {// 只有iOS
            ObjectNode jo = mapper.createObjectNode();
            jo.put(FIELD_MSG_DATA_TYPE, S_ENTRANCE_STATUS.getValue());
            jo.put(FIELD_MSG_ID, new ObjectId().toHexString());
            jo.put(FIELD_MSG_DATA_CLIENT_ID, new ObjectId().toHexString());
            jo.put(FIELD_MSG_DATA_TIME, System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000);// 1年前
            // 黑名单模式，true打开，false隐藏
            if (appId != null) {
                List<String> switchVers = generalConfService.findWithdrawSwitch4Ios(appId);

                boolean hit = false;
                if (switchVers != null && !switchVers.isEmpty()) {
                    for (String ver : switchVers) {
                        if (clientVer.equals(ver)) {
                            jo.put(FIELD_MSG_WITHDRAW, false);// 击中关闭入口黑名单
                            hit = true;
                            continue;
                        }
                    }
                }
                String minVer = "1.3.0.35";
                if (!productEnv) {
                    minVer = "1.3.0.1237";
                }
                if (VersionUtil.compareVersion(clientVer, minVer) < 0) {
                    jo.put(FIELD_MSG_WITHDRAW, false);// 比20160415最新版要小
                    hit = true;
                }
                if (!hit) {
                    jo.put(FIELD_MSG_WITHDRAW, true);// 击不中，则放开
                }
                logger.info("appId:{}, client:{}, ver:{}, minVer:{}, blackListVers:{}, widthdraw:{}", appId, clientStr, clientVer, minVer,
                        MongoUtil.genLogObj(switchVers), jo.get(FIELD_MSG_WITHDRAW).asBoolean());
                ret.add(jo);
            } else {
                jo.put(FIELD_MSG_WITHDRAW, true);// 没有appid，放开
                logger.info("empty appId! client:{}, ver:{}, widthdraw:{}", appId, clientStr, clientVer, jo.get(FIELD_MSG_WITHDRAW).asBoolean());
                ret.add(jo);
            }
        }
        return ret;
    }

    private class CtrlMsgBuilder {
        private MsgDataType dataType;
        private String msgId;
        private String msgClientId;
        private long msgTime;
        private ObjectNode jsonObject;

        public CtrlMsgBuilder(MsgDataType dataType) {
            this.dataType = dataType;
            this.msgId = new ObjectId().toHexString();
            this.msgClientId = new ObjectId().toHexString();
            this.msgTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365);
            this.jsonObject = JsonUtil.createDefaultMapper().createObjectNode();
        }

        public CtrlMsgBuilder msgId(String msgId) {
            this.msgId = msgId;
            return this;
        }

        public CtrlMsgBuilder msgClientId(String msgClientId) {
            this.msgClientId = msgClientId;
            return this;
        }

        public CtrlMsgBuilder msgTime(long msgTime) {
            this.msgTime = msgTime;
            return this;
        }

        public CtrlMsgBuilder addProperty(String key, Boolean value) {
            jsonObject.put(key, value);
            return this;
        }

        public CtrlMsgBuilder addProperty(String key, Number value) {
            jsonObject.put(key, value.toString());
            return this;
        }

        public CtrlMsgBuilder addProperty(String key, String value) {
            jsonObject.put(key, value);
            return this;
        }

        public CtrlMsgBuilder addProperty(String key, JsonNode value) {
            jsonObject.put(key, value);
            return this;
        }

        public ObjectNode build() {
            jsonObject.put(FIELD_MSG_DATA_TYPE, dataType.getValue());
            jsonObject.put(FIELD_MSG_ID, msgId);
            jsonObject.put(FIELD_MSG_DATA_CLIENT_ID, msgClientId);
            jsonObject.put(FIELD_MSG_DATA_TIME, msgTime);
            return jsonObject;
        }
    }

    public List<ObjectNode> checkoutMyRecentlyUserMsg(UserInfo userInfo, String pushId, String lastMsgId, int limit) throws Exception {
        List<ObjectNode> ret = messageMongoDBMapper.checkoutMyRecentlyUserMsg(userInfo, pushId, lastMsgId, limit);
        return ret;
    }

    public void removeCtrlMsg(UserInfo userInfo, String msgDataType) throws Exception {
        messageMongoDBMapper.removeCtrlMsg(userInfo.getUid(), msgDataType);
    }

    /**
     * 后台配置的推送的直播通知到所有的用户
     * 
     * @param
     * @throws Exception
     */
    public void sendOperLiveShow2All(final UserInfo liveUserInfo, String lid, String desc, final String messageTitle) throws Exception {
        String broadcastTitle = StringUtils.isBlank(desc) ? liveUserInfo.getNick() : desc;
        // Message msg = messageMongoDBMapper.genMsg(Locale.SIMPLIFIED_CHINESE, null, title);
        // String msgStr = messageMongoDBMapper.fm(Locale.SIMPLIFIED_CHINESE, F_JLS_START.getValue(), "");
        Message msg = new Message();
        msg.setAppNme(messageTitle);
        msg.setMsgTitle(broadcastTitle);
        Map<String, Object> msgData = MessageMongoDBMapper.genIdolMsgData(F_JLS_START, liveUserInfo, null, null);
        if (lid != null) {
            msgData.put(FIELD_LS_LID, lid);
        }
        msgData.put(FIELD_MSG_MSG_DATA, broadcastTitle);
        msg.setMsgData(msgData);
        messageMongoDBMapper.broadcastAll(MessageType.ALL, msg);
    }

    public void sendOperLiveShow2Anonymous(final UserInfo liveUserInfo, String lid, String desc, final String messageTitle) throws Exception {
        String broadcastTitle = StringUtils.isBlank(desc) ? liveUserInfo.getNick() : desc;
        // Message msg = messageMongoDBMapper.genMsg(Locale.SIMPLIFIED_CHINESE, null, title);
        // String msgStr = messageMongoDBMapper.fm(Locale.SIMPLIFIED_CHINESE, F_JLS_START.getValue(), "");
        Message msg = new Message();
        msg.setAppNme(messageTitle);
        msg.setMsgTitle(broadcastTitle);
        Map<String, Object> msgData = MessageMongoDBMapper.genIdolMsgData(F_JLS_START, liveUserInfo, null, null);
        if (lid != null) {
            msgData.put(FIELD_LS_LID, lid);
        }
        msgData.put(FIELD_MSG_MSG_DATA, broadcastTitle);
        msg.setMsgData(msgData);
        messageMongoDBMapper.broadcastAnonymous(MessageType.ALL, msg);
    }

    /**
     * 后台配置的推送的直播通知到指定的用户
     * 
     * @param
     * @param broadcastTitle
     * @param users
     * @throws Exception
     */
    public void sendOperLiveShow2Uers(final UserInfo liveUserInfo, String lid, String broadcastTitle, final String messageTitle, List<Long> users)
            throws Exception {
        String title = StringUtils.isBlank(broadcastTitle) ? liveUserInfo.getNick() : broadcastTitle;
        // Message msg = messageMongoDBMapper.genMsg(Locale.SIMPLIFIED_CHINESE, null, title);
        Message msg = new Message();
        TitleAndMsgBuilder builder = new TitleAndMsgBuilder() {
            public void build(Message msg, Locale locale) throws Exception {
                msg.setAppNme(messageTitle);
            }
        };
        msg.resetBuilder(builder);
        msg.setMsgTitle(title);
        // String msgStr = messageMongoDBMapper.fm(Locale.SIMPLIFIED_CHINESE, F_JLS_START.getValue(), "");
        Map<String, Object> msgData = MessageMongoDBMapper.genIdolMsgData(F_JLS_START, liveUserInfo, null, null);
        if (lid != null) {
            msgData.put(FIELD_LS_LID, lid);
        }
        msgData.put(FIELD_MSG_MSG_DATA, title);
        msg.setMsgData(msgData);
        msg.setShouldPersist(true);// 存入信箱
        pushManyUids(MessageType.NOTIFICATION, msg, users);
    }

    public void sendSysOpViolation(MsgDataType violateType, UserInfo sendUser, long actionTime, int banedDays, boolean baned) throws Exception {
        if (!violateType.getValue().startsWith("s_op_violation_")) {
            return;
        }

        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String formatTime = dateFormatter.format(new Date(actionTime));// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateType.getValue(), formatTime, banedDays);
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        msg.setShouldPersist(true);// 存入信箱
        String msgStr = messageMongoDBMapper.fm(violateType.getValue(), formatTime, banedDays);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    /**
     * 因被罚（ban）踢某个用户下线
     * 
     * @param sendUid
     * @throws Exception
     */
    public void sendUserQuit(long sendUid, MsgDataType violateType, long actionTime, long banedEndTime) throws Exception {
        if (!violateType.getValue().startsWith("u_baned_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String title = messageMongoDBMapper.fm4Title(violateType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime));
        String msgStr = messageMongoDBMapper.fm(violateType.getValue(), dateFormatter.format(actionTime), dateFormatter.format(banedEndTime));
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_QUIT_MSG, null, msgStr);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void sendUserQuitLsViolate(long sendUid, String violateTypeStr, long actionTime, Long banedEndTime, RuleDesc ruleDesc) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTime = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTime, endTime, null, ruleDesc.getItem(), ruleDesc.getSubItem(),
                ruleDesc.getDesc());
        String msgStr = messageMongoDBMapper
                .fm(violateTypeStr, actTime, endTime, null, ruleDesc.getItem(), ruleDesc.getSubItem(), ruleDesc.getDesc());
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_QUIT_MSG, null, msgStr);
        msg.setMsgData(msgData);
        msgData.put(FIELD_RULE_DESC_ITEM, ruleDesc.getItem());
        msgData.put(FIELD_RULE_DESC_SUB_ITEM, ruleDesc.getSubItem());
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void sendUserQuitNew(long sendUid, String violateTypeStr, long actionTime, Long banedEndTime) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTime = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTime, endTime);
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTime, endTime);
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_QUIT_MSG, null, msgStr);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void baseSendUserViolationTips(String msgStr, UserInfo sendUser) throws Exception {
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, msgStr);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void sendReportPunish(String msgStr, MsgDataType msgDataType, UserInfo sendUser, String bandType) throws Exception {
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, msgStr);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(msgDataType, null, msgStr);
        msgData.put("secondForbid", ViolationMsgType.U_BANED_REPORT_SECOND_FORBID.getValue().equals(bandType));
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void sendUserViolationTips(String violateTypeStr, long actionTime, long banedEndTime, UserInfo sendUser, boolean baned) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_tips")) {
            return;
        }

        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTimeOrDays = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTimeOrDays, endTime);
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTimeOrDays, endTime);

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void baseInsertUserViolationMsg(String msgStr, long sendUid) throws Exception {
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, msgStr);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertUserMsg(msg);
    }

    public void insertReportPunish(String msgStr, long sendUid) throws Exception {
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, msgStr);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_REPORT_WARN, null, msgStr);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertUserMsg(msg);
    }

    public void insertUserViolationMsg(String violateTypeStr, long actionTime, long sendUid, boolean baned) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTimeOrDays = dateFormatter.format(actionTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTimeOrDays);
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTimeOrDays);

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertUserMsg(msg);
    }

    public void sendUserLsViolationTips(String violateTypeStr, long actionTime, Long banedEndTime, String banedDays, UserInfo sendUser, boolean baned)
            throws Exception {
        if (!violateTypeStr.startsWith("u_baned_tips_ls_")) {
            return;
        }

        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTime = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTime, endTime, banedDays);
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTime, endTime, banedDays);

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void sendUserLsViolationTipsNew(String violateTypeStr, long actionTime, Long banedEndTime, String banedDays, UserInfo sendUser,
            boolean baned, RuleDesc ruleDesc) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_tips_ls_")) {
            return;
        }

        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTime = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTime, endTime, banedDays, ruleDesc.getItem(), ruleDesc.getSubItem(),
                ruleDesc.getDesc());
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTime, endTime, banedDays, ruleDesc.getItem(), ruleDesc.getSubItem(),
                ruleDesc.getDesc());

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msgData.put(FIELD_RULE_DESC_ITEM, ruleDesc.getItem());
        msgData.put(FIELD_RULE_DESC_SUB_ITEM, ruleDesc.getSubItem());
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertAndPushUserMsg(MessageType.PASS_THROUGH, msg);
    }

    public void insertUserLsViolationMsg(String violateTypeStr, long actionTime, Long banedEndTime, String banedDays, long sendUid, boolean baned)
            throws Exception {
        if (!violateTypeStr.startsWith("u_baned_ls_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTimeOrDays = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTimeOrDays, endTime, banedDays);
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTimeOrDays, endTime, banedDays);

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertUserMsg(msg);
    }

    public void insertUserLsViolationMsgNew(String violateTypeStr, long actionTime, Long banedEndTime, String banedDays, long sendUid, boolean baned,
            RuleDesc ruleDesc) throws Exception {
        if (!violateTypeStr.startsWith("u_baned_ls_")) {
            return;
        }
        UserInfo sendUser = userHessianService.getClient().getUserByUid(sendUid, false);
        Locale locale = LocaleUtil.genLocaleFromStr(sendUser.getMyLocale());
        String actTimeOrDays = dateFormatter.format(actionTime);// TODO 格式化时间
        String endTime = banedEndTime == null ? "" : dateFormatter.format(banedEndTime);// TODO 格式化时间
        String title = messageMongoDBMapper.fm4Title(violateTypeStr, actTimeOrDays, endTime, banedDays, ruleDesc.getItem(), ruleDesc.getSubItem(),
                ruleDesc.getDesc());
        String msgStr = messageMongoDBMapper.fm(violateTypeStr, actTimeOrDays, endTime, banedDays, ruleDesc.getItem(), ruleDesc.getSubItem(),
                ruleDesc.getDesc());

        Message msg = messageMongoDBMapper.genMsg(locale, sendUser, title);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_VIOLATION, null, msgStr);
        msgData.put(FIELD_MSG_BANED, baned);
        msg.setMsgData(msgData);
        messageMongoDBMapper.insertUserMsg(msg);
    }

    /**
     * 推送系统信息给所有用户.
     * 
     * @param desc
     * @param contentUrl
     * @throws Exception
     */
    public void broadcastSysMessage(final String title, String desc, String contentUrl) throws Exception {
        Message msg = new Message();
        msg.setAppNme(title);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_BC, null, desc);
        msg.setMsgData(msgData);
        msgData.put(FIELD_MSG_OP_BC_URL, contentUrl);
        msg.setStartTime(new Date().getTime());
        messageMongoDBMapper.insertGlobalUserMsg(MessageType.NOTIFICATION, msg);
    }

    public void broadcastSysMessage2Anony(final String title, String desc, String contentUrl) throws Exception {
        Message msg = new Message();
        msg.setAppNme(title);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_BC, null, desc);
        msg.setMsgData(msgData);
        msgData.put(FIELD_MSG_OP_BC_URL, contentUrl);
        msg.setStartTime(new Date().getTime());
        messageMongoDBMapper.insertAnonyUserMsg(MessageType.NOTIFICATION, msg);
    }

    /**
     * 推送系统无跳转信息给所有用户.
     * 
     * @param desc
     * @throws Exception
     */
    public void broadcastSysOpMessage(final String title, String desc) throws Exception {
        Message msg = new Message();
        msg.setAppNme(title);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_MSG, null, desc);
        msg.setMsgData(msgData);
        msg.setStartTime(new Date().getTime());
        messageMongoDBMapper.insertGlobalUserMsg(MessageType.NOTIFICATION, msg);
    }

    public void broadcastSysOpMessage2Anony(final String title, String desc) throws Exception {
        Message msg = new Message();
        msg.setAppNme(title);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_MSG, null, desc);
        msg.setMsgData(msgData);
        msg.setStartTime(new Date().getTime());
        messageMongoDBMapper.insertAnonyUserMsg(MessageType.NOTIFICATION, msg);
    }

    /**
     * 推送系统信息给指定用户
     * 
     * @param desc
     * @param contentUrl
     * @param sendUidList
     * @throws Exception
     */
    public void broadcastSysMessage(final String title, String desc, String contentUrl, List<Long> sendUidList) throws Exception {
        Message msg = new Message();
        TitleAndMsgBuilder builder = new TitleAndMsgBuilder() {
            public void build(Message msg, Locale locale) throws Exception {
                msg.setAppNme(title);
            }
        };
        msg.resetBuilder(builder);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_BC, null, desc);
        msgData.put(FIELD_MSG_OP_BC_URL, contentUrl);
        msg.setMsgData(msgData);
        msg.setShouldPersist(true);// 存入信箱
        pushManyUids(MessageType.NOTIFICATION, msg, sendUidList);
    }

    /**
     * 推送系统无跳转信息给指定用户
     * 
     * @param desc
     * @param sendUidList
     * @throws Exception
     */
    public void broadcastSysOpMessage(final String title, String desc, List<Long> sendUidList) throws Exception {
        Message msg = new Message();
        TitleAndMsgBuilder builder = new TitleAndMsgBuilder() {
            public void build(Message msg, Locale locale) throws Exception {
                msg.setAppNme(title);
            }
        };
        msg.resetBuilder(builder);
        msg.setMsgTitle(desc);
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(S_OP_MSG, null, desc);
        msg.setMsgData(msgData);
        msg.setShouldPersist(true);// 存入信箱
        pushManyUids(MessageType.NOTIFICATION, msg, sendUidList);
    }

    public void broadcastTurnoverMessage(String msgStr, List<Long> sendUidList) throws Exception {
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_TO_BC, null, msgStr);
        if (sendUidList.isEmpty()) {
            messageMongoDBMapper.broadcastAllPassThrough(MessageType.PASS_THROUGH, msgData);
        } else {
            messageMongoDBMapper.send2UsersPush(sendUidList, msgData);
        }
    }

    /**
     * 6种操作下的直播更新数据广播
     * 
     * @param
     * @param lid
     * @param
     * @param
     * @throws Exception
     */
    public void sendUserLsUpdate(String lid, BroadcastBuilder liveShowBroadcast) throws Exception {
        try {
            Message msg = new Message();// TODO 只有MessageType.Notification类型的message是需要填写appName和msgTitle，其他都不需要！
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_LIVE_DATA, null, null);
            liveShowBroadcast.buildMapData(msgData);
            msg.setMsgData(msgData);
            messageMongoDBMapper.pushTopic(U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, msg);// 这个topic结构是跟客户端商定的
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void sendUserLsUpdate(String lid, ObjectNode jo) throws Exception {
        try {
            Message msg = new Message();// TODO 只有MessageType.Notification类型的message是需要填写appName和msgTitle，其他都不需要！
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_LIVE_DATA, null, null);
            for (Map.Entry<String, Object> entry : msgData.entrySet()) {
                Object obj = entry.getValue();
                if (obj instanceof Boolean) {
                    jo.put(entry.getKey(), (Boolean) entry.getValue());
                } else if (obj instanceof Character) {
                    jo.put(entry.getKey(), (Character) entry.getValue());
                } else if (obj instanceof Number) {
                    jo.put(entry.getKey(), entry.getValue().toString());
                } else if (obj instanceof String) {
                    jo.put(entry.getKey(), (String) entry.getValue());
                } else if (obj instanceof JsonNode) {
                    jo.put(entry.getKey(), (JsonNode) entry.getValue());
                } else {
                    jo.put(entry.getKey(), JsonUtil.createDefaultMapper().valueToTree(entry.getValue()));
                }
            }
            PushMessage pushMessage = new PushMessage();
            pushMessage.setTitle(msg.getAppNme());
            pushMessage.setDesc(msg.getMsgTitle());
            pushMessage.setMsgType(MessageType.PASS_THROUGH);
            pushMessage.setData(getLocalObjMapper().writeValueAsString(jo));
            pushMessage.setExpiry(msg.getExpiry());
            messageMongoDBMapper.pushTopic(U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, pushMessage);// 这个topic结构是跟客户端商定的
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void pushCountDownMsg(String lid, Long duration, int type) throws Exception {
        try {
            Message msg = new Message();
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_LIVE_COUNTDOWN, null, null);
            msgData.put(MSG_F_COUNTDOWN_DURATION, duration);
            msgData.put(MSG_F_COUNTDOWN_STARTTIME, System.currentTimeMillis());
            msgData.put(MSG_F_COUNTDOWN_TYPE, type);
            msgData.put(MSG_F_COUNTDOWN_LID, lid);
            msg.setMsgData(msgData);
            messageMongoDBMapper.pushTopic(U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, msg);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void pushToUpdateDistance(String lid, Long uid, String distance) {
        try {
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_UPDATE_DISTANCE, null, null);
            msgData.put(MSG_U_UPDATE_DISTANCE_LID, lid);
            msgData.put(MSG_U_UPDATE_DISTANCE_DISTANCE, distance);
            messageMongoDBMapper.send2UserPush(uid, msgData);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 给uid发送消息气泡(包含userInfode信息)
     * 
     * @param userInfo 发送的用户的信息
     * @param uid 发送的目标用户
     * @param type 1 申请加我好友 2 访问了我的个人主页 3 我的申请被回复
     * @throws Exception
     */
    public void pushBubbleMsg(UserInfo userInfo, Long uid, Integer type, long mc) throws Exception {
        // Message msg = new Message();
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(U_CHECK_BUBBLEMSG, null, null);
        msgData.put(MSG_U_BUBBLE_UID, userInfo.getUid());
        msgData.put(MSG_U_BUBBLE_NICK, userInfo.getNick());
        msgData.put(MSG_U_BUBBLE_HEADURL, userInfo.getHeaderUrl());
        msgData.put(MSG_U_BUBBLE_MC, mc);
        msgData.put(MSG_U_BUBBLE_TYPE, type);// 1 加我好友 2 访问了我的主页 3 我的申请被回复
        // msg.setMsgData(msgData);
        messageMongoDBMapper.send2UserPush(uid, msgData);

    }

    private void pushManyUids(MessageType messageType, Message msg, List<Long> uids) throws Exception {
        List<UserInfo> users = userHessianService.getClient().findUserListByUids(uids, false);
        messageMongoDBMapper.insertAndPushManyUserMsg(messageType, msg, users);
    }

    public void pushTurnoverOpMsg(Long uid, String comment, String img, boolean fillUserInfo) throws Exception {
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_OP_TO_BC, null, null);
        msgData.put("uid", uid);
        msgData.put("comment", comment);
        msgData.put("backImgUrl", img);
        if (fillUserInfo) {
            UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
            if (userInfo != null) {
                msgData.put("nick", userInfo.getNick());
                msgData.put("headerUrl", userInfo.getHeaderUrl());
            }
        }
        messageMongoDBMapper.broadcastAllPassThrough(MessageType.PASS_THROUGH, msgData);
    }

    public void pushTurnoverOpMsgRubbish(Long uid, String comment, String img, Long recvUid) throws Exception {
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_OP_TO_BC, null, null);
        // msgData.put("uid", uid);
        msgData.put("comment", comment);
        msgData.put("backImgUrl", img);
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
        if (userInfo != null) {
            // msgData.put("nick", userInfo.getNick());
            msgData.put("headerUrl", userInfo.getHeaderUrl());
        }
        userInfo = userHessianService.getClient().getUserByUid(recvUid, false);
        if (userInfo != null) {
            msgData.put("recvNick", userInfo.getNick());
        }
        messageMongoDBMapper.broadcastAllPassThrough(MessageType.PASS_THROUGH, msgData);
    }

    public void pushTurnoverOpMsgRubbish2Channel(Long uid, Long recvUid, String comment, String img, String lid) throws Exception {
        Message msg = new Message();
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_OP_TO_BC, null, null);
        // msgData.put("uid", uid);
        msgData.put("comment", comment);
        msgData.put("backImgUrl", img);
        msgData.put("lid", lid);
        UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
        if (userInfo != null) {
            msgData.put("nick", userInfo.getNick());
            msgData.put("headerUrl", userInfo.getHeaderUrl());
        }
        userInfo = userHessianService.getClient().getUserByUid(recvUid, false);
        if (userInfo != null) {
            msgData.put("recvNick", userInfo.getNick());
        }
        msg.setMsgData(msgData);
        messageMongoDBMapper.pushTopic(MsgDataType.U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, msg);
    }

    public void sendOpMedalAdd(List<Long> uids,Medal medal) throws Exception {
        Message message=new Message();
        Map<String,Object> map = messageMongoDBMapper.genActMsgData(MsgDataType.S_OP_MEDAL_ADD, null, null);
        map.put("medal",medal);
        message.setMsgData(map);
        message.setShouldPersist(true);// 存入信箱
        message.set_ttserver(1);
        message.setStartTime(System.currentTimeMillis());
        message.setOneTime(true);
        messageMongoDBMapper.insertAndPushManyCtrlMsg(MessageType.PASS_THROUGH, message,uids);
    }

    /**
     * 发指定消息类型push。并写入ctrl消息
     * @param uid
     * @param msg
     * @throws Exception
     */
    public void sendPushAndInsertCtrlMsg(long uid, String msg,MsgDataType msgDataType) throws Exception {
        List<Long> uids = new ArrayList<>();
        uids.add(uid);
        Message message=new Message();
        Map<String,Object> map = messageMongoDBMapper.genActMsgData(msgDataType, null, msg);
        message.setMsgData(map);
        message.setShouldPersist(true);// 存入信箱
        message.set_ttserver(1);
        message.setStartTime(System.currentTimeMillis());
        message.setOneTime(true);
        messageMongoDBMapper.insertAndPushManyCtrlMsg(MessageType.PASS_THROUGH, message,uids);
    }
    /**
     * 发生一级封禁，二级封禁解封push。写入ctrl消息
     * @param uid
     * @param msg
     * @throws Exception
     */
    public void sendReportCancelBan(long uid,String msg) throws Exception {
        List<Long> uids = new ArrayList<>();
        uids.add(uid);
        Message message=new Message();
        Map<String,Object> map = messageMongoDBMapper.genActMsgData(MsgDataType.S_REPORT_CANCEL_BAN, null, msg);
        message.setMsgData(map);
        message.setShouldPersist(true);// 存入信箱
        message.set_ttserver(1);
        message.setStartTime(System.currentTimeMillis());
        message.setOneTime(true);
        messageMongoDBMapper.insertAndPushManyCtrlMsg(MessageType.PASS_THROUGH, message,uids);
    }
    /**
     * 当产生一级封禁，二级封禁时候，需要给举报人写用户消息
     * @param uid
     * @param msg
     * @throws Exception
     */
    public void sendReportBanReward(long uid,String msg) throws Exception {
        Message message=new Message();
        message.setUid(uid);
        Map<String,Object> map = messageMongoDBMapper.genActMsgData(MsgDataType.S_REPORT_BAN_REWARD, null, msg);
        message.setMsgData(map);
        message.setStartTime(System.currentTimeMillis());
        messageMongoDBMapper.insertUserMsg(message);
    }
    public void pushAnchorAuthResult2User(long uid, boolean status) throws Exception {
        Message msg = new Message();
        Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_ANCHOR_AUTH_RESULT, null, null);
        msgData.put(FIELD_MSG_TITLE, status?"你的实名认证申请已通过审核，快来开播吧~":"你的实名认证申请未通过审核，请重新提交申请。");
        msgData.put(FIELD_MSG_PASS, status);
        msg.setMsgData(msgData);
        msg.setUid(uid);
        msg.set_ttserver(1);
        msg.setStartTime(System.currentTimeMillis());
        msg.setShouldPersist(true);
        messageMongoDBMapper.insertCtrlMsg(msg);
    }
}
