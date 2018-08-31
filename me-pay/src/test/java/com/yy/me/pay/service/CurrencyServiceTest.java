package com.yy.me.pay.service;

import com.yy.me.pay.entity.ExchangeConfig;
import com.yy.me.pay.entity.UserExchangeHistory;
import com.yy.me.pay.entity.UserIncomeHistory;
import com.yy.me.pay.entity.UserWithdrawHistory;
import com.yy.me.pay.service.CurrencyService;
import com.yy.tinytimes.thrift.turnover.currency.TCurrencyType;
import com.yy.tinytimes.thrift.turnover.currency.UsedChannelType;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Chris on 16/4/6.
 */
@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class CurrencyServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private CurrencyService currencyService;

    @Test
    public void testGetUserAccountInfo() {
        long uid = 100001444L;
        try {
            Map<TCurrencyType, Long> accountMap = currencyService.getUserAccountInfo(uid);

            System.out.println("User[" + uid + "] account: ");
            for (Map.Entry<TCurrencyType, Long> entry : accountMap.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (Exception e) {
            fail("Get user account info error: "  + e.getMessage());
        }
    }

    @Test
    public void testQueryIncomeHistory() {
        long uid = 100001444L;
        try {
            List<UserIncomeHistory> historyList = currencyService.queryIncomeHistory(uid, 10, 0);

            System.out.println("User[" + uid + "] income history: " + StringUtils.join(historyList, ", "));
        } catch (Exception e) {
            fail("Query income history error: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWithdrawHistory() {
        long uid = 100001444L;
        try {
            List<UserWithdrawHistory> historyList = currencyService.queryWithdrawHistory(uid, 10, 0);

            System.out.println("User[" + uid + "] withdraw history: " + StringUtils.join(historyList, ", "));
        } catch (Exception e) {
            fail("Query withdraw history error: " + e.getMessage());
        }
    }

    @Test
    public void testQueryExchangeHistory() {
        long uid = 100001444L;
        try {
            List<UserExchangeHistory> historyList = currencyService.queryExchangeHistory(uid, 10, 0);

            System.out.println("User[" + uid + "] exchange history: " + StringUtils.join(historyList, ", "));
        } catch (Exception e) {
            fail("Query exchange history error: " + e.getMessage());
        }
    }

    @Test
    public void testQueryExchangeConfigList() {
        try {
            List<ExchangeConfig> configList = currencyService.queryExchangeConfigList();

            System.out.println("Exchange config: " + StringUtils.join(configList, ", "));
        } catch (Exception e) {
            fail("Query exchange config list error: " + e.getMessage());
        }
    }

    @Test
    public void testExchange() {
        long uid = 100001444L;
        try {
            boolean result = currencyService.exchange(uid, 1, "0.0.0.0", UsedChannelType.WEB);

            assertTrue(result);
        } catch (Exception e) {
            fail("Exchange error: " + e.getMessage());
        }
    }

    @Test
    public void testM2y(){
        long uid = 201000883L;
        try{
            int result = currencyService.m2y("000001", uid, 10, "测试兑换Y币");

            Assert.assertThat(result, Is.is(1));
        }catch (Exception e){
            fail("m2y error: " + e.getMessage());
        }
    }
}
