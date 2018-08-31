package com.yy.me.enums;

/**
 * Created by ben on 16/7/19.
 */
public enum CountDownOperType {
    SET(1), START(2), STOP(3), PAUSE(4);

    int value;

    private CountDownOperType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CountDownOperType getOperType(int value) {
        for (CountDownOperType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return SET;
    }
}
