package com.yy.tinytimes.thrift.turnover.currency;

import com.facebook.swift.codec.*;

public enum TAccountOperateType
{
    WITHDRAW(1), EXCHANGE(2), CONSUME_PROPS(3), PROPS_REVENUE(4), BUY_VIRT_OVERAGE(5), BUY_VIRT_OVERAGE_FAIL(6), ACTIVITY_AUTO_INC(7), AUTO_MONTH_SETTLE(8), ACCOUNT_FREEZE(9), ACCOUNT_UNFREEZE(10), CHANNEL_REAL_TO_PERSON_VIRT(11), ISSUE_SYCEE(12), TRANSFER(13), SYSTEM_OPER(14), SYSTEM_COMPENSATE(15), EXTERNAL_MODIFICATION(16), GIFT_BAG_LOTTERY(17), CHARGE_CURRENCY(18), CHARGE_CURRENCY_PRESENT(19), CHARGE_CURRENCY_DISCOUNT(20), DATING_BACKUP_GROUP(21), DATING_BACKUP_GROUP_BACK_FEE(22), DATING_BACKUP_GROUP_FINISH(23), BUY_SKIN(24), BUY_SEAL(25), BUY_SEAL_BACK_FEE(26), RED_PACKET_ISSUE(27), RED_PACKET_CHARGE(28), RED_PACKET_GRAB(29), RED_PACKET_CLOSE(30), NOBLE_OPEN(31), NOBLE_RENEW(32), NOBLE_UPGRADE(33), UPGRADE_PROPS(34), REVERT_PAY(35), WITHDRAW_BACK(36), NOBLE_RENEW_EXCHANGE_VIRT(37), OFFICIAL_ISSUE(38), PAY_VIP_ROOM(39), CONSUME_PROPS_FOR_OTHER(40), VIRT_LOTTERY(41), BUY_SPOOF_HANGING(42), LUCKY_TREASURES(43), PRODUCT_CONSUME(44), PRODUCT_CONSUME_REVERT(45), BUY_LOTTERY_CHANCE(46);

    private final int value;

    TAccountOperateType(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }

    private static TAccountOperateType[] values = TAccountOperateType.values();
    public static TAccountOperateType valueOf(int value)
    {
        for(TAccountOperateType val :values){
            if (val.getValue()==value){
                return val;
            }
        }
        return null;
    }
}
