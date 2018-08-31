package com.yy.me.pay.service;

import com.yy.me.entity.PaymentAccount;
import com.yy.me.enums.PayAccountType;
import com.yy.me.pay.service.SettleService;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class SettleServiceTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private SettleService settleService;
    
    @Test
    public void testWithdraw() throws Exception {
        PaymentAccount payAccount = new PaymentAccount();
        payAccount.setUid(1000008);
        payAccount.setAccount("test@163.com");
        payAccount.setIdCard("4455568787987987915");
        payAccount.setName("Chris");
        payAccount.setType(PayAccountType.ALIPAY.getValue());
        
        int code = settleService.withdraw(payAccount, 100);
        
        System.out.println("response: " + code);
    }

}
