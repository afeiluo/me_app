package com.yy.me.pay.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.duowan.yy.sysop.yctoken.YCTokenAppSecretProvider;
import com.duowan.yy.sysop.yctoken.YCTokenBuilder;
import com.duowan.yy.sysop.yctoken.YCTokenPropertyProvider;

public class PaymentTokenUtil {

    private final static int INCOME_APP_ID = 1;
    private final static String INCOME_APP_SECRET_TEST = "fdb91ccf_0";
    private final static String INCOME_APP_SECRET_PROD = "fdb91ccf_0sris2";
    private final static short INCOME_APP_VERSION = 1;

    /**
     * 红包功能使用
     */
    private final static int INCOME_APP_ID_FOR_RED_ENVELOPE = 2;
    private final static String INCOME_APP_SECRET_FOR_RED_ENVELOPE_TEST = "srfzew432";
    private final static String INCOME_APP_SECRET_FOR_RED_ENVELOPE_PROD = "srfzew432_zwevsn";
    private final static short INCOME_APP_VERSION_FOR_RED_ENVELOPE = 1;

    public static String genToken(final boolean isProd, final long uid) {
        YCTokenBuilder tokenBuilder = new YCTokenBuilder(new YCTokenAppSecretProvider() {
            public Map<Short, String> getAppsecret(int appKey) {
                Map<Short, String> m = new HashMap<Short, String>();
                // 传入密钥版本和密码
                if (isProd) {
                    m.put(INCOME_APP_VERSION, INCOME_APP_SECRET_PROD);
                } else {
                    m.put(INCOME_APP_VERSION, INCOME_APP_SECRET_TEST);
                }

                return m;
            }
        });

        // 计算token有效期,初步设置10分钟过期
        long expireTime = (System.currentTimeMillis() + 10 * 60 * 1000) / 1000;
        // 传入appkey和token有效期
        YCTokenPropertyProvider propProvider = new YCTokenPropertyProvider(INCOME_APP_ID, expireTime);
        // 传入业务需要的具体参数，具体参数以业务为准
        propProvider.addTokenExtendProperty("UID", uid);
        propProvider.addTokenExtendProperty("SID", 0L);
        Base64 coder = new Base64(76, new byte[] {}, true);
        byte[] buildBinanyToken = tokenBuilder.buildBinaryToken(propProvider);
        String token = coder.encodeAsString(buildBinanyToken); // 打印token

        return token;
    }

    public static String genEnvelopeToken(final boolean isProd, final long uid, final String lid) {
        YCTokenBuilder tokenBuilder = new YCTokenBuilder(new YCTokenAppSecretProvider() {
            public Map<Short, String> getAppsecret(int appKey) {
                Map<Short, String> m = new HashMap<Short, String>();
                // 传入密钥版本和密码
                if (isProd) {
                    m.put(INCOME_APP_VERSION_FOR_RED_ENVELOPE, INCOME_APP_SECRET_FOR_RED_ENVELOPE_PROD);
                } else {
                    m.put(INCOME_APP_VERSION_FOR_RED_ENVELOPE, INCOME_APP_SECRET_FOR_RED_ENVELOPE_TEST);
                }

                return m;
            }
        });

        // 计算token有效期,初步设置10分钟过期
        long expireTime = (System.currentTimeMillis() + 10 * 60 * 1000) / 1000;
        // 传入appkey和token有效期
        YCTokenPropertyProvider propProvider = new YCTokenPropertyProvider(INCOME_APP_ID_FOR_RED_ENVELOPE, expireTime);
        // 传入业务需要的具体参数，具体参数以业务为准
        propProvider.addTokenExtendProperty("UID", uid);
        propProvider.addTokenExtendProperty("SID", 0L);
        if (StringUtils.isNotBlank(lid)) {
            propProvider.addTokenExtendProperty("LID", lid);
        }
        Base64 coder = new Base64(76, new byte[] {}, true);
        byte[] buildBinanyToken = tokenBuilder.buildBinaryToken(propProvider);
        String token = coder.encodeAsString(buildBinanyToken); // 打印token

        return token;
    }

}
