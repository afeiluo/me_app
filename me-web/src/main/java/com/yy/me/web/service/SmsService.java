package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

import com.yy.cs.base.redis.RedisClientFactory;
import com.yy.me.entity.SmsResponse;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.http.HttpUtil;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.json.JsonUtil;
import com.yy.me.metrics.MetricsClient;

@Service
public class SmsService {

	private static final Logger logger = LoggerFactory
			.getLogger(SmsService.class);

	private static final String ENCODING = "UTF-8";

	private static final String COUNTRY_CODE_CHINA = "86"; // 中国大陆

	private static final String smsAppId = "63";
	private static final String smsAppKey = "sf9123jkl08cn7";
	private static final String smsSendUrl = "http://gossip.sysop.duowan.com:9900";
	private static final String smsSendParams = "appId=%s&appKey=%s&mobile=%s&message=%s&muid=%s";
	private static final int smsCodeExpireTime = 300; // 短信验证码有效时长（s）
	public static final String KEY_SMSCODE = "smscode:%s"; // 短信验证码.
															// smscode:$mobile
	@Autowired
	private RedisClientFactory cntRedisFactory;

	private static String progress = "SmsService";

	@Autowired
	private MetricsClient metricsClient;

	@Autowired
	private UserInfoService userInfoService;
	
	/**
	 * 检查验证码
	 * 
	 * @param mobile
	 * @param smscode
	 * @return
	 */
	private RetMsgObj checkCode(String mobile, String smscode) {
		// 获取主库Jedis实例
		try (Jedis jedis = cntRedisFactory.getMasterPool().getResource()) {
			String smskey = String.format(KEY_SMSCODE, mobile);
			String oldSmscode = jedis.get(smskey);
			// 验证码已过期
			if (StringUtils.isBlank(oldSmscode)) {
				logger.info("the code is invalid");
				return genMsgObj(SMSCODE_INVALID, "the code is invalid");

			}

			// 验证通过
			if (oldSmscode.equals(smscode)) {
				jedis.del(smskey);
				return genMsgObj(SUCCESS);

			} else {
				return genMsgObj(SMSCODE_INVALID, "the code is error");
			}
		} catch (Exception e) {
			logger.error(
					"check smscode has been found some error,mobile:{},smscode:{},e:",
					mobile, smscode, e);
			return genMsgObj(FAILED, "check message is error");
		}

	}

	/**
	 * 检查短信验证码
	 * 
	 * @param mobile
	 * @param smscode
	 * @param request
	 * @param response
	 */
	public void checkMsgSmsCode(String mobile, String smscode,
			HttpServletRequest request, HttpServletResponse response) {
		long start = System.currentTimeMillis();
		int rescode = MetricsClient.RESCODE_SUCCESS;
		try {
			//+ 86
			if(mobile.length()==11){
				if(mobile.startsWith("1")){
					mobile="86"+mobile;
				}else{
					logger.info("the code is invalid");
					sendResponse(request, response,
							genMsgObj(FAILED, "mobile is error"));
					return;
				}
			}
			RetMsgObj retMsgObj = checkCode(mobile, smscode);
			if (retMsgObj.getCode() != SUCCESS) {
				rescode = MetricsClient.RESCODE_FAIL;
			}
			sendResponse(request, response, retMsgObj);
		} catch (Exception e) {
			logger.error(
					"check smscode has been found some error,mobile:{},smscode:{},e:",
					mobile, smscode, e);
			rescode = MetricsClient.RESCODE_FAIL;
			sendResponse(request, response,
					genMsgObj(FAILED, "check message is error"));
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP, progress,
					this.getClass(), "checkMsgSmsCode", 1,
					System.currentTimeMillis() - start, rescode);
		}
	}

	/**
	 * 生成验证码并发送
	 */
	public void makeMsgToSend(String mobile, HttpServletRequest request,
			HttpServletResponse response) {
		// 获取主库Jedis实例
		long start = System.currentTimeMillis();
		String smscode = null;
		int rescode = MetricsClient.RESCODE_SUCCESS;
		try (Jedis jedis = cntRedisFactory.getMasterPool().getResource()) {
			String ip = HttpUtil.getRemoteIP(request);
			//临时取消
//			if (SMSSendTooFrequency(mobile, ip, jedis)) {
//				logger.warn(
//						"the ip {} who is binding the mobile {} that is too frequency",
//						ip, mobile);
//				sendResponse(
//						request,
//						response,
//						genMsgObj(REQUEST_MSG_FREQUENT,
//								"the ip who is binding the mobile that is too frequency"));
//				return;
//			}
			
			// 判定该电话号码是否被绑定或是否绑定超过三个
			Integer openSource = BaseServletUtil.getMeUserSource(request);
			//openSource =6;
			if (openSource == null) {
				logger.warn("The OpenSource Is Null And Perhaps The User Is Not The Thirdpart");
				sendResponse(
						request,
						response,
						genMsgObj(FAILED,
								"The OpenSource Is Null And Perhaps The User Is Not The Thirdpart"));
				return;
			}
			
			
			RetMsgObj retMsgObj = userInfoService.judgeMentMobileAndUid(add86FrontMobile(mobile), openSource);
			if (retMsgObj.getCode() != SUCCESS) {
				rescode = MetricsClient.RESCODE_FAIL;
				sendResponse(request, response, retMsgObj);
				return;
			}
			String smskey = String.format(KEY_SMSCODE, mobile);
			smscode = random(6);
			jedis.setex(smskey, smsCodeExpireTime, smscode);

			/* 发送短信 */
			String sendContent = "[ME]您的短信验证码为code，有效时间五分钟";
			boolean sendStatus = sendToMobile(mobile,
					sendContent.replaceAll("code", smscode));
			if (sendStatus) {
				sendResponse(request, response, genMsgObj(SUCCESS));
			} else {
				sendResponse(request, response, genMsgObj(FAILED));
			}

		} catch (Exception e) {
			rescode = MetricsClient.RESCODE_FAIL;
			logger.error("make message and send message is error", e);
			sendResponse(request, response,
					genMsgObj(FAILED, "make message and send message is error"));
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP, progress,
					this.getClass(), "makeMsgToSend", 1,
					System.currentTimeMillis() - start, rescode);
		}
	}

	/**
	 * 随机生成指定长度的数字串
	 * 
	 * @param length
	 * @return
	 */
	private String random(int length) {
		StringBuilder sb = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < length; i++) {
			sb.append(r.nextInt(10));
		}
		return sb.toString();
	}

	/**
	 * 发送短信
	 * 
	 * @param mobile
	 * @param content
	 * @return
	 */
	private boolean sendToMobile(String mobile, String content) {
		if (StringUtils.isBlank(mobile) || StringUtils.isBlank(content)) {
			logger.warn("Missed required parameters. mobile: {}, content: {}",
					mobile, content);
			return false;
		}
		if(mobile.length()==11){
			if(mobile.startsWith("1")){
				mobile="86"+mobile;
			}else{
				return false;
			}
		}
		if (!isChineseMobile(mobile)) {
			logger.warn("Mobile is invalid: {}", mobile);
			return false;
		}

		mobile = mobile.substring(2);

		try {
			String params = String.format(smsSendParams, smsAppId, smsAppKey,
					mobile, URLEncoder.encode(content, ENCODING),
					System.currentTimeMillis());

			String response = sendData(smsSendUrl, params);
			if (StringUtils.isBlank(response)) {
				logger.error("Fail to send sms to mobile: {}", mobile);
				return false;
			}

			SmsResponse smsResp = JsonUtil.instance.fromJson(response,
					SmsResponse.class);
			logger.info("Send sms to mobile[{}]: {}", mobile, smsResp);

			if (smsResp.getCode() == 0) {
				return true;
			}
		} catch (Exception e) {
			logger.error("[sendToMobile] send sms error.", e);
		}

		return false;
	}

	/**
	 * 检查手机短信接收频率
	 * 
	 * @param mobile
	 * @param ip
	 * @param jedis
	 * @return
	 */
	private Boolean SMSSendTooFrequency(String mobile, String ip, Jedis jedis) {
		// step 1: 检查每一个手机号码接受的频率有没有超出最大次数
		if (jedis.exists("SENDSMSCODE_MOBILE:" + mobile)) {//
			String countValue = jedis.get("SENDSMSCODE_MOBILE:" + mobile);
			if (countValue != null && Integer.parseInt(countValue) > 8) {// 同一个手机号限制一天内发送短信的最大次数为8
				return true;
			} else {
				jedis.incrBy("SENDSMSCODE_MOBILE:" + mobile, 1);
			}
		} else {
			jedis.incrBy("SENDSMSCODE_MOBILE:" + mobile, 1);
			jedis.expire("SENDSMSCODE_MOBILE:" + mobile, 24 * 60 * 60);// 一天
		}
		// setp 2: 检查 每个ip的发送频率有没有超出上限
		if (jedis.exists("SENDSMSCODE_IP:" + ip)) {//
			String countValue = jedis.get("SENDSMSCODE_IP:" + ip);
			if (countValue != null && Integer.parseInt(countValue) > 200) {// 同一个ip限制分钟内最多只能发送200次
				return true;
			} else {
				jedis.incrBy("SENDSMSCODE_IP:" + ip, 1);
			}
		} else {
			jedis.incrBy("SENDSMSCODE_IP:" + ip, 1);
			jedis.expire("SENDSMSCODE_IP:" + ip, 60);// 一分钟
		}
		return false;
	}

	/**
	 * 发送信息
	 * 
	 * @param url
	 * @param parms
	 * @return
	 * @throws Exception
	 */
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
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");

			if (parms != null) {
				OutputStreamWriter out = new OutputStreamWriter(
						conn.getOutputStream(), StandardCharsets.UTF_8);

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
			reader = new BufferedReader(new InputStreamReader(urlStream,
					StandardCharsets.UTF_8));

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
	
	// +86
    private String add86FrontMobile(String mobile) {
        if (mobile.length() == 11) {
            if (mobile.startsWith("1")) {
                mobile = "86" + mobile;
                return mobile;
            } else {
                return null;
            }
        } else if (mobile.length() == 13) {
            if (!mobile.startsWith("86")) {
                return null;
            }
            return mobile;
        } else {
            return null;
        }
    }

}
