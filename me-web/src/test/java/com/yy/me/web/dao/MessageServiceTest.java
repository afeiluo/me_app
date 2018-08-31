package com.yy.me.web.dao;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.config.GeneralConfService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;

/**
 * 
 * @author Jiang Chengyan
 * 
 */
@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class MessageServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;


    @Autowired
    private GeneralConfService generalConfService;

    @BeforeClass
    public static void init() {
        MongoUtil.resetMachineId(103);
    }

    // @Test
    public void testInsertUserMsg() throws Exception {
    }

    // @Test
    public void testFindUserMsg() throws Exception {
    }

    // @Test
    public void testBroadcastAllMsg() throws Exception {
        messageService.broadcastSysMessage("偷偷发个广播", "结英说要发广播哦", "http://www.pbc.gov.cn/");
        System.out.println();
    }

    @Test
    public void testFetchCtrlMsg() throws Exception {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(90000000L);
        List<ObjectNode> ret = messageMongoDBMapper.checkoutMyRecentlyCtrlMsg(userInfo, "111");
        System.out.println(ret);
    }

}
