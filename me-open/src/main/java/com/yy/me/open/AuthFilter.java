package com.yy.me.open;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mongodb.ReadPreference;
import com.yy.cnt.ControlCenterService;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.http.filter.AuthUtil;
import com.yy.me.http.filter.AuthUtil.AuthHelper;
import com.yy.me.http.login.UserLoginUtil;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
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
        messageMongoDBMapper = (MessageMongoDBMapper) context.getBean("messageMongoDBMapper");
        globalProperties = (Properties) context.getBean("settings");
        liveShowThriftService = (ReferenceFactory) context.getBean("liveShowThriftService");
        metricsClient = (MetricsClient) context.getBean("metricsClient");
        GeneralLiveShowClient.initScheduledCal(metricsClient.getAppName(), liveShowThriftService);

        userHessianService = (ReferenceFactory)context.getBean("userHessianService");

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
        au.initByControlCenter(metricsClient, (ControlCenterService) context.getBean("controlCenterService"), productEnv);

        authHelper = new AuthHelper() {

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
                    banStr = messageMongoDBMapper.fm(userInfo.getBanedType(), MessageService.dateFormatter.format(actionTime),
                            MessageService.dateFormatter.format(userInfo.getBanedEndTime()));
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
            public RetMsgObj checkPswTimeAndUpdate(HttpServletRequest request, long uid, UserInfo userInfo) throws Exception {// 需要验证是否更改过密码
                Date changePswDate = getChangePswDate(request);
                if (userInfo.getChangePswTime() != null) {
                    if (changePswDate != null && changePswDate.after(userInfo.getChangePswTime())) {// 这是更新密码过后的新票据
                        userInfo.setChangePswTime(changePswDate);
                        userHessianService.getClient().updatePwChangeTimeByUid(userInfo.getUid(), userInfo.getChangePswTime());// 保存最新的更新密码的时间
                    } else if (changePswDate != null && changePswDate.before(userInfo.getChangePswTime())) {// 说明这个票据不是更新密码之后的最新票据,返回客户端提示失败
                        return BaseServletUtil.genMsgObj(PASSWORD_HAS_CHANGED, "you have changed your psw on other device");
                    } else if (changePswDate == null) {
                        return BaseServletUtil.genMsgObj(PASSWORD_HAS_CHANGED, "you have changed your psw on other device");
                    }
                } else {// 当前的用户信息里面没有记录上次更改密码的时间
                    if (changePswDate != null) {
                        userInfo.setChangePswTime(changePswDate);
                        userHessianService.getClient().updatePwChangeTimeByUid(userInfo.getUid(), userInfo.getChangePswTime());// 保存最新的更新密码的时间
                    }
                }
                return null;
            }

            @Override
            public void afterLoginSuccess(HttpServletRequest request, long uid, UserInfo userInfo) throws Exception {
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
