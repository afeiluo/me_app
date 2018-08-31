package com.yy.me.controller;

import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.user.UserHessianService;
import com.yy.me.web.service.FriendService;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * 好友关系controller
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:spring/appContext.xml" })
public class FriendControllerTest {

    @Autowired
    FriendService friendService;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<com.yy.me.friend.FriendService> hessianFactory;

    @Ignore
    @Test
    public void testPullBlack() {
        assertThat(1, is(1));
    }

    @Ignore
    @Test
    public void testGetApplyCount() throws Exception {
        /*Thread[] pool = new Thread[10];
        for (int i = 0; i < 10; i++) {
            pool[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        Long count = friendService.getApplyCount(100001417L, new Date());

                        System.out.println("fac->" + count);
                    }
                }
            });
            pool[i].start();
        }

        for (int i = 0; i < 10; i++) {
            pool[i].join();
        }*/
    }

    @Ignore
    @Test
    public void testGetApplyHeadUrlLimit() throws Exception {
        Long count = friendService.getApplyHeadUrlLimit(101000463L, 100001417L, new Date());
        System.out.println("fhl->" + count);
        count = friendService.incrApplyHeadUrlLimit(101000463L, 100001417L, new Date(), 3);
        System.out.println("fhl->" + count);

        count = friendService.getApplyHeadUrlLimit(101000463L, 100001417L, new Date());
        System.out.println("fhl->" + count);
        count = friendService.incrApplyHeadUrlLimit(101000463L, 100001417L, new Date(), 3);
        System.out.println("fhl->" + count);

        count = friendService.getApplyHeadUrlLimit(101000463L, 100001418L, new Date());
        System.out.println("fhl->" + count);
        count = friendService.incrApplyHeadUrlLimit(101000463L, 100001418L, new Date(), 3);
        System.out.println("fhl->" + count);

        count = friendService.getApplyHeadUrlLimit(101000463L, 100001419L, new Date());
        System.out.println("fhl->" + count);
        count = friendService.incrApplyHeadUrlLimit(101000463L, 100001419L, new Date(), 3);
        System.out.println("fhl->" + count);

        count = friendService.getApplyHeadUrlLimit(101000463L, 100001420L, new Date());
        System.out.println("fhl->" + count);
        count = friendService.incrApplyHeadUrlLimit(101000463L, 100001420L, new Date(), 3);
        System.out.println("fhl->" + count);
    }

    @Ignore
    @Test
    public void testFriendApplyLimit() throws Exception {
        Long uid = 101000868l;
        List<Long> uids = Lists
                .newArrayList(100001414l, 100001416l, 100001410l, 100001418l, 100001408l, 100001421l, 100001407l,
                        100001424l, 100001428l, 100001426l, 100001489l, 100001494l, 100001493l, 100001518l, 100001691l,
                        100001429l, 100001430l, 100001433l, 100001434l, 100001435l, 100001436l, 100001437l, 100001415l,
                        100001438l, 100001439l, 100001440l, 100001411l, 100001413l, 100001441l, 100001442l, 100001444l,
                        100001445l, 100001447l, 100001449l, 100001451l, 100001452l, 100001453l, 100001412l, 100001456l,
                        100001457l, 100001458l, 100001406l, 1000008l, 1000009l, 1000001l, 1000002l, 1000003l, 1000004l,
                        1000005l, 1000006l, 1000007l, 1000010l, 1000011l, 1000012l, 1000013l, 1000014l, 1000015l,
                        100001459l, 100001460l, 100001461l, 100001432l, 100001462l, 100001463l, 100001466l, 100001468l,
                        100001469l, 100001470l, 100001472l, 100001473l, 100001488l, 100001492l, 100001506l, 100001549l,
                        200000001l, 200000017l, 100001635l, 100001646l, 100001474l, 100001475l, 100001476l, 100001477l,
                        100001479l, 100001480l, 100001481l, 100001482l, 100001483l, 100001485l, 100001486l, 100001487l,
                        100001484l, 100001454l, 100001419l, 100001425l, 100001420l, 100001448l, 100001495l, 100001496l,
                        100001497l, 100001498l, 100001501l);

        for (int i = 1; i <= uids.size(); i++) {
//            hessianFactory.getClient().apply(uids.get(i - 1), uid, "我是测试" + i, null);
        }
    }

    @Ignore
    @Test
    public void testRelation() throws Exception {

        int relation = 50;

        int r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(6));

        relation = 34;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(5));

        relation = 18;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(4));

        relation = 8;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(3));

        relation = 4;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(2));

        relation = 2;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(1));

        relation = 1;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(0));

        relation = 0;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(0));

        relation = -1;

        r1 = FriendService.getRelation(relation);
        assertThat(r1, Is.is(0));
    }
}
