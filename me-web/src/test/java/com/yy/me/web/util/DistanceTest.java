package com.yy.me.web.util;

import static com.yy.me.http.BaseServletUtil.getLocalObjMapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yy.me.lbs.DistIpUtil;

public class DistanceTest {

    @Ignore
    @Test
    public void test1Km() {
        System.out.println(DistIpUtil.calDistance(31.5, -96.39, 31.5, -96.38));
    }

    @Ignore
    @Test
    public void testReplace() {
        List<String> oriList = new ArrayList<String>();
        oriList.add("我");
        oriList.add("是");
        oriList.add("中");
        oriList.add("国");
        oriList.add("人");
        System.out.println("ori　list:" + oriList);
        for (int i = 0; i < oriList.size(); i++) {
            oriList.set(i, "6");
        }
        System.out.println("cur　list:" + oriList);
    }

    @Test
    public void testDeserial() throws Exception {
        String str = " {\"uid\":201001769,\"headerUrl\":\"https://ourtimespicture.bs2dl-ssl.yy.com/c_201001769_1483515361534.jpg\",\"mmsType\":1,\"oldHeaderUrl\":\"https://ourtimespicture.bs2dl-ssl.yy.com/c_201001769_1478695503964.jpg\",\"actionTime\":1483515363544}";
        ObjectNode jo = (ObjectNode) getLocalObjMapper().readTree(str);
        System.out.println(jo);
    }
}
