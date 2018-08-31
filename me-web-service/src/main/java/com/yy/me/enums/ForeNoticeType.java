package com.yy.me.enums;

public enum ForeNoticeType {
    WEBLINK(0), LIVELINK(1), TOPICLSLINK(2), NONE(3);

    int value;

    private ForeNoticeType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ForeNoticeType findByValue(int value) {
        switch (value) {
        case 0:
            return WEBLINK;
        case 1:
            return LIVELINK;
        case 2:
            return TOPICLSLINK;
        case 3:
            return NONE;
        default:
            return WEBLINK;
        }
    }
}
