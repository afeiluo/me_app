package com.yy.me.pay.filter;

import java.io.IOException;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.yy.cnt.ControlCenterService;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.http.filter.AuthUtil;
import com.yy.me.http.filter.AuthUtil.AuthHelper;
import com.yy.me.http.login.UserLoginUtil;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.service.inner.ServiceConst;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;

/**
 * Created by Chris on 16/3/29.
 */
public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);


    private ReferenceFactory<UserHessianService> userHessianService;

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
        userHessianService = (ReferenceFactory)context.getBean("userHessianService");
        globalProperties = (Properties) context.getBean("settings");
        liveShowThriftService = (ReferenceFactory) context.getBean("liveShowThriftService");
        metricsClient = (MetricsClient) context.getBean("metricsClient");
        GeneralLiveShowClient.initScheduledCal(metricsClient.getAppName(), liveShowThriftService);

        UserLoginUtil.setServiceKey(globalProperties.get("serviceKey") == null ? null : globalProperties.get("serviceKey").toString());

        productEnv = globalProperties.get("node.productEnv") == null ? true : Boolean
                .parseBoolean(globalProperties.get("node.productEnv").toString());
        ServiceConst.productEnv = productEnv;
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
                return null;
            }

            @Override
            public UserInfo findUserInfoByUid(long uid) throws Exception {
                UserInfo userInfo = userHessianService.getClient().getUserByUid(uid,true);
                return userInfo;
            }

            @Override
            public boolean checkBaned(UserInfo userInfo) {
                return false;
            }

            @Override
            public boolean checkAuthNo(HttpServletRequest request) {
                if ("no2".equals(request.getParameter("auth"))) {
                    return true;
                }
                return false;
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
