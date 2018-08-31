package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;

public enum TCurrencyType
{
    VIRT(1), REAL(2), ACTIVITY(3), YB(4), TIME(5), COMMISSION(6), SYCEE(7), GOLDEN(8), SILVER(9), COPPER(10), RMB(11), SILVER_SHELL(12), HELLO__GOLDEN(13), HELLO__DIAMOND(14), HELLO__APPLE_DIAMOND(15), HELLO__RED_DIAMOND(16), SUPER_PURPLE_DIAMOND(17), RED_PACKET(18), XH__GOLDEN(19), XH__DIAMOND(20), XH__RUBY(21), BILIN__WHALE(22), BILIN__PROFIT(23), TINY_TIME__MI_BI(24), TINY_TIME__MI_DOU(25), TINY_TIME__PROFIT(26), TINY_TIME__EDOU(27), YO_MALL__SALARY(28), ME_MIDAS__MIBI(29), TB_TDOU(100);

    private final int value;

    TCurrencyType(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }

    private static TCurrencyType[] values = TCurrencyType.values();
    public static TCurrencyType valueOf(int value)
    {
        for(TCurrencyType val :values){
            if (val.getValue()==value){
                return val;
            }
        }
        return null;
    }
}
