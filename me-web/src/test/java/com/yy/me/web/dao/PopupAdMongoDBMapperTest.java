package com.yy.me.web.dao;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.yy.me.web.entity.PopupAd;

import static org.junit.Assert.*;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class PopupAdMongoDBMapperTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private PopupAdMongoDBMapper popupAdMapper;
    
    @Test
    public void testSave() throws Exception {
        PopupAd ad = new PopupAd();
        ad.setTitle("弹窗广告测试");
        ad.setImgUrl("http://ourtimespicture.bs2dl.yy.com/feed_8658630264446821451290924843.jpg");
        ad.setJumpUrl("http://www.yy.com/");
        ad.setStartTime(new Date());
        ad.setEndTime(DateUtils.addMonths(new Date(), 1));
        
        popupAdMapper.save(ad);
    }
    
    @Test
    public void testCheckExists() throws Exception {
        boolean exists = popupAdMapper.checkDurationExists(null, new Date(), DateUtils.addMonths(new Date(), 2));
        assertTrue(exists);
        
        exists = popupAdMapper.checkDurationExists(null, DateUtils.addMonths(new Date(), -1), DateUtils.addDays(new Date(), 1));
        assertTrue(exists);
        
        exists = popupAdMapper.checkDurationExists(null, DateUtils.addMonths(new Date(), 1), DateUtils.addMonths(new Date(), 2));
        assertFalse(exists);
    }

}
