package com.yy.me.enums;

/**
 * 直播送审类型
 * @author Jiang Chengyan
 *
 */
public enum ReportVerifyType {
    FEED_PIC(1), LS_SNAPSHOT(2), USER_HEADER(3);

    private ReportVerifyType(int value) {
        this.value = value;
    }

    int value;

    public int getValue() {
        return value;
    }

    public static ReportVerifyType findReportVerifyType(int value) {
        for (ReportVerifyType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return FEED_PIC;
    }
}
