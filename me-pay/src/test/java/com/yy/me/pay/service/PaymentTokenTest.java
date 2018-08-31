package com.yy.me.pay.service;

import com.duowan.yy.sysop.yctoken.YCToken;
import com.duowan.yy.sysop.yctoken.YCTokenAppSecretProvider;
import com.duowan.yy.sysop.yctoken.YCTokenBuilder;
import com.duowan.yy.sysop.yctoken.YCTokenPropertyProvider;
import com.yy.me.time.DateTimeUtil;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PaymentTokenTest {

    private final static int INCOME_APP_ID = 1;
    private final static String INCOME_APP_SECRET_TEST = "fdb91ccf_0";
    private final static String INCOME_APP_SECRET_PROD = "fdb91ccf_0sris2";
    private final static String INCOME_APP_SECRET_FOR_RED_ENVELOPE_TEST = "srfzew432";
    private final static String INCOME_APP_SECRET_FOR_RED_ENVELOPE_PROD = "srfzew432_zwevsn";
    private final static short INCOME_APP_VERSION = 1;

    @Test
    public void testToken() throws UnsupportedEncodingException {
        String token = "bwA9AAEAAAABAD-jA1cAAAAA56ADVwAAAAD7FsEXA1VJRAgIAAC0xAQAAAAAA1NJRAgIAAAAAAAAAAAAA0xJRAkYADU2ZWZlMTcwMDAwMDY3MmU0NzBhMzZiNU-aIcKeLQ1Pnd6-gl7u-R6tvTDQ";

        Base64 coder = new Base64();
        // 分解token读取里面内容
        byte[] decode = coder.decode(token.getBytes(StandardCharsets.UTF_8));

        YCTokenBuilder tokenBuilder = new YCTokenBuilder(new YCTokenAppSecretProvider() {
            public Map<Short, String> getAppsecret(int appKey) {
                Map<Short, String> m = new HashMap<Short, String>();
                // 传入密钥版本和密码
                m.put(INCOME_APP_VERSION, INCOME_APP_SECRET_FOR_RED_ENVELOPE_TEST);

                return m;
            }
        });

        YCToken tokenBytes = tokenBuilder.validateTokenBytes(decode);
        System.out.println("appKey：" + tokenBytes.getAppKey());
        System.out.println("expireTime：" + tokenBytes.getExpireTime());
        System.out.println("timestamp：" + tokenBytes.getTimestamp());
        System.out.println("version: " + tokenBytes.getVersion());
        
        long expireTime = tokenBytes.getExpireTime() * 1000;
        System.out.println("有效期至：" + DateTimeUtil.formatDateTime(new Date(expireTime)));
        
//        System.out.println("expire time: " + DateFormatUtil.formatDateTime(new Date(1455257950590L)));
//        System.out.println("current time: " + DateFormatUtil.formatDateTime(new Date(1455257884619L)));

        if (expireTime < System.currentTimeMillis()) {
            System.out.println("token已失效");
        }

        ByteBuffer bufferUid = ByteBuffer.wrap(tokenBytes.fetchExtendPropertyValue("UID"));
        bufferUid.order(ByteOrder.LITTLE_ENDIAN);
        System.out.println("uid：" + bufferUid.asIntBuffer().get());

        ByteBuffer bufferSid = ByteBuffer.wrap(tokenBytes.fetchExtendPropertyValue("SID"));
        bufferSid.order(ByteOrder.LITTLE_ENDIAN);
        System.out.println("sid：" + bufferSid.asIntBuffer().get());

        ByteBuffer bufferLid = ByteBuffer.wrap(tokenBytes.fetchExtendPropertyValue("LID"));
        bufferSid.order(ByteOrder.LITTLE_ENDIAN);
        System.out.println("lid：" + new String(bufferLid.array(), "UTF-8"));
    }
    
    @Test
    public void genToken() {
        final boolean productEnv = true;
        long uid = 100650665L;

        YCTokenBuilder tokenBuilder = new YCTokenBuilder(new YCTokenAppSecretProvider() {
            public Map<Short, String> getAppsecret(int appKey) {
                Map<Short, String> m = new HashMap<Short, String>();
                // 传入密钥版本和密码
                if (productEnv) {
                    m.put(INCOME_APP_VERSION, INCOME_APP_SECRET_PROD);
                } else {
                    m.put(INCOME_APP_VERSION, INCOME_APP_SECRET_TEST);
                }

                return m;
            }
        });

        // 计算token有效期,初步设置5分钟过期
        long expireTime = (System.currentTimeMillis() + 60 * 60 * 1000) / 1000;
        // 传入appkey和token有效期
        YCTokenPropertyProvider propProvider = new YCTokenPropertyProvider(INCOME_APP_ID, expireTime);
        // 传入业务需要的具体参数，具体参数以业务为准
        propProvider.addTokenExtendProperty("UID", uid);
        propProvider.addTokenExtendProperty("SID", 0L);
        Base64 coder = new Base64(76, new byte[] {}, true);
        byte[] buildBinanyToken = tokenBuilder.buildBinaryToken(propProvider);
        String token = coder.encodeAsString(buildBinanyToken); // 打印token

        System.out.println("Token: " + token);
    }

}
