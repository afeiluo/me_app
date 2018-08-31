package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;

public enum TAppId
{
    FINANCE(1), DATING(2), HUNDRED(3), FREE_SHOW(4), GAME_GUILD(5), KTV(6), BLACKJACK(7), SPY(8), SLAVE_SALES(9), SCRATCH_OFF(10), NIUNIU(11), MEDICAL_TREATMENT(12), SPORT(13), VIP_PK(14), HELLO_APP(15), FINANCE_FORCE_RELIEVE_CONTRACT(16), GAME_SPOT(17), BILIN(18), XUN_HUAN(19), WEI_FANG(20), TINY_TIME(21), YO_MALL(22);

    private final int value;

    TAppId(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }

    private static TAppId[] values = TAppId.values();
    public static TAppId valueOf(int value)
    {
        for(TAppId val :values){
            if (val.getValue()==value){
                return val;
            }
        }
        return null;
    }
}
