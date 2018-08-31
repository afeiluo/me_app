package com.yy.me.enums;

/**
 * MMS送审素材类别
 * 
 * @author Jiang Chengyan
 * 
 */
public enum MmsType {
    USER_HEAD(1), FEED_PIC(2), FEED_COMMENT(3), ANCHOR_EGG_PIC(4), LS_SNAPSHOT(5), USER_NICK(6), USER_SIGNATURE(7), LS_LINK_SNAPSHOT(8), USER_VIDEO(9), USER_MULTI_HEAD(
            10);

    MmsType(int value) {
        this.value = value;
    }

    int value;

    public int getValue() {
        return value;
    }
}
