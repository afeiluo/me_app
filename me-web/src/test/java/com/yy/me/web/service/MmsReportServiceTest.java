package com.yy.me.web.service;

import static com.yy.me.service.inner.ServiceConst.MMS_ACTION_TIME;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.me.json.JsonUtil;

//@Ignore
@ContextConfiguration(locations = { "classpath*:spring/appContext.xml" })
public class MmsReportServiceTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    private MmsReportService reportService;

    @Ignore
    @Test
    public void testReport() {
        String headerUrl = "http://ourtimes.bs2dl.yy.com/feed_1447812502102.jpg";

        Map<String, Object> jo = Maps.newHashMap();
        jo.put("oldHeaderUrl", headerUrl);
        jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
        String exJsonParam = JsonUtil.instance.toJson(jo);

        boolean result = reportService.pushImgReport(100001001, headerUrl, exJsonParam);
        assertTrue(result);
    }

    @Test
    public void testReportVideo() {
        String videoUrl = "http://ourtimes.bs2dl.yy.com/feed_1447812502102.jpg";

        Map<String, Object> jo = Maps.newHashMap();
        jo.put("videoUrl", videoUrl);
        jo.put(MMS_ACTION_TIME, System.currentTimeMillis());

        boolean result = reportService.pushVideoReport(100001001, videoUrl, jo);
        assertTrue(result);
    }

    @Ignore
    @Test
    public void testReportAlbum() {
        String pic1 = "http://ourtimes.bs2dl.yy.com/feed_1447812502102.jpg";
        String pic2 = "http://ourtimespicture.bs2dl.yy.com/c_201002094_1481877054164.jpeg";
        List<String> picList = Lists.newArrayList();
        picList.add(pic1);
        picList.add(pic2);
        Map<String, Object> jo = Maps.newHashMap();
        jo.put("albums", picList);
        jo.put(MMS_ACTION_TIME, System.currentTimeMillis());

        boolean result = reportService.pushImgReport(100001001, picList, jo);
        assertTrue(result);
    }
}
