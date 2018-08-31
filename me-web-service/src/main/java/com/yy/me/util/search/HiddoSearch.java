package com.yy.me.util.search;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yy.cs.base.http.CSHttpClient;
import com.yy.cs.base.http.CSHttpClientFactory;
import com.yy.me.user.UserInfo;

@Service
public class HiddoSearch {

    private static final Logger logger = LoggerFactory.getLogger(HiddoSearch.class);

    private static CSHttpClientFactory cSHttpClientFactory = new CSHttpClientFactory();
    static {
        cSHttpClientFactory.setSocketTimeOut(10000);
        cSHttpClientFactory.setConnectionTimeout(10000);
        cSHttpClientFactory.setMaxTotal(1000);
        cSHttpClientFactory.setDefaultMaxPerRoute(200);
    }
    private static CSHttpClient cSHttpClient = new CSHttpClient(cSHttpClientFactory);

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    private final static String UPDATE_TINYTIMES = "/tinytimes_user_update?uid=%s&username=%s&nick=%s&signature=%s&pinyinNick=%s&shortPyNick=%s&fansCount=%s&verify=%s&hotRate=%s&lastLiveTime=%s";
    private final static String UPDATE_TINYTIMES_WITHOUTLASTLIVETIME = "/tinytimes_user_update?uid=%s&username=%s&nick=%s&signature=%s&pinyinNick=%s&shortPyNick=%s&fansCount=%s&verify=%s&hotRate=%s";
    private final static String SEARCH_TINYTIMES = "/search?q=%s&uid=%s&app=21&v=1&typ=1&rows=10&start=%s";

    private final static String SEARCH_TOPIC_TINYTIMES = "/search?q=%s&uid=0&app=21&v=1&typ=3";

    @Value("#{settings['hiddo.search.ip_port']}")
    private String hiidoSearchUrl;
    @Value("#{settings['hiddo.update.ip_port']}")
    private String hiidoUpdateUrl;

    /**
     * 当用户更新信息的时候上报海度的查找服务
     * 
     * @param userInfo
     * @param fans 上报的粉丝数
     */
    public void addOrUpdateUserInfo(UserInfo userInfo, long fans, Long lastLiveTime) {
        String updateUrl = null;
        long startTime = System.currentTimeMillis();
        try {
            String username = userInfo.getUsername();
            if (lastLiveTime == null) {// 不上报上次直播的时间
                updateUrl = hiidoUpdateUrl
                        + String.format(UPDATE_TINYTIMES_WITHOUTLASTLIVETIME, userInfo.getUid() + "",
                                StringUtils.isBlank(username) ? "" : URLEncoder.encode(username, "utf-8"),
                                StringUtils.isBlank(userInfo.getNick()) ? "" : URLEncoder.encode(userInfo.getNick(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getSignature()) ? "" : URLEncoder.encode(userInfo.getSignature(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getPinyinNick()) ? "" : URLEncoder.encode(userInfo.getPinyinNick(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getShortPyNick()) ? "" : URLEncoder.encode(userInfo.getShortPyNick(), "utf-8"), fans,
                                userInfo.getVerified() ? "1" : "0", 0);
            } else {
                updateUrl = hiidoUpdateUrl
                        + String.format(UPDATE_TINYTIMES, userInfo.getUid() + "",
                                StringUtils.isBlank(username) ? "" : URLEncoder.encode(username, "utf-8"),
                                StringUtils.isBlank(userInfo.getNick()) ? "" : URLEncoder.encode(userInfo.getNick(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getSignature()) ? "" : URLEncoder.encode(userInfo.getSignature(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getPinyinNick()) ? "" : URLEncoder.encode(userInfo.getPinyinNick(), "utf-8"), StringUtils
                                        .isBlank(userInfo.getShortPyNick()) ? "" : URLEncoder.encode(userInfo.getShortPyNick(), "utf-8"), fans,
                                userInfo.getVerified() ? "1" : "0", 0,
                                lastLiveTime == null ? 0 : lastLiveTime);
            }
            String ret;
            ret = cSHttpClient.doGet(updateUrl);
            if (!ret.equals("OK")) {
                logger.error("update UserInfo to hiido error ret:{}, updateurl:{}", ret, updateUrl);
            }
        } catch (Exception e) {
            logger.error("updateUrl:" + updateUrl + ", " + e.getMessage(), e);
        }
        logger.info("addOrUpdateUserInfo uid {}------cost time {} ms", userInfo.getUid(), System.currentTimeMillis() - startTime);
    }

    /**
     * 
     * @param userInfo
     *            要搜索的用户的uid
     * @param key
     *            搜索的关键字
     * @param imei
     *            手机的标示码
     * @param page
     *            查询的页面
     */
    public List<Long> searchUserInfo(Long uid, String key, String imei, Integer page) {
        List<Long> retList = new ArrayList<Long>();
        long startTime = System.currentTimeMillis();
        String uidStr = StringUtils.isBlank(imei) ? uid + "" : imei;
        Integer startIndex = (page == null) ? 0 : (page - 1) * 10;
        JsonNode root;
        String searchUrl = null;
        try {
            searchUrl = hiidoSearchUrl + String.format(SEARCH_TINYTIMES, URLEncoder.encode(key, "UTF-8"), uidStr, startIndex + "");
            logger.info("searchUrl:{}", searchUrl);
            String ret = cSHttpClient.doGet(searchUrl);
            root = mapper.readTree(ret);
            if (root.get("responseHeader").get("status").asInt() != 0) {
                logger.error("request search interface error responseStatus:{} ", root.get("responseHeader").get("status"));
                return retList;
            }
            JsonNode node = root.get("response").get("1");
            JsonNode docs = node.get("docs");
            Integer total = node.get("numFound").intValue();
            Iterator<JsonNode> it = docs.iterator();
            while (it.hasNext()) {
                JsonNode tmp = it.next();
                Long foundUid = tmp.get("uid").asLong();
                retList.add(foundUid);
            }
        } catch (Exception e) {
            logger.error("searchUrl:" + searchUrl + ", " + e.getMessage(), e);
        }
        logger.info("searchUserInfo uid {}------cost time {} ms", uid, System.currentTimeMillis() - startTime);
        return retList;
    }

    /**
     * 搜索话题
     * 
     * @param key
     * @return
     */
    public List<String> searchTopic(String key) {
        List<String> retList = new ArrayList<String>();
        long startTime = System.currentTimeMillis();
        JsonNode root;
        String searchUrl = null;
        try {
            searchUrl = hiidoSearchUrl + String.format(SEARCH_TOPIC_TINYTIMES, URLEncoder.encode(key, "UTF-8"));
            logger.info("searchUrl:{}", searchUrl);
            String ret = cSHttpClient.doGet(searchUrl);
            root = mapper.readTree(ret);
            if (root.get("responseHeader").get("status").asInt() != 0) {
                logger.error("request search interface error responseStatus:{} ", root.get("responseHeader").get("status"));
                return retList;
            }
            JsonNode node = root.get("response").get("3");
            JsonNode docs = node.get("docs");
            Integer total = node.get("numFound").intValue();
            Iterator<JsonNode> it = docs.iterator();
            while (it.hasNext()) {
                JsonNode tmp = it.next();
                retList.add(tmp.asText());
                // String foundToic = tmp.get("topic").asText();
                // retList.add(foundToic);
            }
        } catch (Exception e) {
            logger.error("searchUrl:" + searchUrl + ", " + e.getMessage(), e);
        }
        logger.info("searchTopic key {}------cost time {} ms", key, System.currentTimeMillis() - startTime);
        return retList;
    }
}
