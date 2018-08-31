package com.yy.me.enums;

public enum AnnounceType {
    WEBLINK(0), LIVELINK(1), TOPICLSLINK(2);

    int value;

    private AnnounceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AnnounceType findByValue(int value) {
        switch (value) {
        case 0:
            return WEBLINK;
        case 1:
            return LIVELINK;
        case 2:
            return TOPICLSLINK;
        default:
            return WEBLINK;
        }
    }
}
