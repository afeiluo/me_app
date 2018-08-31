package com.yy.me.enums;

public enum BannerType {
    WEBLINK(0), LIVELINK(1), TOPICLSLINK(2);

    int value;

    private BannerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BannerType findByValue(int value) {
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
