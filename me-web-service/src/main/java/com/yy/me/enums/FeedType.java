package com.yy.me.enums;

/**
 * Feed流内容类型
 * 
 * 注：IDOL_LIVE、IDOL_SHARE_LIVE仅用于/feed/indexList接口中动态生成数据使用（即其FID是虚构的，并不存在于DB中），并不适用于普通存储（如PIC、VIDEO、REPLAY）
 * 
 * @author Jiang Chengyan
 * 
 */
public enum FeedType {
    TEXT_ONLY(0), PIC(1), VIDEO(2), REPLAY(3), IDOL_LIVE(4), IDOL_SHARE_LIVE(5), RECOMMEND_LIVE(6), RECOMMEND_PIC_FEED(7), RECOMMEND_VIDEO_FEED(8), RECOMMEND_REPLAY_FEED(
            9), GDS_REPLAY(10), TOPIC_TOP3_LIVE(11), TOP_LIVE(12),TOP_LIVE_A(14),TOP_LIVE_B(15),TOP_LIVE_C(16), NORMAL_LIVE(13),HOT_LIVE_TOSEE(17), STICK(18),COLUMN_LIVE(19),HIIDO_RECOM(20),TOPIC_LIVE(21),PAYMENT_TOP(22);// 仅用2周的推荐录播

    private FeedType(int value) {
        this.value = (byte) value;
    }

    int value;

    public int getValue() {
        return value;
    }

    public static boolean shouldFillPicUrl(FeedType type) {
        switch (type) {
        case IDOL_LIVE:
        case IDOL_SHARE_LIVE:
        case RECOMMEND_LIVE:
            return true;
        default:
            break;
        }
        return false;
    }

    public static FeedType findFeedType(int value) {
        for (FeedType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return TEXT_ONLY;
    }

}
