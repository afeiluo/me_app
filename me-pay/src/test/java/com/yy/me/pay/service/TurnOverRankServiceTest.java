package com.yy.me.pay.service;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.yy.me.pay.service.TurnOverRankService;

import static org.junit.Assert.*;

/**
 * Created by Chris on 16/4/6.
 */
@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class TurnOverRankServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private TurnOverRankService rankService;

    @Test
    public void testGetUserTodayBeansIncome() {
        long uid = 100001444L;

        try {
            long amount = rankService.getUserTodayBeansIncome(uid);
            System.out.println("user[" + uid + "] today beans income: " + amount);
        } catch (Exception e) {
            fail("Get user today beans income error: " + e.getMessage() );
        }

    }
}
