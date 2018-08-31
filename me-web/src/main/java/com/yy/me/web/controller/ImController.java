package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponse;
import static com.yy.me.http.BaseServletUtil.sendResponseAuto;
import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.RET_LS_TOKEN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.friend.entity.FriendRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.yy.me.web.service.FriendService;
import com.yy.me.web.service.ImWebService;
import com.yy.me.yycloud.ApTokenUtils;

/**
 * IM相关请求
 */
@RestController
@RequestMapping("/im")
public class ImController {

	private static Logger logger = LoggerFactory.getLogger(ImController.class);
	@Autowired
	private FriendService friendService;

	@Autowired
	private ImWebService imWebService;

	/**
	 * 生成欢聚云IM登录token
	 * 
	 * @param uid
	 *            用户uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/genImLoginToken")
	public void genImLoginToken(@RequestParam long uid,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			Map<String, Object> ret = Maps.newHashMap();
			String token = ApTokenUtils.genImLoginToken(uid);
			ret.put(RET_LS_TOKEN, token);
			sendResponse(request, response, genMsgObj(SUCCESS, null, ret));
		} catch (Exception e) {
			logger.error("genImLoginToken error.uid:" + uid, e);
			sendResponse(request, response, genMsgObj(FAILED));
		}
	}

	/**
	 * 生成欢聚云IM 1v1聊天token
	 *
	 * @param uid
	 *            用户uid
	 * @param toUid
	 *            对方用户uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/genImMsgToken")
	public void genImMsgToken(@RequestParam long uid, @RequestParam long toUid,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			Map<String, Object> ret = Maps.newHashMap();
			List<Long> list = new ArrayList<>();
			list.add(toUid);
			int relation = friendService.getFriendRelation(uid,
					toUid);
			ret.put("relation", relation);
			if (relation == 2 ) {
				ret.putAll(ApTokenUtils.genImMsgToken(uid, list));
			}
			sendResponse(request, response, genMsgObj(SUCCESS, null, ret));
		} catch (Exception e) {
			logger.error("genImMsgToken error.uid:" + uid, e);
			sendResponse(request, response, genMsgObj(FAILED));
		}
	}

	/**
	 * 获取Im消息列表
	 * 
	 * @param uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/getImMessageList")
	public void getImMessageList(@RequestParam long uid,
			@RequestParam List<Long> uidList, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid <= 0L) {
			logger.warn("Req Param Not Right: " + uid);
			sendResponseAuto(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0L"));
			return;
		}
		imWebService.getImMessageList(uid, uidList, request, response);

	}

	/**
	 * 根据uid获取该用户的直播状态（判断对方是否在直播间待满一分钟）
	 * 
	 * @param uid
	 * @param isTimerPulls
	 *            是否定时拉取 1 是 0 否
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "isLiveOver1")
	public void isLiveOverOne(@RequestParam long uid,
			@RequestParam long otherUid, int isTimerPulls,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid <= 0L) {
			logger.warn("Req Param Not Right: " + uid);
			sendResponseAuto(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0L"));
			return;
		}
		imWebService.isLiveOverOne(uid, otherUid, isTimerPulls, request,
				response);
	}
}
