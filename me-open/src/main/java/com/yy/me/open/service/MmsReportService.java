package com.yy.me.open.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yy.me.json.JsonUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.thrift.TException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.enums.MmsType;
import com.yy.me.service.inner.ServiceConst;
import com.yy.me.user.UserInfo;
import com.yy.me.util.GeneralUtil;
import com.yy.tinytimes.thrift.mms.server.MmsReport;
import com.yy.tinytimes.thrift.mms.server.MmsReportAttc;
import com.yy.tinytimes.thrift.mms.server.MmsReportReq;
import com.yy.tinytimes.thrift.mms.server.MmsReportRsp;
import com.yy.tinytimes.thrift.mms.server.MmsReportServ.Iface;
import com.yy.tinytimes.thrift.mms.server.MmsSign;

@Service
public class MmsReportService {
    private static final Logger logger = LoggerFactory.getLogger(MmsReportService.class);

    private static final FastDateFormat DATETIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public static final String chid = "101017";
    public static final String appKey = "101017";
    public static final String appSecret = "elA75fOHgxxojcr78MptoRfPUW9MkxWL";

    public static final String txt_chid = "999911050";
    public static final String txt_appKey = "999911050";
    public static final String txt_appSecret = "QKmUbLafiUs3hlQAXnr9yVyOZlywRnQ4";

    @Value(value = "#{settings['node.productEnv']}")
    private boolean productEnv;

    @Autowired
    @Qualifier("mmsTxtReportThriftClient")
    private ReferenceFactory<com.yy.tinytimes.thrift.mms.txt.MmsReportServ> mmsTxtFactory;

    @Autowired
    @Qualifier("mmsReportThriftClient")
    private ReferenceFactory<Iface> thriftFactory;

    private Iface getClient() {
        return thriftFactory.getClient();
    }

    public boolean pushImgReport(long uid, String imgUrl, String exJsonParam) {
        if (uid <= 0 || StringUtils.isBlank(imgUrl)) {
            logger.warn("Invalid image report. uid: {}, img: {}", uid, imgUrl);
            return false;
        }
        
        boolean result = true;

        try {
            MmsReportReq reportReq = buildRequest(uid, imgUrl, exJsonParam);

            MmsReportRsp reportResp = getClient().pushReports(reportReq);
            
            logger.info("Reported image to MMS - serial: {}, uid: {}, img: {}, exParm: {}. Result - code: {}, msg: {}",
                    reportReq.getReports().get(0).getSerial(), uid, imgUrl, exJsonParam, reportResp.getCode(),
                    reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS image report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }
            
//            try {
//                DBObject obj = javaObj2Db(mongoTemplate, reportReq, null);
//                appendObjId(obj);
//                mongoTemplate.getDb().getCollection(COLLETION_MMS_REPORT_SEND_NAME).save(obj);
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//            }
        } catch (TException e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter error.", e);
            result = false;
        }

        return result;
    }

    public boolean pushTxtReport(UserInfo userInfo, String exJsonParam,MmsType mmsType) {
        if (userInfo.getUid() <= 0 || (StringUtils.isBlank(userInfo.getNick())&& StringUtils.isBlank(userInfo.getSignature()))) {
            logger.warn("Invalid text report. user: {}", userInfo);
            return false;
        }
        //测试环境不上报，不然会有问题
        if(!ServiceConst.productEnv){
            return true;
        }
        boolean result = true;

        try {
            com.yy.tinytimes.thrift.mms.txt.MmsReportReq reportReq = buildTxtRequest(userInfo, exJsonParam,mmsType);

            com.yy.tinytimes.thrift.mms.txt.MmsReportRsp reportResp = mmsTxtFactory.getClient().pushReports(reportReq);

            logger.info("Reported txt to MMS - serial: {}, uid: {}, nick: {},sign:{}, exParm: {}. Result - code: {}, msg: {}",
                    reportReq.getReports().get(0).getSerial(), userInfo.getUid(), userInfo.getNick(),userInfo.getSignature(), exJsonParam, reportResp.getCode(),
                    reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS text report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }
        } catch (TException e) {
            logger.error("Push MMS text report[uid: " + userInfo.getUid() + ", nick: " + userInfo.getNick() +", signature: "+userInfo.getSignature()+ "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error("Push MMS text report[uid: " + userInfo.getUid() + ", nick: " + userInfo.getNick() +", signature: "+userInfo.getSignature()+ "] encounter error.", e);
            result = false;
        }

        return result;
    }

    private MmsReportReq buildRequest(long uid, String imgUrl, String exJsonParam) throws Exception {
        MmsReportReq req = new MmsReportReq();
        req.setAppid(chid);
        req.setChid(chid);

        MmsReportAttc attachment = new MmsReportAttc();
        attachment.setAttcType("IMG");
        attachment.setAttcUrl(imgUrl);
        List<MmsReportAttc> attachments = Lists.newArrayList(attachment);

        List<MmsReport> reports = new ArrayList<MmsReport>();
        MmsReport mmsReport = new MmsReport();

        if (productEnv) {
            mmsReport.setSerial(new ObjectId().toHexString());
        } else {
            mmsReport.setSerial("test@" + new ObjectId().toHexString());

            ObjectNode jo = JsonUtil.createDefaultMapper().createObjectNode();
            jo.put("srcType", "TEST");

            String reportComment = JsonUtil.createDefaultMapper().writeValueAsString(jo);
            mmsReport.setReportComment(reportComment);
        }

        mmsReport.setUid(uid);
        mmsReport.setReportTime(DATETIME_FORMAT.format(new Date()));
        mmsReport.setAttachments(attachments);
        mmsReport.setExtPar(exJsonParam);
        reports.add(mmsReport);

        req.setReports(reports);

        List<String> signParNames = Lists.newArrayList();
        signParNames.add("MmsReportReq.appid");
        signParNames.add("MmsReportReq.chid");
        signParNames.add("MmsReport.serial");
        signParNames.add("MmsReport.uid");
        signParNames.add("MmsReport.reportTime");
        signParNames.add("MmsReportAttc.attcType");
        signParNames.add("MmsReportAttc.attcUrl");

        StringBuilder buf = new StringBuilder();
        buf.append(req.getAppid()).append(req.getChid());
        buf.append(mmsReport.getSerial()).append(mmsReport.getUid()).append(mmsReport.getReportTime());
        buf.append(attachment.getAttcType()).append(attachment.getAttcUrl());

        String data = buf.toString() + appSecret;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        String sign = Hex.encodeHexString(bytes);

        MmsSign mmsSign = new MmsSign(appKey, sign, signParNames);

        req.setMmsSign(mmsSign);

        return req;
    }

    private static final String txt_report_content="{\"nick\":\"%s\",\"sign\":\"%s\"}";
    private com.yy.tinytimes.thrift.mms.txt.MmsReportReq buildTxtRequest(UserInfo userInfo, String exJsonParam,MmsType mmsType) throws Exception {
        com.yy.tinytimes.thrift.mms.txt.MmsReportReq req = new com.yy.tinytimes.thrift.mms.txt.MmsReportReq();
        req.setAppid(txt_chid);
        req.setChid(txt_chid);
        com.yy.tinytimes.thrift.mms.txt.MmsReportAttc attachment = new com.yy.tinytimes.thrift.mms.txt.MmsReportAttc();
        attachment.setAttcType("JSON");
        attachment.setAttcText(String.format(txt_report_content, StringUtils.isBlank(userInfo.getNick())?" ":userInfo.getNick(),StringUtils.isBlank(userInfo.getSignature())?" ":userInfo.getSignature()));
        List<com.yy.tinytimes.thrift.mms.txt.MmsReportAttc> attachments = Lists.newArrayList(attachment);

        List<com.yy.tinytimes.thrift.mms.txt.MmsReport> reports = new ArrayList<com.yy.tinytimes.thrift.mms.txt.MmsReport>();
        com.yy.tinytimes.thrift.mms.txt.MmsReport mmsReport = new com.yy.tinytimes.thrift.mms.txt.MmsReport();

        if (productEnv) {
            mmsReport.setSerial(new ObjectId().toHexString());
        } else {
            mmsReport.setSerial("test@" + new ObjectId().toHexString());

            ObjectNode jo = JsonUtil.createDefaultMapper().createObjectNode();
            jo.put("srcType", "TEST");

            String reportComment =JsonUtil.createDefaultMapper().writeValueAsString(jo);
            mmsReport.setReportComment(reportComment);
        }

        mmsReport.setUid(userInfo.getUid());
        mmsReport.setReportTime(DATETIME_FORMAT.format(new Date()));
        mmsReport.setAttachments(attachments);
        mmsReport.setExtPar(exJsonParam);
        reports.add(mmsReport);

        req.setReports(reports);

        List<String> signParNames = Lists.newArrayList();
        signParNames.add("MmsReportReq.appid");
        signParNames.add("MmsReportReq.chid");
        signParNames.add("MmsReport.serial");
        signParNames.add("MmsReport.uid");
        signParNames.add("MmsReport.reportTime");
        signParNames.add("MmsReportAttc.attcType");
        signParNames.add("MmsReportAttc.attcText");

        StringBuilder buf = new StringBuilder();
        buf.append(req.getAppid()).append(req.getChid());
        buf.append(mmsReport.getSerial()).append(mmsReport.getUid()).append(mmsReport.getReportTime());
        buf.append(attachment.getAttcType()).append(attachment.getAttcText());

        String data = buf.toString() + txt_appSecret;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        String sign = Hex.encodeHexString(bytes);

        com.yy.tinytimes.thrift.mms.txt.MmsSign mmsSign = new com.yy.tinytimes.thrift.mms.txt.MmsSign();
        mmsSign.setAppKey(txt_appKey);
        mmsSign.setSign(sign);
        mmsSign.setSignParNames(signParNames);
        req.setMmsSign(mmsSign);

        return req;
    }

}
