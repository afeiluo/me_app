package com.yy.me.web.service;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class CensorWordServiceTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private CensorWordWebService censorWordService;
    
    @Test
    public void verifyCensorWords() throws Exception {
//        String content = "这是一段正常的文字";
//        String updateContent = censorWordService.replaceCensorWords(content, "*");
//        assertEquals(content, updateContent);
//        
//        content = "脱衣服务是敏感词吗";
//        updateContent = censorWordService.replaceCensorWords(content, "*");
//        assertEquals(updateContent, "****是敏感词吗");
        

        String content2 = "苍井空";
        censorWordService.replaceCensorWords(content2, "*");
    }

}
