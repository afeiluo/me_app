package com.yy.me.service.inner;

import java.util.Random;

/**
 * 注：对于需要选择属主ID作为ttserver参考值的collection，选择当前插入机器所在地区mid会更加合适，如100、200等——根据地区来确定“安全可靠的写入位置”
 * 1. 对于高频操作（用户经常性调用），采用属主ID的机器（如lid、fid的mid，或uid的ttserver）
 * 2. 对于临时性有效的数据，_id和ttserver采用当前机器（如有lid，则采用lid的mid值）——或者用地区mid，如100、200等
 * 3. 对于只插入，没有其他操作的ttserver，采用当前机器
 * 4. 对于只插入，没有其他操作且可丢弃的操作的ttserver，采用属主ID的机器（如lid、fid的mid，或uid的ttserver）
 * COLLETION_USER_LAST_LOAD_FEED_TIME_NAME、COLLETION_USER_LAST_ENTER_LIVE_SHOW_NAME采用放弃一致性！同时，当用户的请求更换到不同的服务器时，也会当作找不到处理
 * ！！
 * 
 * @author Jiang Chengyan
 * 
 */
public class ServiceConst {
    /**
     * 用户集合
     */
    public static final String COLLETION_USER_INFO_NAME = "user_info";

    /**
     * 用户信息更新时间
     */
    public static final String FIELD_HIIDO_UT = "ut";
    public static final String FIELD_HIIDO_LASTLIVETIME = "lastLiveTime";// 上次直播的时间

    public static final String FIELD_LS_LID = "lid";
    public static final String FIELD_LS_PCU_HIIDO = "pcu4Hiido";
    public static final String FIELD_LS_NORMAL_USER_HIIDO = "normalUser4Hiido";

    public static final String FIELD_RULE_DESC_ITEM = "banedItem";
    public static final String FIELD_RULE_DESC_SUB_ITEM = "banedSubItem";
    public static final String FIELD_RULE_DESC_DESC = "banedDesc";

    // MongoDB - 强升
    public static final String FIELD_FORCE_UPGRADE_ANDROID = "Android";
    public static final String FIELD_FORCE_UPGRADE_IOS = "iOS";

    public static final String RET_U_UID = "uid";
    public static final String RET_U_DEVICE_ID = "deviceId";
    public static final String RET_U_OS_TYPE = "osType";
    public static final String RET_U_LINK_DEVICE_ID = "linkDeviceId";
    public static final String RET_U_LINK_OS_TYPE = "linkOsType";
    public static final String RET_U_NEWUSER = "newUser";
    public static final String RET_U_FIRSTTIME = "firstTime";
    public static final String RET_U_ANCHOR_TYPE = "anchorType";
    public static final String RET_U_IDENTITY_VERIFIED = "idVerified";
    public static final String RET_U_RELATION = "relation";
    public static final String RET_U_CANSUPERLIKE = "canSuperLike";

    // 审核类别字段(int)
    public static final String MMS_TYPE = "mmsType";
    public static final String MMS_PIC_U_OLD_HEADER_URL = "oldHeaderUrl";
    public static final String MMS_ACTION_TIME = "actionTime";
    public static final String MMS_HEAD_URL="headUrl";
    public static final String MMS_VIDEO_URL="videoUrl";

    public static final String REFRESH_TOP_LIVESHOW_SIGN = "top_live_show_sign";
    public static final String REFRESH_STABLE_MISAKA_CONNECT_SIGN = "refresh_stable_misaka_connect_sign";
    public static final String UPDATE_CLIENT_RESOURCE_SIGN = "update_client_resource_sign";
    public static final String REFRESH_GIFT_ICON_SIGN = "refresh_gift_icon_sign";
    public static final String REFRESH_GIFT_PANEL_AD_SIGN = "refresh_gift_panel_ad_sign";
    public static final String REFRESH_GIFT_RECHARGE_ACTIVITY_AD_SIGN = "refresh_gift_recharge_act_ad_sign";
    public static final String REFRESH_HONGBAO_ICON_SIGN = "refresh_hongbao_icon_sign";
    public static final String REFRESH_TURNOVAL_MEDAL_SIGN = "refresh_turnoval_medal_sigin";
    public static final String BROADCAST_LIVESHOW_GUESTCOUNT_SIGN = "broadcast_liveshow_guestcount_sign";
    public static final String REFRESH_LIVESHOW_TOTALGUESTCOUNT_SIGN = "refresh_liveshow_totalguestcount_sign";
    public static final String REFRESH_LIVESHOW_GUEST_CHECK_SIGN = "refresh_liveshow_guest_check_sign";
    public static final String CLEAR_BANED_END_USER_SIGN = "clear_baned_end_user_sign";
    public static final String SEARCH_UPLOAD_SIGN = "search_upload_sign";
    public static final String UPDATE_BANNER_SIGN = "update_banner_sign";
    public static final String UPDATE_ANNOUNCE_SIGN = "update_announce_sign";
    public static final String UPDATE_FORENOTICE_SIGN = "update_forenotice_sign";

    // 礼包消息透传消息
    public static final String GIFT_EXPAND_KEY_LID = "lid";
    public static final String GIFT_EXPAND_KEY_COMBO_ID = "comboId";
    public static final String GIFT_EXPAND_KEY_ROBOT = "robot";
    public static final String GIFT_EXPAND_KEY_MSG = "msg";

    public static final String FIELD_MSG_TITLE = "title";
    public static final String FIELD_MSG_PASS = "pass";

    public static Random rand = new Random(System.currentTimeMillis());

    public static boolean productEnv = true;

    public static boolean closeLink = false;

}
