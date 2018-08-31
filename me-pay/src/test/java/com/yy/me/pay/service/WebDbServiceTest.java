package com.yy.me.pay.service;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.fail;

/**
 * Created by Phil on 17/1/6.
 */
@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class WebDbServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private WebDbService webDbService;

    @Test
    public void testGetYyNick(){
        long imId = 909016041L;
        try{
            String result = webDbService.getYyNick(imId);

            Assert.assertNotNull(result);
        }catch (Exception e){
            fail("getYyNick error: " + e.getMessage());
        }
    }
}
