package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponse;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_DISTANCE;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_HEADER_URL;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_NICK;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_UID;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.liveshow.pb.Lsdto.LiveShowDtoPb;
import com.yy.me.liveshow.thrift.LsGuestInfo;
import com.yy.me.liveshow.thrift.LsResponse;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.time.MaskClock;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;

@Service
public class ImWebService {

	private static final Logger logger = LoggerFactory
			.getLogger(ImWebService.class);
	@Autowired
	private MetricsClient metricsClient;

	@Autowired
	@Qualifier("userHessianService")
	private ReferenceFactory<UserHessianService> userHessianService;

	@Autowired
	@Qualifier("mongoTemplateUser")
	private MongoTemplate userMongoTemplate;

	@Autowired
	@Qualifier("liveShowThriftService")
	private ReferenceFactory<com.yy.me.liveshow.thrift.LiveShowThriftService> liveShowThriftService;

	/**
	 * 获取IM消息列表
	 * 
	 * @param uid
	 * @param uidList
	 * @param request
	 * @param response
	 */
	public void getImMessageList(@RequestParam long uid,
			@RequestParam List<Long> uidList, HttpServletRequest request,
			HttpServletResponse response) {
		long t = MaskClock.getCurtime();
		int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			UserInfo userInfo = userHessianService.getClient().getUserByUid(
					uid, true);
			List<UserInfo> otherUserInfos = userHessianService.getClient()
					.findUserListByUids(uidList, true);
			ArrayNode resultArray = BaseServletUtil.getLocalObjMapper()
					.createArrayNode();

			for (UserInfo oterUidInfo : otherUserInfos) {
				ObjectNode itemNode = BaseServletUtil.getLocalObjMapper()
						.createObjectNode();
				itemNode.put(FIELD_U_UID, oterUidInfo.getUid());
				itemNode.put(FIELD_U_HEADER_URL, oterUidInfo.getHeaderUrl());
				itemNode.put(FIELD_U_NICK, oterUidInfo.getNick());
				itemNode.put(FIELD_U_DISTANCE,
						getDistance(userInfo, oterUidInfo));
				resultArray.add(itemNode);
			}
			sendResponse(request, response,
					genMsgObj(SUCCESS, null, resultArray));
		} catch (Exception e) {
			logger.error("get uid list error.", e);
			sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
			rescode = MetricsClient.RESCODE_FAIL;
			return;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ImWebService", this.getClass(), "getImMessageList", 1,
					MaskClock.getCurtime() - t, rescode);
		}

	}

	private String getDistance(UserInfo userInfo, UserInfo otherUserInfo) {
		if (userInfo.getLongitude() != null && userInfo.getLatitude() != null
				&& otherUserInfo.getLongitude() != null
				&& otherUserInfo.getLatitude() != null) {
			Double distance = DistIpUtil.calDistance(userInfo.getLongitude(),
					userInfo.getLatitude(), otherUserInfo.getLongitude(),
					otherUserInfo.getLatitude());
			return DistIpUtil.getDisStr(distance);
		}
		return "";
	}

	/**
	 * 是否在直播间超过1分钟
	 * 
	 * @param uid
	 * @param otherUid
	 * @param request
	 * @param response
	 */
	public void isLiveOverOne(long uid, long otherUid, int isTimerPulls,
			HttpServletRequest request, HttpServletResponse response) {
		long t = MaskClock.getCurtime();
		int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			List<LsGuestInfo> lsResps = liveShowThriftService.getClient()
					.findUsersInLs(null, Lists.newArrayList(otherUid));
			for (LsGuestInfo lsResp : lsResps) {
				long enterTime = lsResp.getEnterTime();
				long timeDiff = System.currentTimeMillis() - enterTime;
				if ((isTimerPulls == 1 && timeDiff >= 60 * 1000)
						|| isTimerPulls == 0) {
					sendResponse(request, response,
							genMsgObj(SUCCESS, null, lsResp.getLid()));
					return;
				}
			}
			sendResponse(request, response, genMsgObj(SUCCESS, null, "0"));
		} catch (Exception e) {
			logger.error("get uid list error.", e);
			sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
			rescode = MetricsClient.RESCODE_FAIL;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ImWebService", this.getClass(), "isLiveOverOne", 1,
					MaskClock.getCurtime() - t, rescode);
		}
	}

}
