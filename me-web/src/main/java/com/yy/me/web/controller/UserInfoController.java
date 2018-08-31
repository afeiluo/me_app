package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.HEADER_X_CLIENT;
import static com.yy.me.http.BaseServletUtil.HEADER_X_CLIENT_VER;
import static com.yy.me.http.BaseServletUtil.HEADER_X_PUSH_ID;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponse;
import static com.yy.me.util.GeneralUtil.checkAndFilter;
import static com.yy.me.util.GeneralUtil.checkAndFilterWithoutWrap;
import static com.yy.me.util.GeneralUtil.genMethodParamStr;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.service.inner.ServiceConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.time.MaskClock;
import com.yy.me.user.UserInfo;
import com.yy.me.user.entity.InterestNode;
import com.yy.me.user.entity.MultiInfo;
import com.yy.me.util.GeneralUtil;
import com.yy.me.web.service.UserInfoService;

/**
 * 用户个人数据
 * 
 * @author Jiang Chengyan
 * 
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {

	private static Logger logger = LoggerFactory
			.getLogger(UserInfoController.class);
	private static String progress = "user";
	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private MetricsClient metricsClient;

	/**
	 * 服务端生成可用ID
	 * http://61.147.186.63:8081/user/genUsername?appId=100001&sign=&data
	 * ={"uid":1000008}
	 * 
	 * @param uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/genUsername")
	public void genUsername(@RequestParam long uid, HttpServletRequest request,
			HttpServletResponse response) {
		long t = MaskClock.getCurtime();
		int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			String username = userInfoService.genUsername(uid);
			Map<String, String> jo = Maps.newHashMap();
			jo.put("username", username);
			sendResponse(request, response, genMsgObj(SUCCESS, null, jo));
		} catch (Exception e) {
			rescode = MetricsClient.RESCODE_FAIL;
			logger.error("Gen username error.", e);
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP, progress,
					this.getClass(), "get", 1, MaskClock.getCurtime() - t,
					rescode);
		}
	}

	/**
	 * 新增或更新用户数据
	 * http://61.147.186.63:8081/user/addOrUpdate?appId=100001&sign=&data
	 * ={"uid":1000008,"nick":"test_user","headerUrl"
	 * :"http://www.qq745.com/uploads/allimg/140902/1-140Z2214108.jpg"
	 * ,"verified":true,"sex":1,"searchId":
	 * "55a65e3e02389a3d35731928","signature"
	 * :"I'm ...","userSource":"1","thirdPartyId":"2341245323"}
	 * 
	 * @param userinfo
	 *            用户数据
	 * @param channel
	 *            渠道信息（pcyy）
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/addOrUpdate")
	public void addOrUpdate(UserInfo userinfo,
			@RequestParam(required = false) String channel,
			HttpServletRequest request, HttpServletResponse response) {

		// 原有的逻辑
		userinfo.setNick(checkAndFilterWithoutWrap(userinfo.getNick(), 30));
		userinfo.setSignature(checkAndFilterWithoutWrap(
				userinfo.getSignature(), 100));
		if (userinfo.getUid() == null || userinfo.getUid() < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(userinfo));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right Need More Userinfo"));
			return;
		}
		String clientType = request.getHeader(HEADER_X_CLIENT);
		String ver = request.getHeader(HEADER_X_CLIENT_VER);
		String pushId = request.getHeader(HEADER_X_PUSH_ID);
		userInfoService.addOrUpdate(userinfo, clientType, ver, pushId, channel,
				request, response);
	}

	@RequestMapping(value = "/setInterest")
	public void setInterest(@RequestParam long uid, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid <= 0L) {
			logger.warn("Miss required uid.");
			sendResponse(request, response,
					genMsgObj(FAILED, "Miss required uid."));
			return;
		}
		try {
			List<InterestNode> list = BaseServletUtil.getLocalObjMapper()
					.convertValue(
							((NYYHttpRequestWrapper) request).getDataNode()
									.get("interests"),
							new TypeReference<List<InterestNode>>() {
							});
			// String intrests = request.getParameter("interests");
			// list = BaseServletUtil.getLocalObjMapper().readValue(intrests,
			// new TypeReference<List<InterestNode>>() {
			// });
			userInfoService.setInterest(uid, list, request, response);
		} catch (Exception e) {
			sendResponse(request, response,
					genMsgObj(FAILED, "exception happened:" + e.getMessage()));
			return;
		}

	}

	@RequestMapping(value = "/setAlbum")
	public void setAlbum(@RequestParam long uid, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid <= 0L) {
			logger.warn("Miss required uid.");
			sendResponse(request, response,
					genMsgObj(FAILED, "Miss required uid."));
			return;
		}
		try {
			List<MultiInfo> list = BaseServletUtil.getLocalObjMapper()
					.convertValue(
							((NYYHttpRequestWrapper) request).getDataNode()
									.get("multis"),
							new TypeReference<List<MultiInfo>>() {
							});
			// String intrests = request.getParameter("multis");
			// List<MultiInfo> list =
			// BaseServletUtil.getLocalObjMapper().readValue(intrests, new
			// TypeReference<List<MultiInfo>>() {
			// });
			userInfoService.setMulti(uid, list, request, response);
		} catch (Exception e) {
			sendResponse(request, response,
					genMsgObj(FAILED, "exception happened:" + e.getMessage()));
			return;
		}

	}

	@RequestMapping(value = "/getAlbum")
	public void getAlbum(@RequestParam long uid, @RequestParam long otherUid,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid < 1 || otherUid < 1) {
			logger.warn("Req Param Not Right: "
					+ genMethodParamStr(uid, otherUid));
			sendResponse(
					request,
					response,
					genMsgObj(FAILED,
							"Req Param Not Right: uid <= 0||otherUid<=0"));
			return;
		}
		userInfoService.getMulti(uid, otherUid, request, response);
	}

	@RequestMapping(value = "/getInterestAndMeetInfo")
	public void getInterestAndMeetInfo(@RequestParam long uid,
			@RequestParam long otherUid, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid < 1 || otherUid < 1) {
			logger.warn("Req Param Not Right: "
					+ genMethodParamStr(uid, otherUid));
			sendResponse(
					request,
					response,
					genMsgObj(FAILED,
							"Req Param Not Right: uid <= 0||otherUid<=0"));
			return;
		}
		userInfoService
				.getInterestAndMeetInfo(uid, otherUid, request, response);
	}

	/**
	 * 申请实名认证
	 * http://61.147.186.63:8081/user/applyVerification?appId=100001&sign=
	 * &data={"uid":1000008}
	 * 
	 * @param uid
	 *            用户ID
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/applyVerification")
	public void applyVerification(@RequestParam long uid,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid <= 0L) {
			logger.warn("Miss required uid.");
			sendResponse(request, response,
					genMsgObj(FAILED, "Miss required uid."));
			return;
		}

		userInfoService.applyVerification(uid, request, response);
	}

	@RequestMapping(value = "/accessList")
	public void accessList(@RequestParam long uid, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}
		userInfoService.getAccessList(uid, request, response);
	}

	/**
	 * 获取用户数据 http://61.147.186.63:8081/user/get?appId=100001&sign=&data={"uid":
	 * 1000008,"otherUid":1000009}
	 * 
	 * @param uid
	 *            操作人（查看者）uid
	 * @param otherUid
	 *            被查看用户的个人信息
	 * @param response
	 */
	@RequestMapping(value = "/get")
	public void get(@RequestParam long uid, @RequestParam long otherUid,
			Long lastRequest, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}

		// userInfoService.find(uid, otherUid, request.startAsync());
		long t = MaskClock.getCurtime();
		int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			userInfoService.find(uid, otherUid, lastRequest, request, response);
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP, progress,
					this.getClass(), "get", 1, MaskClock.getCurtime() - t,
					rescode);
		}
	}

	/**
	 * 
	 * @param uid
	 * @param longitude
	 * @param latitude
	 * @param firstTime
	 *            是否是第一次注册后的上报
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/reportLocation")
	public void reportLocation(@RequestParam long uid,
			@RequestParam Double longitude, @RequestParam Double latitude,
			@RequestParam Boolean firstTime,
			@RequestParam(required = false) String provice,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String lid,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}
		userInfoService.reportLocation(uid, longitude, latitude, firstTime,
				provice, city, lid, request, response);
	}

	/**
	 * 判断用户的id是否已经被使用了
	 * http://61.147.186.63:8081/user/get?appId=100001&sign=&data={"uid":
	 * 1000008,"otherUid":1000009}
	 * 
	 * @param uid
	 *            操作人（查看者）uid
	 * @param otherUid
	 *            被查看用户的个人信息
	 * @param response
	 */
	@RequestMapping(value = "/checkUsername")
	public void checkUsername(@RequestParam String id, @RequestParam long uid,
			HttpServletRequest request, HttpServletResponse response) {
		if (StringUtils.isBlank(id)) {
			logger.warn("Need Req param id ");
			sendResponse(request, response,
					genMsgObj(FAILED, "Need Req param id"));
			return;
		}
		userInfoService.checkUsername(id, request, response);
	}

	/**
	 * 获取用户信箱中的所有信件
	 * http://61.147.186.63:8081/user/checkAndFetchMsg?appId=100001&
	 * sign=&data={"uid":1000008}
	 * 
	 * @param uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/checkAndFetchMsg")
	public void checkAndFetchMsg(@RequestParam long uid,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}
		userInfoService.checkAndFetchMsg(uid, request, response);
	}

	/**
	 * 获取目前有效的所有控制消息 http://61.147.186.63:8081/user/fetchCtrlMsg? appId=100001&
	 * sign=& data={"uid":1000008,"pushId":"3jw3c534x104s60fgmt94t6t77wi6ahga"}
	 * 
	 * @param uid
	 * @param pushId
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/fetchCtrlMsg")
	public void fetchCtrlMsg(@RequestParam long uid,
			@RequestParam String pushId, @RequestParam String appId,
			HttpServletRequest request, HttpServletResponse response) {
		String clientStr = request.getHeader(HEADER_X_CLIENT);
		String ver = request.getHeader(HEADER_X_CLIENT_VER);
		if (uid < 1 || StringUtils.isBlank(pushId)) {
			logger.warn("Req Param Not Right: "
					+ genMethodParamStr(uid, pushId));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}

		userInfoService.fetchCtrlMsg(appId, uid, pushId, clientStr, ver,
				request, response);
	}

	/**
	 * 删除指定类型控制消息 http://61.147.186.63:8081/user/deleteCtrlMsg? appId=100001&
	 * sign=& data={"uid":1000008,"msgDataType":"s_op_header_invalid"}
	 * 
	 * @param uid
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/deleteCtrlMsg")
	public void deleteCtrlMsg(@RequestParam long uid,
			@RequestParam String msgDataType, HttpServletRequest request,
			HttpServletResponse response) {
		String clientStr = request.getHeader(HEADER_X_CLIENT);
		String ver = request.getHeader(HEADER_X_CLIENT_VER);
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}

		userInfoService.deleteCtrlMsg(msgDataType, uid, clientStr, ver,
				request, response);
	}

	/**
	 * 分页获取用户看得到的“消息”
	 * http://61.147.186.63:8081/user/findUserMsg?appId=100001&sign
	 * =&data={"uid":1000008,"lastMsgId":
	 * "56414f7f117074f262afa68e","pushId":"3jw3c534x104s60fgmt94t6t77wi6ahga"
	 * ,"msgType":1}
	 * 
	 * @param uid
	 * @param pushId
	 *            推送ID
	 * @param lastMsgId
	 *            上一页拉取的消息ID
	 * @param limit
	 *            （默认20，可不传）
	 * @param msgType
	 *            暂定1是系统消息
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/findUserMsg")
	public void findUserMsg(@RequestParam long uid,
			@RequestParam String pushId,
			@RequestParam(required = false) String lastMsgId,
			@RequestParam(required = false, defaultValue = "20") int limit,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid < 1) {
			logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
			return;
		}
		userInfoService.findUserMsg(uid, pushId, lastMsgId, limit, request,
				response);
	}

	/**
	 * 举报用户
	 * http://61.147.186.63:8081/user/report?appId=100001&sign=&data={"uid":
	 * 1000008,"otherUid":1000009,"reportContent": "test"}
	 * 
	 * @param uid
	 *            举报者ID
	 * @param otherUid
	 *            被举报用户ID
	 * @param reportContent
	 *            举报内容（可为空，可不传）
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/report")
	public void report(@RequestParam long uid, @RequestParam long otherUid,
			@RequestParam(required = false) String reportContent,
			HttpServletRequest request, HttpServletResponse response) {
		reportContent = checkAndFilter(reportContent, 1500);
		if (uid <= 0L || otherUid <= 0) {
			logger.warn("Req Param Not Right: "
					+ GeneralUtil.genMethodParamStr(uid, otherUid,
							reportContent));
			sendResponse(
					request,
					response,
					genMsgObj(
							FAILED,
							"Req Param Not Right: uid <= 0L || otherUid <= 0 || (reportContent != null && reportContent.length() > 300)"));
			return;
		}
		userInfoService.report(uid, otherUid, reportContent, request, response);
	}

	/**
	 * 获取关注列表
	 * http://61.147.186.63:8081/user/relation/getIdols?appId=100001&sign=
	 * &data={"uid":1000009,"fid":1000008,"limit":10}
	 *
	 * @param uid
	 *            操作用户ID
	 * @param lastRid
	 *            上一次查找关注列表的最后一个rid（可为空）
	 * @param fid
	 *            获取关注列表所属的用户id
	 * @param limit
	 *            最大返回大小（此参数可不传，默认是20）
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/relation/getIdols")
	public void getIdols(@RequestParam long uid,
			@RequestParam(required = false) String lastRid,
			@RequestParam long fid,
			@RequestParam(defaultValue = "20") int limit,
			HttpServletRequest request, HttpServletResponse response) {
		if (limit < 1 || limit > 1000) {
			logger.warn("Req Param Not Right: "
					+ GeneralUtil.genMethodParamStr(lastRid, fid, limit));
			sendResponse(
					request,
					response,
					genMsgObj(FAILED, "Req Param Not Right: 1 < pageNum < 1000"));
			return;
		}
		userInfoService.getIdols(uid, fid, lastRid, limit, request, response);
	}

	/**
	 * 获取粉丝列表
	 * http://61.147.186.63:8081/user/relation/getFans?appId=100001&sign=&
	 * data={"uid":1000009,"tid":1000008,"limit":10}
	 * 
	 * @param uid
	 *            操作用户ID
	 * @param lastRid
	 *            上一次查找粉丝列表的最后一个rid（可为空）
	 * @param tid
	 *            获取粉丝列表所属的用户id
	 * @param limit
	 *            最大返回大小（此参数可不传，默认是20）
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/relation/getFans")
	public void getFans(@RequestParam long uid,
			@RequestParam(required = false) String lastRid,
			@RequestParam long tid,
			@RequestParam(defaultValue = "20") int limit,
			HttpServletRequest request, HttpServletResponse response) {
		if (limit < 1 || limit > 100) {
			logger.warn("Req Param Not Right: "
					+ GeneralUtil.genMethodParamStr(lastRid, tid, limit));
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right: 1 < pageNum < 100"));
			return;
		}
		userInfoService.getFans(uid, tid, lastRid, limit, request, response);
	}

	/**
	 * 该uid
	 * 是否被绑定
	 * 
	 * @param uid
	 * @param mobile
	 * @param request
	 * @param response
	 */
	@RequestMapping("/mobileAndUser/isBind")
	public void uidIsBind(@RequestParam long uid, HttpServletRequest request,
			HttpServletResponse response) {
		if (uid <= 0 ) {
			logger.warn("Req Param Not Right: [mobile]:" + ".[uid]:"
					+ uid);
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right"));
			return;
		}
		userInfoService.isBind(uid,request, response);
	}

	/**
	 * 绑定手机
	 * 
	 * @param uid
	 * @param mobile
	 * @param request
	 * @param response
	 */
	@RequestMapping("/mobileAndUser/bindMobile")
	public void bindUid(@RequestParam long uid, @RequestParam String mobile,
			HttpServletRequest request, HttpServletResponse response) {
		if (uid <= 0 || StringUtils.isEmpty(mobile)) {
			logger.warn("Req Param Not Right: [mobile]:" + mobile + ".[uid]:"
					+ uid);
			sendResponse(request, response,
					genMsgObj(FAILED, "Req Param Not Right"));
			return;
		}
		Integer openSource = BaseServletUtil.getMeUserSource(request);
		if (!ServiceConst.productEnv && openSource == null) {//测试环境,没有传token不能从token里获取usersource，所以从用户信息表里查询一下。
			UserInfo user = userInfoService.getUser(uid);
			openSource = user.getUserSource();
		}
		 //openSource="5";
		if (openSource == null) {
			logger.warn("The OpenSource Is Null And Perhaps The User Is Not The Thirdpart");
			sendResponse(
					request,
					response,
					genMsgObj(FAILED,
							"The OpenSource Is Null And Perhaps The User Is Not The Thirdpart"));
			return;
		}
		userInfoService.bindUid(uid, mobile, openSource, request, response);
	}

	/**
	 * 取消绑定手机//TODO 测试用
	 *
	 * @param uid
	 * @param request
	 * @param response
	 */
	@RequestMapping("/mobileAndUser/unBindMobile")
	public void unBindMobile(@RequestParam long uid,
						HttpServletRequest request, HttpServletResponse response) {
		userInfoService.unBindMobile(uid, request, response);
	}

}
