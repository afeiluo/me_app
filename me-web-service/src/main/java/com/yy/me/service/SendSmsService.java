package com.yy.me.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.yy.me.entity.SmsResponse;
import com.yy.me.json.JsonUtil;

@Service
public class SendSmsService {
    private static final Logger logger = LoggerFactory.getLogger(SendSmsService.class);

    private static final String ENCODING = "UTF-8";

    private static final String COUNTRY_CODE_CHINA = "86"; // 中国大陆

    private static final String smsAppId = "63";
    private static final String smsAppKey = "sf9123jkl08cn7";
    private static final String smsSendUrl = "http://gossip.sysop.duowan.com:9900";
    private static final String smsSendParams = "appId=%s&appKey=%s&mobile=%s&message=%s&muid=%s";

    public boolean sendToMobile(String mobile, String content) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(content)) {
            logger.warn("Missed required parameters. mobile: {}, content: {}", mobile, content);
            return false;
        }

        if (!isChineseMobile(mobile)) {
            logger.warn("Mobile is invalid: {}", mobile);
            return false;
        }

        mobile = mobile.substring(2);

        try {
            String params = String.format(smsSendParams, smsAppId, smsAppKey, mobile,
                    URLEncoder.encode(content, ENCODING), System.currentTimeMillis());

            String response = sendData(smsSendUrl, params);
            if (StringUtils.isBlank(response)) {
                logger.error("Fail to send sms to mobile: {}", mobile);
                return false;
            }

            SmsResponse smsResp = JsonUtil.instance.fromJson(response, SmsResponse.class);
            logger.info("Send sms to mobile[{}]: {}", mobile, smsResp);

            if (smsResp.getCode() == 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error("[sendToMobile] send sms error.", e);
        }

        return false;
    }

    public boolean sendToMobiles(List<String> mobileList, String content) {
        if (mobileList == null || mobileList.isEmpty() || StringUtils.isBlank(content)) {
            logger.warn("Missed required parameters. mobile: {}, content: {}", mobileList, content);
            return false;
        }

        List<String> validMobileList = Lists.newArrayList();
        for (String mobile : mobileList) {
            if (isChineseMobile(mobile)) {
                validMobileList.add(mobile.substring(2));
            }
        }

        if (validMobileList.isEmpty()) {
            logger.warn("No valid mobile: {}", mobileList);
            return false;
        }

        String mobiles = StringUtils.join(validMobileList, ",");

        try {
            String params = String.format(smsSendParams, smsAppId, smsAppKey, mobiles,
                    URLEncoder.encode(content, ENCODING), System.currentTimeMillis());

            String response = sendData(smsSendUrl, params);
            if (StringUtils.isBlank(response)) {
                logger.error("Fail to send sms to mobile: {}", mobiles);
                return false;
            }

            SmsResponse smsResp = JsonUtil.instance.fromJson(response, SmsResponse.class);
            logger.info("Send sms to mobiles: {}", smsResp);

            if (smsResp.getCode() == 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error("[sendToMobiles] send sms error.", e);
        }

        return false;
    }

    private String sendData(String url, String parms) throws Exception {
        HttpURLConnection conn = null;
        InputStream urlStream = null;
        BufferedReader reader = null;

        try {
            URL requestUrl = new URL(url);
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            if (parms != null) {
                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);

                try {
                    out.write(parms);
                    out.flush();
                } catch (Exception e) {
                    logger.error("Write parameters error.", e);
                } finally {
                    try {
                        out.close();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }

            urlStream = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(urlStream, StandardCharsets.UTF_8));

            StringBuffer response = new StringBuffer();
            String line = null;
            do {
                line = reader.readLine();
                if (line != null) {
                    response.append(line).append('\n');
                }
            } while (line != null);

            return response.toString();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }

                if (urlStream != null) {
                    urlStream.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
    }

    /**
     * 检查是否为中国大陆的手机号码
     * 
     * @param mobile
     * @return
     */
    private boolean isChineseMobile(String mobile) {
        if (mobile.startsWith(COUNTRY_CODE_CHINA) && mobile.length() == 13) {
            return true;
        }
        return false;
    }

}
