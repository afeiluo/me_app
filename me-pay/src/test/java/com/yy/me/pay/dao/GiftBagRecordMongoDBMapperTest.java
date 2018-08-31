package com.yy.me.pay.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.common.collect.Maps;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.pay.entity.GiftBagRecord;
import com.yy.me.pay.entity.GiftCallbackReq.PlatformInfo;

@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml" })
public class GiftBagRecordMongoDBMapperTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private GiftBagRecordMongoDBMapper giftBagBMapper;

    @Test
    public void testSave() {
        GiftBagRecord giftBag = new GiftBagRecord();
        giftBag.setSeq("423423423423423");
        giftBag.setUid(200000118);
        giftBag.setRecvUid(100001444);
        giftBag.setUsedTime(new Date(System.currentTimeMillis()));
        giftBag.setPlatform(PlatformInfo.Android.getValue());
        giftBag.setLid("568cb1db00006723111d014c");
        giftBag.setPropId(1);
        giftBag.setPropCount(2);
        giftBag.setAmount(10);
        giftBag.setIncome(5);

        try {
            Map<String, Object> expand = Maps.newHashMap();
            expand.put("lid", "568cb1db00006723111d014c");
            giftBag.setExpand(BaseServletUtil.getLocalObjMapper().writeValueAsString(expand));
            
            giftBagBMapper.save(giftBag);
        } catch (Exception e) {
            e.printStackTrace();
            fail("save error");
        }
    }
}
