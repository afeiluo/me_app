package com.yy.me.web.dao;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.yy.me.web.entity.SplashInfo;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class SplashInfoMongoDBMapperTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private SplashInfoMongoDBMapper splashInfoMapper;
    
    @Test
    public void testSave() throws Exception {
        SplashInfo splashInfo = new SplashInfo();
        splashInfo.setTitle("测试闪屏页");
        splashInfo.setImgUrl("http://ourtimespicture.bs2dl.yy.com/feed_8658630264446821451272084535.jpg");
        splashInfo.setStartTime(new Date());
        splashInfo.setEndTime(DateUtils.addMonths(new Date(), 1));
        
        splashInfoMapper.save(splashInfo);
    }

}
