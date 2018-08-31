package com.yy.me.pay.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.base.http.CSHttpClient;
import com.yy.cs.base.json.Json;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.pay.entity.GiftCallbackReq;
import com.yy.me.pay.entity.GiftCallbackReq.CurrencyType;
import com.yy.me.pay.entity.GiftCallbackReq.PlatformInfo;
import com.yy.me.pay.entity.GiftCallbackReq.UseInfo;

@Ignore
public class GiftCallbackTest {
    
    @Test
    public void testCallback() throws Exception {
        CSHttpClient httpClient = new CSHttpClient();
        
        Map<String, String> req = new HashMap<String, String>();
        req.put("appId", "100001");
        req.put("sign", "");
        
        GiftCallbackReq giftReq = new GiftCallbackReq();
        giftReq.setSeq("5646488464876");
        giftReq.setUid(1000008);
        giftReq.setRecvUid(1000009);
        giftReq.setUsedTimestamp(System.currentTimeMillis());
        
        UseInfo useInfo = new UseInfo();
        useInfo.setPropId(1);
        useInfo.setCurrencyType(CurrencyType.MIBI.getValue());
        useInfo.setAmount(10);
        useInfo.setPropCount(10);
        useInfo.setIncome(5);
        useInfo.setIntimacy(5);
        giftReq.setUseInfos(Lists.newArrayList(useInfo));
        
        Map<String, Object> expand = Maps.newHashMap();
        expand.put("lid", "568cb1db00006723111d014c");
        giftReq.setExpand(BaseServletUtil.getLocalObjMapper().writeValueAsString(expand));
        
        giftReq.setPlatform(PlatformInfo.Android.getValue());
        req.put("data", Json.ObjToStr(giftReq));
        
        
        System.out.println(httpClient.doPost("http://test.tt.yy.com/pay/revenue/giftCallback", req));

    }

}
