package com.yy.me.web;

import static com.yy.me.http.BaseServletUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.time.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mongodb.ReadPreference;
import com.yy.cnt.ControlCenterService;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.enums.ViolationMsgType;
import com.yy.me.http.filter.AuthUtil;
import com.yy.me.http.filter.AuthUtil.AuthHelper;
import com.yy.me.http.filter.RuleDesc;
import com.yy.me.http.login.UserLoginUtil;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.mq.LsBroadcastAloProducer;
import com.yy.me.liveshow.client.util.LiveShowServletUtil;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.liveshow.thrift.LsRequestHeader;
import com.yy.me.log.ElkLogger;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.service.inner.MessageService;
import com.yy.me.service.inner.ServiceConst;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;

public class AuthFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static ElkLogger elkLogger = ElkLogger.getLogger(AuthFilter.class);

    private LsBroadcastAloProducer lsBroadcastAloProducer;
    private MessageService messageService;
    private ReferenceFactory<UserHessianService> userHessianService;
    private MessageMongoDBMapper messageMongoDBMapper;

    private Properties globalProperties;

    private boolean productEnv;

    private AuthUtil au;

    private AuthHelper authHelper;

    private MetricsClient metricsClient;

    private ReferenceFactory<LiveShowThriftService> liveShowThriftService;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println(AuthFilter.class.getCanonicalName() + " Filter is initted!");
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        lsBroadcastAloProducer = (LsBroadcastAloProducer) context.getBean("lsBroadcastAloProducer");
        messageService = (MessageService) context.getBean("messageService");
        userHessianService = (ReferenceFactory)context.getBean("userHessianService");
        messageMongoDBMapper = (MessageMongoDBMapper) context.getBean("messageMongoDBMapper");
        globalProperties = (Properties) context.getBean("settings");
        liveShowThriftService = (ReferenceFactory) context.getBean("liveShowThriftService");
        metricsClient = (MetricsClient) context.getBean("metricsClient");
        ControlCenterService cnt = (ControlCenterService) context.getBean("controlCenterService");
        GeneralLiveShowClient.initScheduledCal(metricsClient.getAppName(), liveShowThriftService);

        UserLoginUtil.setServiceKey(globalProperties.get("serviceKey") == null ? null : globalProperties.get("serviceKey").toString());
        productEnv = globalProperties.get("node.productEnv") == null ? true : Boolean
                .parseBoolean(globalProperties.get("node.productEnv").toString());
        MongoUtil.resetMachineId(globalProperties.get("node.serverId") == null ? null : Integer.parseInt(globalProperties.get("node.serverId")
                .toString()));
        ServiceConst.productEnv = productEnv;
        if (!productEnv) {
            ServiceConst.closeLink = true;
        }

        // MongoDB Tag
        ReadPreference readPreference = (ReadPreference) context.getBean("readPreference");
        // MongoTemplate mongoTemplate = (MongoTemplate) context.getBean("mongoTemplate");// 此库由于机器数量不足，暂时不用tag
        // mongoTemplate.getDb().setReadPreference(readPreference);
        MongoTemplate mongoTemplateLiveshow = (MongoTemplate) context.getBean("mongoTemplateLiveshow");
        mongoTemplateLiveshow.getDb().setReadPreference(readPreference);
        MongoTemplate mongoTemplateUser = (MongoTemplate) context.getBean("mongoTemplateUser");
        mongoTemplateUser.getDb().setReadPreference(readPreference);
        MongoTemplate mongoTemplateConfig = (MongoTemplate) context.getBean("mongoTemplateConfig");
        mongoTemplateConfig.getDb().setReadPreference(readPreference);

        au = new AuthUtil();
        au.initByControlCenter(metricsClient, cnt, productEnv);

        authHelper = new AuthHelper() {
            @Override
            public RuleDesc genRuleDesc(UserInfo userInfo) {
                RuleDesc ruleDesc = new RuleDesc();
                if (userInfo.getBaned() instanceof Boolean && userInfo.getBaned()) {
                    if (StringUtils.isBlank(userInfo.getBanedSubItem())) {// 为空就直接返回null
                        return null;
                    }
                    if (userInfo.getBanedType().equals(ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2.getValue())) {
                        ruleDesc.setDesc(userInfo.getBanedDesc());
                        ruleDesc.setItem(userInfo.getBanedItem());
                        ruleDesc.setSubItem(userInfo.getBanedSubItem());
                        return ruleDesc;
                    }
                }
                return null;
            }

            @Override
            public void resetParameter(HttpServletRequest request, String key, String value) {
                NYYHttpRequestWrapper nyyReq = (NYYHttpRequestWrapper) request;
                nyyReq.setParameter(key, value);
            }

            @Override
            public String genBanedStr(HttpServletRequest request, UserInfo userInfo) {
                String banStr = null;
                if (userInfo.getBaned() instanceof Boolean && userInfo.getBaned()) {
                    Date actionTime = userInfo.getBanedActionTime();
                    if (actionTime == null) {
                        actionTime = userInfo.getBanedEndTime();
                    }
                    if (userInfo.getBanedType().equals(ViolationMsgType.U_BANED_TIPS_LS_ACCOUNT_2.getValue())) {
                        String item = userInfo.getBanedItem();
                        String subItem = userInfo.getBanedSubItem();
                        String desc = userInfo.getBanedDesc();
                        banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), MessageService.dateFormatter.format(actionTime),
                                MessageService.dateFormatter.format(userInfo.getBanedEndTime()), null, item, subItem, desc);
                    } else if (userInfo.getBanedType().equals(ViolationMsgType.U_BANED_REPORT_FIRST_FORBID.getValue())) {//一级封禁
                        String desc = userInfo.getBanedDesc();
                        Date banedEndTime = userInfo.getBanedEndTime();
                        if (banedEndTime == null) {
                            UserInfo paramUserInfo = new UserInfo();
                            paramUserInfo.setUid(userInfo.getUid());
                            banedEndTime = DateTimeUtil.addHours(new Date(), 1);
                            paramUserInfo.setBanedEndTime(banedEndTime);
                            userHessianService.getClient().updateByUidAndInvalidCache(paramUserInfo);
                        }
                        banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), desc, DateTimeUtil.formatCompactDateTime2(banedEndTime));
                    } else if (userInfo.getBanedType().equals(ViolationMsgType.U_BANED_REPORT_SECOND_FORBID.getValue())) {//二级封禁
                        String desc = userInfo.getBanedDesc();
                        banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), desc);
                    } else {
                        banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), MessageService.dateFormatter.format(actionTime),
                                MessageService.dateFormatter.format(userInfo.getBanedEndTime()));
                    }
                }
                return banStr;
            }

            @Override
            public UserInfo findUserInfoByUid(long uid) throws Exception {
                UserInfo userInfo = null;
                userInfo = userHessianService.getClient().getUserByUid(uid,true);
                return userInfo;
            }

            @Override
            public boolean checkBaned(UserInfo userInfo) {
                if (userInfo.getBaned() instanceof Boolean && userInfo.getBaned()) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean checkAuthNo(HttpServletRequest request) {
                if ("no2".equals(request.getParameter("auth"))) {
                    return true;
                }
                return false;
            }
            
            @Override
            public void updateUserCheckPswTime(long uid, UserInfo userInfo, Date changePswDate) throws Exception {
                userInfo.setChangePswTime(changePswDate);
                userHessianService.getClient().updatePwChangeTimeByUid(userInfo.getUid(), userInfo.getChangePswTime());// 保存最新的更新密码的时间
            }

            @Override
            public void afterLoginSuccess(HttpServletRequest request, long uid, UserInfo userInfo) throws Exception {
                String userSource = getUaasUserSource(request);
                String account = getUserAccount(request);
                boolean hasUpdate =  userHessianService.getClient().updateUserSourceAndThirdPartyId(userSource,account,userInfo);// 将uaas的usersource和account信息更新到userInfo中去
                try {
                    LsRequestHeader lsRequestHeader = LiveShowServletUtil.genHeader(request);
                    logger.info("LsRequestHeader:{}", lsRequestHeader);
                    elkLogger.write("web_tt_after_login_success", lsRequestHeader.toString());
                } catch (Exception e) {
                    logger.error("AuthFilter error:{}", e.getMessage(), e);
                }
            }
        };
        logger.info(AuthFilter.class.getCanonicalName() + " Filter is initted!");
    }

    @Override
    public void destroy() {
        logger.info(AuthFilter.class.getCanonicalName() + " Filter is destroyed!");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        au.doFilter(req, resp, chain, authHelper);
    }
}
