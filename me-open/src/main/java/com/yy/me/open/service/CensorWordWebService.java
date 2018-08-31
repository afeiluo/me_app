package com.yy.me.open.service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.tinytimes.thrift.mms.keywords.Config;
import com.yy.tinytimes.thrift.mms.keywords.KeywordsSearch.Iface;
import com.yy.tinytimes.thrift.mms.keywords.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CensorWordWebService {
    private static final Logger logger = LoggerFactory.getLogger(CensorWordWebService.class);

    public static final String APPID = "999980000";

    @Autowired
    @Qualifier("keyWordsThriftClient")
    private ReferenceFactory<Iface> thriftFactory;

    private Iface getClient() {
        return thriftFactory.getClient();
    }
    
    /**
     * 检查文字内容中是否含有敏感词，如含有则替换为“*”返回.
     * 
     * @param content
     * @return
     */
    public String verifyCensorText(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        try {
            return replaceCensorWords(content, "*");
        } catch (Exception e) {
            logger.error("Verify censor text error, reuse original content: " + content, e);
            return content;
        }
    }
    

    public String replaceCensorWords(String content, String replaceStr) throws Exception {
        try {
            Config keyWordConfig = new Config(true, false, 0);

            Result searchResult = getClient().search(keyWordConfig, content);

            if (searchResult == null) {
                logger.error("Search result is empty!");
                throw new IllegalStateException("Keywords service not available!");
            }

            String errorMsg = searchResult.getMessage();
            if (StringUtils.isNotBlank(errorMsg)) {
                logger.error("Search keywords error: {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (!searchResult.isSuccess()) {
                return content;
            }

            List<String> keywordList = searchResult.getKeywordList();
            String updateContent = content;
            for (String keyword : keywordList) {
                String replacement = StringUtils.repeat(replaceStr, StringUtils.length(keyword));

                updateContent = StringUtils.replace(updateContent, keyword, replacement);
            }
            logger.info("Content[{}] include censor words, replace to: [{}]", content, updateContent);

            return updateContent;
        } catch (Exception e) {
            logger.error("Search keywords encounter error.", e);
            throw e;
        }
    }
    
    /**
     * 检查是否包含敏感词。
     * 
     * @param content
     * @return 不包含敏感词返回true，包含敏感词返回false
     * @throws Exception
     */
    public boolean checkCensorWords(String content) throws Exception {
        try {
            Config keyWordConfig = new Config(true, false, 1);

            Result searchResult = getClient().search(keyWordConfig, content);

            if (searchResult == null) {
                logger.error("Search result is empty!");
                throw new IllegalStateException("Keywords service not available!");
            }

            String errorMsg = searchResult.getMessage();
            if (StringUtils.isNotBlank(errorMsg)) {
                logger.error("Search keywords error: {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (!searchResult.isSuccess()) {
                return true;
            }

            List<String> keywordList = searchResult.getKeywordList();
            logger.info("Content[{}] include censor words: {}", content, StringUtils.join(keywordList, ", "));
            return false;
        } catch (Exception e) {
            logger.error("Check keywords encounter error.", e);
            throw e;
        }
    }

}
