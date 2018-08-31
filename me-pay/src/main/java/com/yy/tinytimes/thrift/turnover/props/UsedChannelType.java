package com.yy.tinytimes.thrift.turnover.props;

import com.facebook.swift.codec.*;

public enum UsedChannelType
{
    CLIENT(0), WEB(10000), IOS(10001), ANDROID(10002), IOS_CRACKED(10003), WECHAT_OFFICIAL_ACCOUNT(10004), TURNOVER_WEB(1), DATING_COM(2), YLPHONE(3), YLSERVER(4), BAIDU_TIEBA(5), FINANCE_APP(6), DATING_APP_IOS(7), DATING_APP_ANDROID(8), YY_LOVE_APP_IOS(9), YY_LOVE_APP_ANDROID(10), YY_SHI_TING_APP_ANDROID(11), YY_SHI_TING_APP_IOS(12), FINANCE_SC(13), DATING_APP_IOSCRACKED(14), YY_LOVE_APP_IOSCRACKED(15), MEXIAOMI(16), MEGAME_STORE(17), MEBAIDU_BROWSER(18), MEBILIN(19), DATING_BLIND_IOS(20), DATING_BLIND_ANDROID(21), VIP_PEI_LIAO_ANDROID(22), VIP_PEI_LIAO_IOS(23), MEMIDAS(24), MEBAIDU_PIC(25), MEJD(26), MEEMULATOR_APP(27), MEKUAIKAN(28);

    private final int value;

    UsedChannelType(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }

    private static UsedChannelType[] values = UsedChannelType.values();
    public static UsedChannelType valueOf(int value)
    {
        for(UsedChannelType val :values){
            if (val.getValue()==value){
                return val;
            }
        }
        return null;
    }
}
