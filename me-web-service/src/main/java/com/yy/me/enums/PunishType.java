package com.yy.me.enums;

/**
 * 处罚类型
 *
 * Created by Chris on 16/6/14.
 */
public enum PunishType {
    HEAD(1), // 头像违规
    LIVESHOW(2), // 直播违规
    LIVESHOW_A(3), // 直播A类违规
    LIVESHOW_B(4), // 直播B类违规
    NICK(5),//昵称违规
    SIGNATURE(6),//签名违规
    LIVESHOW_LINK_A(7), // 连麦A类违规
    LIVESHOW_LINK_B(8) // 连麦B类违规
    ;

    private int type;

    PunishType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
