package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;

public enum TUserType
{
    ANCHOR(1), OW(2), VISITOR(3), GUEST(4);

    private final int value;

    TUserType(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }

    private static TUserType[] values = TUserType.values();
    public static TUserType valueOf(int value)
    {
        for(TUserType val :values){
            if (val.getValue()==value){
                return val;
            }
        }
        return null;
    }
}
