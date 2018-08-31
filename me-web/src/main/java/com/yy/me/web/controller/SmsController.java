package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.metrics.MetricsClient.ProtocolType;
import com.yy.me.time.MaskClock;
import com.yy.me.web.service.SmsService;

import redis.clients.jedis.Jedis;

@RestController
@RequestMapping("/sms/")
public class SmsController {
	private static Logger logger = LoggerFactory.getLogger(SmsController.class);

	private static String REGEX_MOBILE = "^\\d+$"; // 为了扩展性，这里只做简单验证，手机号格式严格检查由前端控制

	@Autowired
	private MetricsClient metricsClient;

	@Autowired
	private SmsService smsService;

	/**
	 * 获取验证码绑定手机号
	 * 
	 * @param appId
	 *            业务ID
	 * @param mobile
	 *            手机号码
	 * @param oper_type
	 *            操作类型. 0-注册, 1-忘记密码
	 * @return
	 */
	@RequestMapping("/bind/get")
	public void bindGetMsg(long uid, String mobile, HttpServletRequest request,
			HttpServletResponse response) {
		String inputParams = String.format("appId=%s, mobile=%s", mobile, uid);
		logger.info("[generate] inputParams: {}", inputParams);

		/* 检查参数 */
		RetMsgObj resultCode = checkGenerateParams(mobile);

		if (resultCode.getCode() != SUCCESS) {
			logger.info("the mobile format is error, mobile-{}", mobile);
			sendResponse(request, response, resultCode);
			return;
		}
		smsService.makeMsgToSend(mobile, request, response);

	}

	/**
	 * 校验验证码
	 * 
	 * @param appId
	 *            业务ID
	 * @param mobile
	 *            手机号码
	 * @param oper_type
	 *            操作类型. 0-注册, 1-忘记密码
	 * @param smscode
	 *            短信验证码
	 * @return
	 */
	@RequestMapping("/bind/check")
	public void check(String mobile, String smscode,
			HttpServletRequest request, HttpServletResponse response) {

		String inputParams = String.format(" mobile=%s, smscode=%s", mobile,
				smscode);
		logger.info("[check] inputParams: {}", inputParams);
		long start = System.currentTimeMillis();

		if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(smscode)) {
			logger.info("the mobile format is error, mobile-{}", mobile);
			sendResponse(request, response, genMsgObj(FAILED));
		}
		smsService.checkMsgSmsCode(mobile, smscode, request, response);

	}

	/**
	 * 检查电话号码格式，简单检查，重点还是在客户端检查
	 * 
	 * @param mobile
	 * @return
	 */
	private RetMsgObj checkGenerateParams(String mobile) {
		if (!isMobile(mobile)) {
			return genMsgObj(MOBILE_FORMAT_ERROR, "the mobile format is error!");
		}
		return genMsgObj(SUCCESS);
	}

	/**
	 * 手机号码验证
	 * 
	 * @param input
	 * @return
	 */
	private boolean isMobile(String input) {
		if (StringUtils.isBlank(input))
			return false;
		return Pattern.matches(REGEX_MOBILE, input);
	}
}
