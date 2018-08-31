package com.yy.me.pay.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by Chris on 16/1/20.
 */
public class GiftCallbackReq {

    private String seq; // 流水号
    private long uid; // 送礼物的用户uid
    private long recvUid; // 收礼物的用户uid
    private long usedTimestamp; // 礼物送出时间
    private List<UseInfo> useInfos; // 礼物信息
    private String expand; // 透传的扩展信息
    private int platform; // 送出礼物的平台

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getRecvUid() {
        return recvUid;
    }

    public void setRecvUid(long recvUid) {
        this.recvUid = recvUid;
    }

    public long getUsedTimestamp() {
        return usedTimestamp;
    }

    public void setUsedTimestamp(long usedTimestamp) {
        this.usedTimestamp = usedTimestamp;
    }

    public List<UseInfo> getUseInfos() {
        return useInfos;
    }

    public void setUseInfos(List<UseInfo> useInfos) {
        this.useInfos = useInfos;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class UseInfo {
        private int propId; // 道具Id
        private int currencyType; // 货币类型, 米币 - 24 当前可有免费的货币类型
        private int amount; // 金额
        private int propCount; // 数量
        private int income; // 主播获得的收益（米豆）
        private int intimacy; // 亲密度值

        public int getPropId() {
            return propId;
        }

        public void setPropId(int propId) {
            this.propId = propId;
        }

        public int getCurrencyType() {
            return currencyType;
        }

        public void setCurrencyType(int currencyType) {
            this.currencyType = currencyType;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getPropCount() {
            return propCount;
        }

        public void setPropCount(int propCount) {
            this.propCount = propCount;
        }
        
        public int getIncome() {
            return income;
        }

        public void setIncome(int income) {
            this.income = income;
        }

        public int getIntimacy() {
            return intimacy;
        }

        public void setIntimacy(int intimacy) {
            this.intimacy = intimacy;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public enum CurrencyType {
        MIBI(24), // 米币
        ;

        int value;

        private CurrencyType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum PlatformInfo {
        PC(1),
        Web(2),
        Android(3),
        IOS(4),
        AndroidPad(5),
        IPAD(6),
        WinPhone(7),;

        int value;

        private PlatformInfo(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

}
