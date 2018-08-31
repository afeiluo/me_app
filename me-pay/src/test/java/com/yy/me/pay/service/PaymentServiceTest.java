package com.yy.me.pay.service;

import static com.yy.me.http.BaseServletUtil.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.me.pay.entity.GiftBagRecord;
import com.yy.me.pay.entity.GiftCallbackReq;
import com.yy.me.pay.entity.GiftCallbackReq.CurrencyType;
import com.yy.me.pay.entity.GiftCallbackReq.PlatformInfo;
import com.yy.me.pay.entity.GiftCallbackReq.UseInfo;
import com.yy.me.service.inner.ServiceConst;
import com.yy.me.thread.ThreadUtil;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class PaymentServiceTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private SendGiftService paymentService;
    
    @Autowired
    private PaymentWebService paymentWebService;
    
    @Test
    public void testReceiveGiftBag() throws Exception {
        GiftCallbackReq giftReq = new GiftCallbackReq();
        giftReq.setSeq("1000018");
        giftReq.setUid(200000010);
        giftReq.setRecvUid(100001444);
        giftReq.setUsedTimestamp(System.currentTimeMillis());
        
        UseInfo useInfo = new UseInfo();
        useInfo.setPropId(210001);
        useInfo.setCurrencyType(CurrencyType.MIBI.getValue());
        useInfo.setAmount(10);
        useInfo.setPropCount(10);
        useInfo.setIncome(5);
        giftReq.setUseInfos(Lists.newArrayList(useInfo));
        
        Map<String, Object> expand = Maps.newHashMap();
        expand.put("lid", "56a1d5ed00006734191602af");
        giftReq.setExpand(getLocalObjMapper().writeValueAsString(expand));
        
        giftReq.setPlatform(PlatformInfo.Android.getValue());
        
        
        paymentWebService.receiveGiftBag(giftReq, null, null);
    }
    
    @Test
    public void testBatchReceiveGiftBag() {
        final String lid = "56effc370000c95d035febab";
        final long recvUid = 100001444L;
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String comboId = Long.toString(System.currentTimeMillis());
                long seq = 1000001;
                for (int i = 0; i < 100; i++) {
                    GiftCallbackReq giftReq = new GiftCallbackReq();
                    giftReq.setSeq(Long.toString(seq++));
                    giftReq.setUid(100002181);
                    giftReq.setRecvUid(recvUid);
                    giftReq.setUsedTimestamp(System.currentTimeMillis());

                    UseInfo useInfo = new UseInfo();
                    useInfo.setPropId(210001);
                    useInfo.setCurrencyType(CurrencyType.MIBI.getValue());
                    useInfo.setAmount(10);
                    useInfo.setPropCount(1);
                    useInfo.setIncome(5);
                    giftReq.setUseInfos(Lists.newArrayList(useInfo));

                    Map<String, Object> expand = Maps.newHashMap();
                    expand.put(ServiceConst.GIFT_EXPAND_KEY_LID, lid);
                    expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, comboId);
                    try {
                        giftReq.setExpand(getLocalObjMapper().writeValueAsString(expand));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }

                    giftReq.setPlatform(PlatformInfo.Android.getValue());
                    
                    if (i % 10 == 0) {
                        ThreadUtil.sleep(2000);
                    }

                    paymentWebService.receiveGiftBag(giftReq, null, null);
                }
            }
        });
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(2000);
                
                String comboId = Long.toString(System.currentTimeMillis());
                long seq = 1000001;
                for (int i = 0; i < 100; i++) {
                    GiftCallbackReq giftReq = new GiftCallbackReq();
                    giftReq.setSeq(Long.toString(seq++));
                    giftReq.setUid(100001712);
                    giftReq.setRecvUid(recvUid);
                    giftReq.setUsedTimestamp(System.currentTimeMillis());

                    UseInfo useInfo = new UseInfo();
                    useInfo.setPropId(210001);
                    useInfo.setCurrencyType(CurrencyType.MIBI.getValue());
                    useInfo.setAmount(10);
                    useInfo.setPropCount(1);
                    useInfo.setIncome(5);
                    giftReq.setUseInfos(Lists.newArrayList(useInfo));

                    Map<String, Object> expand = Maps.newHashMap();
                    expand.put(ServiceConst.GIFT_EXPAND_KEY_LID, lid);
                    expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, comboId);
                    try {
                        giftReq.setExpand(getLocalObjMapper().writeValueAsString(expand));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }

                    giftReq.setPlatform(PlatformInfo.Android.getValue());
                    
                    if (i % 10 == 0) {
                        ThreadUtil.sleep(2000);
                    }

                    paymentWebService.receiveGiftBag(giftReq, null, null);
                }
            }
        });
        
        ThreadUtil.sleep(3, TimeUnit.MINUTES);
    }
    
    @Test
    public void testGiftBroadcast() throws Exception {
        String lid = "56eb9b5b0000c91dc6aded4c";
        
        GiftBagRecord giftBag = new GiftBagRecord();
        giftBag.setSeq("1000001");
        giftBag.setUid(100001444);
        giftBag.setRecvUid(100001768);
        giftBag.setUsedTime(new Date(System.currentTimeMillis()));
        giftBag.setLid(lid);
        giftBag.setPropId(210001);
        giftBag.setPropCount(3);
        giftBag.setIncome(30);

        Map<String, Object> expand = Maps.newHashMap();
        expand.put(ServiceConst.GIFT_EXPAND_KEY_LID, lid);
        expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, String.valueOf(System.currentTimeMillis()));
        expand.put(ServiceConst.GIFT_EXPAND_KEY_ROBOT, true);
        giftBag.setExpand(getLocalObjMapper().writeValueAsString(expand));

        try {
            paymentService.sendGiftBroadcast(giftBag);
        } catch (Exception e) {
            e.printStackTrace();
            fail("gift broadcast fail.");
        }
    }
    
    @Test
    public void testGiftBatchBroadcast() throws Exception {
        List<GiftBagRecord> giftBagList = Lists.newArrayList();

        Random random = new Random();

        String lid = "56ed1b890000c975bc276973";
        List<Long> uidList = Lists.newArrayList(100001444L, 100002648L, 100002647L);
        List<Integer> propIdList = Lists.newArrayList(210001, 210002, 210003, 210010, 210011, 210012, 210000);

        long seq = 1000001;
        for (int i = 0; i < 5; i++) {
            GiftBagRecord giftBag = new GiftBagRecord();
            giftBag.setSeq(Long.toString(seq++));
            giftBag.setUid(uidList.get(random.nextInt(3)));
            giftBag.setRecvUid(100001737);
            giftBag.setUsedTime(new Date(System.currentTimeMillis()));
            giftBag.setLid(lid);
            
            int propCount = random.nextInt(5);

            giftBag.setPropCount(propCount);
            giftBag.setIncome(10 * propCount);

            int propId = propIdList.get(random.nextInt(7));
            giftBag.setPropId(propId);

            Map<String, Object> expand = Maps.newHashMap();
            expand.put(ServiceConst.GIFT_EXPAND_KEY_LID, lid);
            expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, String.valueOf(System.currentTimeMillis()));

            if (propId == 210000) {
                expand.put("barrage", "haha, test");
                expand.put("sex", 1);
            }

            giftBag.setExpand(getLocalObjMapper().writeValueAsString(expand));

            giftBagList.add(giftBag);
        }

        try {
            paymentService.batchSendGiftBroadcast(lid, giftBagList);
        } catch (Exception e) {
            e.printStackTrace();
            fail("gift batch broadcast fail.");
        }
    }
    
    @Test
    public void testComboGiftBroadcast() throws Exception {
        String comboId = "423425235jijiojio";

        for (int i = 0; i < 20; i++) {
            GiftBagRecord giftBag = new GiftBagRecord();
            giftBag.setSeq("1000001");
            giftBag.setUid(100001444);
            giftBag.setRecvUid(100001562);
            giftBag.setUsedTime(new Date(System.currentTimeMillis()));
            giftBag.setLid("56a737d0000067069f113eb6");
            giftBag.setPropId(210001);
            giftBag.setPropCount(1);
            giftBag.setIncome(10);

            Map<String, Object> expand = Maps.newHashMap();
            expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, comboId);
            expand.put(ServiceConst.GIFT_EXPAND_KEY_ROBOT, true);
            giftBag.setExpand(getLocalObjMapper().writeValueAsString(expand));

            try {
                paymentService.sendGiftBroadcast(giftBag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void testComboGiftBatchBroadcast() throws Exception {
        List<GiftBagRecord> giftBagList = Lists.newArrayList();

        String lid = "56cfd91b00006765299ddac1";
        String comboId = "423425235jijiojio";

        long seq = 1000001;
        for (int i = 0; i < 10; i++) {
            GiftBagRecord giftBag = new GiftBagRecord();
            giftBag.setSeq(Long.toString(seq++));
            giftBag.setUid(100001444L);
            giftBag.setRecvUid(100001448);
            giftBag.setUsedTime(new Date(System.currentTimeMillis()));
            giftBag.setLid(lid);
            giftBag.setPropId(210001);
            giftBag.setPropCount(1);
            giftBag.setIncome(10);

            Map<String, Object> expand = Maps.newHashMap();
            expand.put(ServiceConst.GIFT_EXPAND_KEY_LID, lid);
            expand.put(ServiceConst.GIFT_EXPAND_KEY_COMBO_ID, comboId);

            giftBag.setExpand(getLocalObjMapper().writeValueAsString(expand));

            giftBagList.add(giftBag);
        }

        try {
            paymentService.batchSendGiftBroadcast(lid, giftBagList);
        } catch (Exception e) {
            e.printStackTrace();
            fail("gift batch broadcast fail.");
        }
    }

}
