package com.yy.me.enums;

public enum LsStopReason {
    UNKNOWN(0, "未知"), USER_PRESS(1, "用户手工停止"), HUANJUYUN(2, "欢聚云"), CHANG_LIAN_JIE(3, "长连接"), KICK_SELF(4, "自己踢自己"), VIOLATE(5, "违反运营规则被禁止"), START_NEW(6, "自己开新直播，但旧的未停止"), WEB(7, "业务后台手工停止"), CLIENT_SELF_REQUEST(8, "丢包率"), WEB_VIOLATE(9, "（违规）业务后台手工停止"), CHANG_LIAN_JIE_SELF_KICK(10, "长连接自己踢自己"), KICK_SELF_START(11, "（开播）自己踢自己"), KICK_SELF_GUEST_ENTER(12, "（另一手机进直播间）自己踢自己");

    String desc;
    int value;

    private LsStopReason(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public static LsStopReason findByValue(int value) {
        for (LsStopReason reason : values()) {
            if (value == reason.value) {
                return reason;
            }
        }
        return UNKNOWN;
    }
}
