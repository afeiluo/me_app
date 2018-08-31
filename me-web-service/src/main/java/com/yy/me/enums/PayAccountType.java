package com.yy.me.enums;

public enum PayAccountType {
    ALIPAY(1), // 支付宝
    ;

    int value;

    private PayAccountType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PayAccountType findByValue(int value) {
        switch (value) {
        case 1:
            return ALIPAY;
        default:
            return null;
        }
    }

}
