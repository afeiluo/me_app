package com.yy.me.open.controller;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.util.GeneralUtil.*;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.bs2.Bs2Service;
import com.yy.me.config.GeneralConfService;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.http.HttpUtil;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.open.service.OpenService;
import com.yy.me.open.service.UserInfoService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserInfoUtil;
import com.yy.me.user.UserSource;
import com.yy.me.util.GeneralUtil;

/**
 * 
 * 使用了@RestController的话就不能redirect了
 * 
 */
@RestController
@RequestMapping("/open")
public class OpenApiController {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiController.class);

    @Autowired
    private GeneralConfService generalConfService;

    @Autowired
    private OpenService openService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ReferenceFactory<UserHessianService> userHessianService;

    /**
     * 获取用户数据
     * http://61.147.186.63:8081/open/user/get?appId=100001&sign=&data={"uid":
     * 1000008,"otherUid":1000009}
     * 
     * @param uid
     *            操作人（查看者）uid
     * @param otherUid
     *            被查看用户的个人信息
     * @param response
     */
    @RequestMapping(value = "/user/get")
    public void get(@RequestParam long uid, @RequestParam long otherUid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("userGet", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (uid < 1) {
            logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
            return;
        }

        try {
            RetMsgObj ret = userInfoService.find(uid, otherUid);
            sendResponse(request, response, ret);
        } catch (Exception e) {
            logger.error("Find user error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
        }
    }

    /**
     * 检查用户是否存在，不存在则创建用户，用于UAAS。
     * 
     * @param userinfo
     * @param extraId
     * @param channel
     * @param request
     * @param response
     */
    @RequestMapping(value = "/user/checkAndSet")
    public void userCheckAndSet(UserInfo userinfo, @RequestParam(required = false) String extraId, @RequestParam(required = false) String channel,
            HttpServletRequest request, HttpServletResponse response) {
        if (!(checkAccessAllowed("userCheckAndSet", request))) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
        }
        try {
            long uid = userinfo.getUid();
            UserInfo userInDB = userHessianService.getClient().getUserByUid(uid, false);
            if (userinfo.getUserSource().equals(UserSource.YY.getValue()) && !StringUtils.isBlank(extraId)) {// 理想情况下
                                                                                                             // ME的thirdpartyId为YYUID
                userinfo.setThirdPartyId(extraId);
            }
            if (userInDB != null) {// 说明这个用户已经在ME里面存在了
                sendResponse(request, response, genMsgObj(SUCCESS));
            } else {// 重新生成在ME里面生成一个用户
                String nickName = userinfo.getNick();
                String headUrl = userinfo.getHeaderUrl();
                String username = userInfoService.genUsername(uid);// 生成一个用户名
                userinfo.setUsername(username);
                if (StringUtils.isBlank(nickName)) {
                    userinfo.setNick("我是ME星人");
                }
                if (StringUtils.isBlank(headUrl)) {
                    userinfo.setHeaderUrl(UserInfoUtil.DEFAULT_PIC);
                } else {
                    byte[] bytesArr = Bs2Service.getImageFromNetByUrl(headUrl);
                    if (bytesArr != null) {
                        String subPath = System.currentTimeMillis() + "";
                        String suffix = StringUtils.substringAfterLast(headUrl, ".");
                        String uploadFileName = "serverUpload__" + subPath + "_" + Math.abs(rand.nextInt()) + "." + suffix;
                        String fileUrl = Bs2Service.upload(bytesArr, uploadFileName, "application/octet-stream");
                        userinfo.setHeaderUrl(fileUrl);
                    } else {
                        userinfo.setHeaderUrl(UserInfoUtil.DEFAULT_PIC);
                    }
                }
                String clientType = request.getHeader(BaseServletUtil.HEADER_X_CLIENT);
                String ver = request.getHeader(BaseServletUtil.HEADER_X_CLIENT_VER);
                String pushId = request.getHeader(BaseServletUtil.HEADER_X_PUSH_ID);
                userInfoService.addOrUpdate(userinfo, clientType, ver, pushId, channel, request, response);// 添加改用户
            }
        } catch (Exception e) {
            logger.error("Check and set error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    /**
     * 根据用户ID获取用户个人数据（PCYY）
     * http://tt.yy.com/open/getUserInfo?appId=100001&sign=&data={"username":"carson"}
     * 
     * @param username 用户ID
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getUserInfo")
    public void getUserInfo(@RequestParam String username, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getUserInfo", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (StringUtils.isBlank(username)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(username));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.getUserInfo(username, request, response);
    }

    /**
     * 获取基本个人信息（伟腾红包）
     * http://tt.yy.com/open/getUserBasicInfo?appId=100001&sign=&data={"username":"carson"}
     * 
     * @param username
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getUserBasicInfo")
    public void getUserBasicInfo(@RequestParam String username, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getUserBasicInfo", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (StringUtils.isBlank(username)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(username));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.getUserBasicInfo(username, request, response);
    }

    /**
     * 获取基本个人信息（伟腾红包）
     * http://tt.yy.com/open/getUserBasicInfoByUid?appId=100001&sign=&data={"uid":100001500}
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getUserBasicInfoByUid")
    public void getUserBasicInfoByUid(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getUserBasicInfoByUid", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (uid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.getUserBasicInfoByUid(uid, request, response);
    }

    /**
     * 根据手机号，判断是否为me用户（伟腾红包）
     * http://tt.yy.com/open/checkUserExistByPhone?appId=100001&sign=&data={"phoneNum":"8613312114752"}
     * 
     * @param phoneNum 国内手机号
     * @param request
     * @param response
     * @return 是否已经注册过
     */
    @RequestMapping(value = "/checkUserExistByPhone")
    public void checkUserExistByPhone(@RequestParam String phoneNum, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("checkUserExistByPhone", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (StringUtils.isBlank(phoneNum)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(phoneNum));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.checkUserExistByPhone(phoneNum, request, response);
    }

    /**
     * 提供给运维的侦测接口
     */
    @RequestMapping(value = "/probe")
    public void probe(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("probe", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        openService.probeDB(request, response);
    }


    /**
     * 根据用户ID批量查询用户个人数据(用于后台开放查询页面me.html)
     */
    @SuppressWarnings("serial")
    @RequestMapping(value = "/batchQueryUserInfo")
    public void batchQueryUserInfo(HttpServletRequest request, HttpServletResponse response) {
        String ip = HttpUtil.getRemoteIP(request);

        if (!checkAccessAllowed("batchGetUserBasicInfos", request)) { // 和batchGetUserBasicInfos同样的限制，只是返回jsonp
            sendResponseAuto(request, response, genMsgObj(NOT_AUTH, ip));
            return;
        }
        List<String> otherUsernames = Collections.emptyList();
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("otherUsernames");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    otherUsernames = getLocalObjMapper().convertValue(tmp, new TypeReference<List<String>>() {
                    });
                } else {
                    String msg = "Empty Usernames data in body";
                    logger.warn(msg);
                    sendResponseAuto(request, response,  genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponseAuto(request, response,  genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }

        if ((otherUsernames.isEmpty()) || otherUsernames.size() > 100) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(otherUsernames));
            sendResponseAuto(request, response,  genMsgObj(FAILED, "Size Not Right"));
            return;
        }
        openService.batchGetUserBasicInfos4JsonP(otherUsernames, request, response);
    }

    /**
     * 根据用户ID批量获取用户个人数据（MMS 后台查询用户基本信息，张文伟）
     * http://me.yy.com/open/batchGetUserBasicInfos?appId=100001&sign=&data={"otherUsernames":["carson","carson1"],
     * "otherUids":["200317481"]}
     * 
     * @param request
     * @param response
     */
    @SuppressWarnings("serial")
    @RequestMapping(value = "/batchGetUserBasicInfos")
    public void batchGetUserBasicInfos(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("batchGetUserBasicInfos", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }
        List<String> otherUsernames = Collections.emptyList();
        List<Long> otherUids = Collections.emptyList();
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("otherUsernames");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    otherUsernames = getLocalObjMapper().convertValue(tmp, new TypeReference<List<String>>() {
                    });
                } else {
                    String msg = "Empty Usernames data in body";
                    logger.warn(msg);
                    sendResponse(request, response, genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("otherUids");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    otherUids = getLocalObjMapper().convertValue(tmp, new TypeReference<List<Long>>() {
                    });
                } else {
                    String msg = "Empty Uids data in body";
                    logger.warn(msg);
                    sendResponse(request, response, genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }

        if ((otherUsernames.isEmpty() && otherUids.isEmpty()) || otherUsernames.size() + otherUids.size() > 100) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(otherUsernames));
            sendResponse(request, response, genMsgObj(FAILED, "Size Not Right"));
            return;
        }
        openService.batchGetUserBasicInfos(otherUsernames, otherUids, request, response);
    }


    /**
     * （营收）
     * http://tt.yy.com/open/getUserInfoByUids?appId=100001&sign=&data={"uids":["200032853","200032852"]}
     * 
     * @param uids
     * @param request
     * @param response
     */
    @RequestMapping("/getUserInfoByUids")
    public void getUserInfoByUids(@RequestParam String uids, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getUserInfoByUids", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        List<Long> uidList = Lists.newArrayList();
        String[] uidArray = StringUtils.split(uids, ",");
        for (String uid : uidArray) {
            uidList.add(Long.valueOf(StringUtils.trim(uid)));
        }

        if (uidList == null || uidList.isEmpty()) {
            logger.warn("uid list is empty.");
            sendResponse(request, response, genMsgObj(FAILED, "uids is empty."));
            return;
        }

        openService.findUserByUids(uidList, request, response);
    }

    /**
     * 查询所有正在进行中的直播（都是可以对外展示的直播，即签约主播。欢聚云推荐服务）
     * http://me.yy.com/open/findAllLs?appId=100001&sign=
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/findAllLs")
    public void findAllLs(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("findAllLs", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }
        openService.findAllLs(request, response);
    }

    /**
     * 查询所有正在进行中的直播（所有主播，包括普通用户。欢聚云推荐服务）
     * http://me.yy.com/open/loadTotalLs?appId=100001&sign=
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadTotalLs")
    public void loadTotalLs(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("loadTotalLs", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }
        openService.loadTotalLs(request, response);
    }

    private boolean checkAccessAllowed(String method, HttpServletRequest request) {
        String ip = HttpUtil.getRemoteIP(request);

        Set<String> ips = generalConfService.fetchOpenApiIps(method);

        if (ips == null || (!ips.contains(ip) && !DistIpUtil.isCompanyIp(ip, true, true))) {
            logger.warn("Invalid {} request from remote ip: {}", method, ip);

            return false;
        }

        return true;
    }

    /**
     * 查询直播间详细信息
     * http://me.yy.com/open/getLiveInfoByLid?appId=100001&sign=&data={"lid":56efe1700000672e470a36b5}
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getLiveInfoByLid")
    public void getLiveInfoByLid(@RequestParam String lid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getLiveInfoByLid", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (StringUtils.isBlank(lid)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(lid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.getLiveInfoByLid(lid, request, response);
    }

    /**
     * 查询用户是否在直播间
     * http://me.yy.com/open/checkUserLiveInfo?appId=100001&sign=&data={"uid":100001500,"lid":3333}
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/checkUserLiveInfo")
    public void checkUserLiveInfo(@RequestParam Long uid, @RequestParam String lid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("checkUserLiveInfo", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (uid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        if (StringUtils.isBlank(lid)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(lid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.checkUserLiveInfo(uid, lid, request,response);
    }

    /**
     * 校验token 是否合法，返回uid
     * http://me.yy.com/open/checkUserToken?appId=100001&sign=&data={"token":"xxxxxx"}
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/checkUserToken")
    public void checkUserToken(@RequestParam String token, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("checkUserToken", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (StringUtils.isBlank(token)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(token));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        if ("POST".equals(request.getMethod())) {
            token = URLDecoder.decode(token);// post过来的需要我们给他做urldecode操作
        }
        openService.checkUserToken(token, request, response);
    }

    /**
     * 批量查询用户正在直播中的直播信息
     * http://me.yy.com/open/batchGetLiveshowByUid?appId=100001&sign=&data={"uidList":["200032853","200032852"]}
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/batchGetLiveshowByUid")
    public void batchGetLiveshowByUid(@RequestParam long guestUid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("batchGetLiveshowByUid", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        List<Long> uidList = null;
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("uidList");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    uidList = getLocalObjMapper().convertValue(tmp, new TypeReference<List<Long>>() {
                    });
                } else {
                    String msg = "Empty Uids data in body";
                    logger.warn(msg);
                    sendResponse(request, response, genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }
        if (uidList == null || uidList.isEmpty() || uidList.size() > 1000) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uidList));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.batchGetLiveshowByUid(guestUid, uidList, request, response);
    }

    /**
     * 批量查询用户的直播状态，若在直播，返回lid
     * http://me.yy.com/open/userLiveshowStatus?appId=100001&sign=&data={"uidList":["200032853","200032852"]}
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/userLiveshowStatus")
    public void queryUserLiveshowStatus(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("queryUserLiveshowStatus", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        List<Long> uidList = null;
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("uidList");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    uidList = getLocalObjMapper().convertValue(tmp, new TypeReference<List<Long>>() {
                    });
                } else {
                    String msg = "Empty Uids data in body";
                    logger.warn(msg);
                    sendResponse(request, response, genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }
        if (uidList == null || uidList.isEmpty() || uidList.size() > 1000) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uidList));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.queryUserLiveshowStatus(uidList, request, response);
    }

    /**
     * 批量查询用户的直播状态，若在直播，返回lid (用于me.html)
     * http://me.yy.com/open/userLiveshowStatus?appId=100001&sign=&data={"uidList":["200032853","200032852"]}&callback=
     * callback_func_name
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/userLiveshowStatus4Jsonp")
    public void queryUserLiveshowStatus4Jsonp(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("queryUserLiveshowStatus", request)) {
            sendResponseAuto(request, response, genMsgObj(NOT_AUTH));
            return;
        }

        List<Long> uidList = null;
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("uidList");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    uidList = getLocalObjMapper().convertValue(tmp, new TypeReference<List<Long>>() {
                    });
                } else {
                    String msg = "Empty Uids data in body";
                    logger.warn(msg);
                    sendResponseAuto(request, response,  genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponseAuto(request, response,  genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }
        if (uidList == null || uidList.isEmpty() || uidList.size() > 1000) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uidList));
            sendResponseAuto(request, response,  genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.queryUserLiveshowStatus4JsonP(uidList, request, response);
    }

    /**
     * 获取基本个人信息（伟腾红包）
     * http://tt.yy.com/open/batchGetUserBasicInfoByUid?appId=100001&sign=&data={"uidList":[100001500]}
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/batchGetUserBasicInfoByUid")
    public void batchGetUserBasicInfoByUid(HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("batchGetUserBasicInfoByUid", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }
        List<Long> uidList = null;
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("uidList");
                if (tmp != null && tmp.isArray() && ((ArrayNode)tmp).size() > 0) {
                    uidList = getLocalObjMapper().convertValue(tmp, new TypeReference<List<Long>>() {
                    });
                } else {
                    String msg = "Empty Uids data in body";
                    logger.warn(msg);
                    sendResponse(request, response, genMsgObj(SUCCESS, msg));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }
        if (uidList == null || uidList.isEmpty() || uidList.size() >= 50) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uidList));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.batchGetUserBasicInfoByUid(uidList, request, response);
    }

    /**
     * 头像惩罚
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/headViolate")
    public void headViolate(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("headViolate", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (uid < 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        logger.info("headViolate,uid:{}", uid);
        openService.headViolate(uid, request, response);
    }

    /**
     * 直播间a类b类惩罚
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/lsViolate")
    public void lsAViolate(@RequestParam long uid, @RequestParam int type, @RequestParam String ruleDesc, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("lsViolate", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (uid < 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        logger.info("lsViolate,uid:{},type:{}", uid, type);
        if (type == 1) {
            openService.lsAViolate(uid, ruleDesc, request, response);
        } else if (type == 2) {
            openService.lsBViolate(uid, ruleDesc, request, response);
        }
    }

    /**
     * 检查用户是否被封禁 （目前供联合运营使用，如Bilin（孙佩新））
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/checkUserBan")
    public void checkUserBan(@RequestParam long otherUid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("checkUserBan", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (otherUid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(otherUid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        logger.info("checkUserBan, otherUid:{}", otherUid);
        openService.checkUserBan(otherUid, request, response);
    }

    /**
     * 分页获取签约主播列表,用于冠名红包防刷机制.
     * 
     * @param page
     * @param size
     * @param request
     * @param response
     */
    @RequestMapping(value = "getAnchorList")
    public void getAnchorList(@RequestParam(required = false) Integer page, @RequestParam int size, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("getAnchorList", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));

            return;
        }

        if (size > 1000) {
            logger.warn("Exceed size limit. size: {}, max size: {}", size, 1000);
            sendResponse(request, response, genMsgObj(FAILED, "Size exceed limit."));
            return;
        }

        if (page != null && page < 1) {
            logger.warn("Invalid page: {}", page);
            sendResponse(request, response, genMsgObj(FAILED, "Invalid page."));
            return;
        }

        if (page == null) {
            page = 1;
        }

        openService.getAnchorList(page, size, request, response);
    }

    /**
     * ME uid查询 yy uid (针对是yy号登录的用户)
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "getYYUid")
    public void getYYUid(@RequestParam Long uid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("getYYUid", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (uid == null || uid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
        }
        openService.getYYUid(uid, request, response);
    }

    /**
     * 替换对应水印的白名单
     * 
     * @param uids
     * @param request
     * @param response
     */
    @RequestMapping(value = "replaceWaterMarkWhiteList")
    public void replaceWaterMarkWhiteList(@RequestParam(required = false) Long[] uids, @RequestParam String id, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("replaceWaterMarkWhiteList", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (!ObjectId.isValid(id)) {
            sendResponse(request, response, genMsgObj(NOT_EXIST));
            return;
        }
        if (uids == null) {
            uids = new Long[] {};
        }
        openService.replaceWaterMarkWhiteList(uids, id, request, response);
    }

    /**
     * 添加对应水印的白名单
     * 
     * @param uids
     * @param request
     * @param response
     */
    @RequestMapping(value = "addWaterMarkWhiteList")
    public void addWaterMarkWhiteList(@RequestParam(required = false) Long[] uids, @RequestParam String id, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("replaceWaterMarkWhiteList", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (!ObjectId.isValid(id)) {
            sendResponse(request, response, genMsgObj(NOT_EXIST));
            return;
        }
        if (uids == null) {
            uids = new Long[] {};
        }
        openService.addWaterMarkWhiteList(uids, id, request, response);
    }

    /**
     * 删除对应水印的白名单
     * 
     * @param uids
     * @param request
     * @param response
     */
    @RequestMapping(value = "delWaterMarkWhiteList")
    public void delWaterMarkWhiteList(@RequestParam(required = false) Long[] uids, @RequestParam String id, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("delWaterMarkWhiteList", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        if (!ObjectId.isValid(id)) {
            sendResponse(request, response, genMsgObj(NOT_EXIST));
            return;
        }
        if (uids == null) {
            uids = new Long[] {};
        }
        openService.delWaterMarkWhiteList(uids, id, request, response);
    }

    /**
     * 营收全频道运营广播
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "turnoverOpPush")
    public void turnoverOpPush(@RequestParam long uid, @RequestParam long recvUid, @RequestParam String comment, @RequestParam String img,
            HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("turnoverOpPush", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        openService.turnoverOpPush(uid, recvUid, comment, img, request, response);
    }

    /**
     * 营收频道运营广播
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "turnoverChannelPush")
    public void turnoverChannelPush(@RequestParam long uid, @RequestParam long recvUid, @RequestParam String comment, @RequestParam String img,
            HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("turnoverChannelPush", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        openService.turnoverChannelPush(uid, recvUid, comment, img, request, response);
    }

    @RequestMapping(value = "getMorraList")
    public void getMorraList(@RequestParam long uid, @RequestParam long startTime, @RequestParam long endTime, HttpServletRequest request,
            HttpServletResponse response) {
        if (!checkAccessAllowed("getMorraList", request)) {
            sendResponse(request, response, genMsgObj(NOT_AUTH));
            return;
        }
        openService.getMorraList(uid, startTime, endTime, request, response);
    }
    /**
     * 取消永久禁言
     */
    @RequestMapping(value = "cancelMute")
    public void cancelMute(@RequestParam long anchorUid, @RequestParam long guestUid, HttpServletRequest request, HttpServletResponse response) {
        if (!checkAccessAllowed("cancelMute", request)) {
            sendResponse(request, response,genMsgObj(NOT_AUTH));
            return;
        }
        openService.cancelMute(anchorUid,guestUid,request, response);
    }
}
