package com.yy.me.enums;

/**
 * 上报类型
 * 
 * @author Jiang Chengyan
 * 
 */
public enum ReportType {
    USER(1), FEED(2), LIVESHOW(3), FEED_COMMENT(4);

    private ReportType(int value) {
        this.value = value;
    }

    int value;

    public int getValue() {
        return value;
    }
}
