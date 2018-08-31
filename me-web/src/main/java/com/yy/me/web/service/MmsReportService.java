package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.*;
import static com.yy.me.service.inner.ServiceConst.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.yy.me.dao.ReportCountMongoDBMapper;
import com.yy.me.enums.ReportFrom;
import com.yy.me.enums.ReportResult;
import com.yy.me.http.HttpUtil;
import com.yy.me.json.JsonUtil;
import com.yy.me.service.BanService;
import com.yy.me.web.dao.ReportAppealMongoDBMapper;
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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.base.http.CSHttpClient;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.enums.MmsType;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LinkUserInfo;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.client.util.LiveShowUtil;
import com.yy.me.liveshow.thrift.ClientType;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.service.inner.ServiceConst;
import com.yy.me.time.MaskClock;

import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.web.dao.ReportMongoDBMapper;
import com.yy.me.yycloud.AppConstants;
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

    /*
     * 头像
     */
    public static final String chid = "101017";
    public static final String appKey = "101017";
    public static final String appSecret = "elA75fOHgxxojcr78MptoRfPUW9MkxWL";

    /*
     * 视频
     */

    public static final String video_chid = "301030";
    public static final String video_appKey = "301030";
    public static final String video_appSecret = "WFUMGLGdTKtaaG2CIB1D3YuJk1wxsRG2";

    /*
     * 相册
     */
    public static final String album_chid = "301029";
    public static final String album_appKey = "301029";
    public static final String album_appSecret = "v7zRSWRLXo4msE8imUNiKgqbS8hWZaYj";

    /*
     * 文字
     */
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

    @Autowired
    @Qualifier("mmsVideoReportThriftClient")
    private ReferenceFactory<com.yy.tinytimes.thrift.mms.server.video.MmsReportServ.Iface> videoThriftFactory;

    @Autowired
    private ReportMongoDBMapper reportMongoDBMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private MetricsClient metricsClient;

    public static String reportUrl = "http://mobile.report.yy.com/reportSubmit.action";

    public static String hiidoActiveUrl = "http://14.17.109.238:8518/service/read";

    @Autowired
    private ReportCountMongoDBMapper reportCountMongoDBMapper;

    @Autowired
    private ReportAppealMongoDBMapper reportAppealMongoDBMapper;

    @Autowired
    private BanService banService;

    @PostConstruct
    public void init() {
        if (!productEnv) {
            reportUrl = "http://172.27.49.10:8098/reportSubmit.action";
        }
    }

    private final CSHttpClient httpClient = new CSHttpClient();

    private Iface getClient() {
        return thriftFactory.getClient();
    }

    private com.yy.tinytimes.thrift.mms.server.video.MmsReportServ.Iface getVideoClient() {
        return videoThriftFactory.getClient();
    }

    public void reportLs(String ip, long uid, String deviceId, String lid, String huanJuYunSerial, String reportedType, String snapshotUrl,
            String reason, String gongPing, Integer from, String gongPingViolateText) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveShow == null) {
                logger.warn("MMS Ls report, LiveShow not exist, lid={}, uid={}", lid, uid);
            } else {
                UserInfo submitUser = userHessianService.getClient().getUserByUid(uid, false);
                if (StringUtils.isBlank(reason) || reason.contains("画面淫秽色情") || reason.contains("语音淫秽色情")) {// 旧版本或者3.1版本满足条件
                    reportMongoDBMapper.insertLiveShow(uid, lid, null, snapshotUrl);

                    Map<String, String> parameters = reportLsBuilder(ip, submitUser, deviceId, lid, null, huanJuYunSerial, reportedType, snapshotUrl);

                    parameters.put("reportedUid", liveShow.getUid() + "");
                    parameters.put("reportedDeviceId", liveShow.getDeviceId() == null ? "notExist" : liveShow.getDeviceId());
                    parameters.put("reportedIP", liveShow.getIp() == null ? "notExist" : liveShow.getIp());
                    parameters.put("reportedNick", liveShow.getNick());
                    parameters.put("reportedIcon", liveShow.getHeaderUrl());
                    parameters.put("reportedLocation", liveShow.getLocationName());

                    Map<String, Object> jo = Maps.newHashMap();
                    jo.put(MMS_TYPE, MmsType.LS_SNAPSHOT.getValue());
                    jo.put(RET_LS_LID, liveShow.getLid());
                    jo.put(RET_LS_UID, liveShow.getUid());
                    if (liveShow.getDeviceId() != null) {// 旧app没有设备id
                        jo.put(RET_U_DEVICE_ID, liveShow.getDeviceId());
                    }
                    if (liveShow.getClientType() != null) {
                        jo.put(RET_U_OS_TYPE, ("iOS".equals(liveShow.getClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue()));
                    }
                    parameters.put("extraData", getLocalObjMapper().writeValueAsString(jo));

                    logger.info("MMS Ls report, uid:{}, image:{}, data:{}", uid, snapshotUrl, parameters);
                    String result = httpClient.doPost(reportUrl, parameters);
                    logger.info("MMS Ls report result:{}, uid:{}, snapshotUrl:{}, serial:{}", result, uid, snapshotUrl, parameters.get("serial"));
                } else {// 后台审核
                    doReport(from, gongPingViolateText, uid, liveShow.getUid(), liveShow.getDeviceId(), lid, snapshotUrl, reason, gongPing);
                }
            }
        } catch (Exception e) {
            logger.error("MMS Ls report[uid: " + uid + ", image: " + snapshotUrl + "] encounter error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "MmsReportService", "reportLs", MaskClock.getCurtime() - start, rescode);
        }
    }

    public void reportLsLink(String ip, long uid, String deviceId, String lid, long linkUid, String huanJuYunSerial, String reportedType,
            String snapshotUrl, String reason, String gongPing, Integer from, String gongPingViolateText) {
        long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveShow == null) {
                logger.warn("MMS Ls Link report, LiveShow not exist, lid={}, uid={}", lid, uid);
            } else if (liveShow.getLinkUid() == null || liveShow.getLinkUsers() == null) {
                logger.warn("MMS Ls Link report, LiveShow not linking, lid={}, uid={}", lid, uid);
            } else if (!LiveShowUtil.checkLinking(liveShow, linkUid)) {
                logger.warn("MMS Ls Link report, LiveShow not linking with him(linkUid={}), lid={}, uid={}", linkUid, lid, uid);
            } else {

                UserInfo submitUser = userHessianService.getClient().getUserByUid(uid, false);
                LinkUserInfo linkUser = LiveShowUtil.findLinkInfo(liveShow, linkUid);
                if (StringUtils.isBlank(reason) || reason.contains("画面淫秽色情") || reason.contains("语音淫秽色情")) {// 旧版本或者3.1版本满足条件
                    reportMongoDBMapper.insertLiveShow(uid, lid, "linkUid:" + linkUid + ", " + linkUser.getStartTime(), snapshotUrl);

                    Map<String, String> parameters = reportLsBuilder(ip, submitUser, deviceId, lid, linkUser, huanJuYunSerial, reportedType,
                            snapshotUrl);

                    parameters.put("reportedUid", linkUid + "");
                    parameters.put("reportedDeviceId", linkUser.getDeviceId() == null ? "notExist" : linkUser.getDeviceId());
                    parameters.put("reportedIP", linkUser.getIp() == null ? "notExist" : liveShow.getIp());
                    parameters.put("reportedNick", linkUser.getNick());
                    parameters.put("reportedIcon", linkUser.getHeaderUrl());
                    // parameters.put("reportedLocation", linkUser.getLocationName());

                    Map<String, Object> jo = Maps.newHashMap();
                    jo.put(MMS_TYPE, MmsType.LS_LINK_SNAPSHOT.getValue());
                    jo.put(RET_LS_LID, liveShow.getLid());
                    jo.put(RET_LS_UID, liveShow.getUid());
                    if (linkUser.getDeviceId() != null) {// 旧app没有设备id
                        jo.put(RET_U_LINK_DEVICE_ID, linkUser.getDeviceId());
                    }
                    if (linkUser.getClientType() != null) {
                        jo.put(RET_U_LINK_OS_TYPE,
                                ("iOS".equals(linkUser.getClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue()));
                    }
                    parameters.put("extraData", getLocalObjMapper().writeValueAsString(jo));

                    logger.info("MMS Ls Link report, uid:{}, image:{}, data:{}", uid, snapshotUrl, parameters);
                    String result = httpClient.doPost(reportUrl, parameters);
                    logger.info("MMS Ls Link report result:{}, uid:{}, snapshotUrl:{}, serial:{}", result, uid, snapshotUrl, parameters.get("serial"));
                } else {// 后台审核
                    doReport(from, gongPingViolateText, uid, linkUid, linkUser.getDeviceId(), lid, snapshotUrl, reason, gongPing);
                }
            }
        } catch (Exception e) {
            logger.error("MMS Ls Link report[uid: " + uid + ", image: " + snapshotUrl + "] encounter error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "MmsReportService", "reportLsLink", MaskClock.getCurtime() - start, rescode);
        }
    }

    private Map<String, String> reportLsBuilder(String ip, UserInfo submitUser, String deviceId, String lid, LinkUserInfo linkUser,
            String huanJuYunSerial, String reportedType, String snapshotUrl) {
        long uid = submitUser.getUid();
        try {
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByLid(lid);
            if (liveShow == null) {
                logger.warn("LiveShow not exist, lid={}, uid={}", lid, uid);
                return null;
            } else {
                reportMongoDBMapper.insertLiveShow(uid, lid, null, snapshotUrl);

                Map<String, String> parameters = Maps.newLinkedHashMap();
                parameters.put("appid", AppConstants.YY_CLOUD_APP_KEY);
                parameters.put("serial", MongoUtil.genObjId().toHexString());
                parameters.put("submitUid", uid + "");
                parameters.put("submitDeviceId", deviceId);
                parameters.put("submitIP", ip);
                parameters.put("submitNick", submitUser.getNick());
                parameters.put("submitIcon", submitUser.getHeaderUrl());

                parameters.put("reportedUid", liveShow.getUid() + "");
                parameters.put("reportedDeviceId", liveShow.getDeviceId() == null ? "notExist" : liveShow.getDeviceId());
                if (liveShow.getIp() != null) {
                    parameters.put("reportedIP", liveShow.getIp());
                }
                parameters.put("reportedNick", liveShow.getNick());
                parameters.put("reportedIcon", liveShow.getHeaderUrl());
                parameters.put("reportedLocation", liveShow.getLocationName());

                if (liveShow.getLinkUid() != null) {
                    if (linkUser == null) {
                        linkUser = liveShow.getLinkUsers().get(0);
                    }
                    List<Map<String, String>> ja = Lists.newArrayList();
                    Map<String, String> jo = Maps.newHashMap();
                    jo.put("lianmaiNick", linkUser.getNick());
                    jo.put("lianmaiUid", linkUser.getUid() + "");
                    ja.add(jo);
                    parameters.put("lianmaiInfo", getLocalObjMapper().writeValueAsString(ja));
                }

                parameters.put("reportedType", reportedType);
                if (StringUtils.isBlank(huanJuYunSerial)) {
                    huanJuYunSerial = lid;
                }
                parameters.put("reportedVideoId", huanJuYunSerial);
                parameters.put("reportedSid", liveShow.getSid() + "");
                parameters.put("reportedVideoTitle", StringUtils.isBlank(liveShow.getTitle()) ? "无Title" : liveShow.getTitle());
                parameters.put("reportedVideoOnlineNum", liveShow.getGuestCount() + "");
                parameters.put("reportedCaptureUrl", snapshotUrl);
                parameters.put("reportedTimestamp", System.currentTimeMillis() + "");
                return parameters;
            }
        } catch (Exception e) {
            logger.error("MMS Ls report builder[uid: " + uid + ", image: " + snapshotUrl + "] encounter error.", e);
        }
        return null;
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

            logger.info("Reported image to MMS - serial: {}, uid: {}, img: {}, exParm: {}. Result - code: {}, msg: {}", reportReq.getReports().get(0)
                    .getSerial(), uid, imgUrl, exJsonParam, reportResp.getCode(), reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS image report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }

            // try {
            // DBObject obj = javaObj2Db(mongoTemplate, reportReq, null);
            // appendObjId(obj);
            // mongoTemplate.getDb().getCollection(COLLETION_MMS_REPORT_SEND_NAME).save(obj);
            // } catch (Exception e) {
            // logger.error(e.getMessage(), e);
            // }
        } catch (TException e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter error.", e);
            result = false;
        }

        return result;
    }

    public boolean pushImgReport(long uid, List<String> imgUrls, Map<String, Object> extMap) {
        if (uid <= 0 || imgUrls == null) {
            logger.warn("Invalid image report. uid: {}, img: {}", uid, imgUrls);
            return false;
        }

        boolean result = true;

        try {
            MmsReportReq reportReq = buildRequest(uid, imgUrls, extMap);

            MmsReportRsp reportResp = getClient().pushReports(reportReq);

            logger.info("Reported image to MMS - serials: {}, uid: {}, img: {}, exParm: {}. Result - code: {}, msg: {}",
                    getAllSerials(reportReq.getReports()), uid, imgUrls, extMap, reportResp.getCode(), reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS image report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }

            // try {
            // DBObject obj = javaObj2Db(mongoTemplate, reportReq, null);
            // appendObjId(obj);
            // mongoTemplate.getDb().getCollection(COLLETION_MMS_REPORT_SEND_NAME).save(obj);
            // } catch (Exception e) {
            // logger.error(e.getMessage(), e);
            // }
        } catch (TException e) {
            logger.error("Push MMS image report[uid: " + uid + ", images: " + imgUrls + "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error("Push MMS image report[uid: " + uid + ", images: " + imgUrls + "] encounter error.", e);
            result = false;
        }

        return result;
    }

    private String getAllSerials(List<MmsReport> reports) {
        StringBuilder sb = new StringBuilder();
        for (MmsReport mmsReport : reports) {
            sb.append(mmsReport.getSerial());
            sb.append(":");
        }
        return sb.toString();
    }

    /**
     * 上报视频
     * 
     * @param uid
     * @param videoUrl
     * @return
     */
    public boolean pushVideoReport(long uid, String videoUrl, Map<String, Object> extMap) {
        if (uid <= 0 || StringUtils.isBlank(videoUrl)) {
            logger.warn("Invalid video report. uid: {}, img: {}", uid, videoUrl);
            return false;
        }

        boolean result = true;

        try {
            MmsReportReq reportReq = buildVideoRequest(uid, videoUrl, extMap);

            MmsReportRsp reportResp = getVideoClient().pushReports(reportReq);

            logger.info("Reported video to MMS - serial: {}, uid: {}, video: {}, exParm: {}. Result - code: {}, msg: {}",
                    reportReq.getReports().get(0).getSerial(), uid, videoUrl, extMap, reportResp.getCode(), reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS video report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }

        } catch (TException e) {
            logger.error("Push MMS video report[uid: " + uid + ", video: " + videoUrl + "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error("Push MMS video report[uid: " + uid + ", video: " + videoUrl + "] encounter error.", e);
            result = false;
        }

        return result;
    }

    public boolean pushTxtReport(UserInfo userInfo, String exJsonParam, MmsType mmsType) {
        if (userInfo.getUid() <= 0 || (StringUtils.isBlank(userInfo.getNick()) && StringUtils.isBlank(userInfo.getSignature()))) {
            logger.warn("Invalid text report. user: {}", userInfo);
            return false;
        }
        // 测试环境不上报，不然会有问题
        if (!ServiceConst.productEnv) {
            return true;
        }
        boolean result = true;

        try {
            com.yy.tinytimes.thrift.mms.txt.MmsReportReq reportReq = buildTxtRequest(userInfo, exJsonParam, mmsType);

            com.yy.tinytimes.thrift.mms.txt.MmsReportRsp reportResp = mmsTxtFactory.getClient().pushReports(reportReq);

            logger.info("Reported txt to MMS - serial: {}, uid: {}, nick: {},sign:{}, exParm: {}. Result - code: {}, msg: {}", reportReq.getReports()
                    .get(0).getSerial(), userInfo.getUid(), userInfo.getNick(), userInfo.getSignature(), exJsonParam, reportResp.getCode(),
                    reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                result = false;

                logger.error("Push MMS text report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
            }
        } catch (TException e) {
            logger.error(
                    "Push MMS text report[uid: " + userInfo.getUid() + ", nick: " + userInfo.getNick() + ", signature: " + userInfo.getSignature()
                            + "] encounter TException.", e);
            result = false;
        } catch (Exception e) {
            logger.error(
                    "Push MMS text report[uid: " + userInfo.getUid() + ", nick: " + userInfo.getNick() + ", signature: " + userInfo.getSignature()
                            + "] encounter error.", e);
            result = false;
        }

        return result;
    }

    public List<String> pushImgReportRetSerial(long uid, String imgUrl, String exJsonParam) {
        if (uid <= 0 || StringUtils.isBlank(imgUrl)) {
            logger.warn("Invalid image report. uid: {}, img: {}", uid, imgUrl);
            return null;
        }

        try {
            MmsReportReq reportReq = buildRequest(uid, imgUrl, exJsonParam);

            MmsReportRsp reportResp = getClient().pushReports(reportReq);

            logger.info("Reported image to MMS - serial: {}, uid: {}, img: {}, exParm: {}. Result - code: {}, msg: {}", reportReq.getReports().get(0)
                    .getSerial(), uid, imgUrl, exJsonParam, reportResp.getCode(), reportResp.getMsg());

            int rstCde = reportResp.getCode();
            if (rstCde <= 0) {
                logger.error("Push MMS image report error. result code: {}, message: {}", rstCde, reportResp.getMsg());
                return null;
            }
            List<MmsReport> list = reportReq.getReports();
            List<String> serialList = Lists.transform(list, new Function<MmsReport, String>() {
                public String apply(MmsReport input) {
                    return input.getSerial();
                }
            });
            return serialList;

            // try {
            // DBObject obj = javaObj2Db(mongoTemplate, reportReq, null);
            // appendObjId(obj);
            // mongoTemplate.getDb().getCollection(COLLETION_MMS_REPORT_SEND_NAME).save(obj);
            // } catch (Exception e) {
            // logger.error(e.getMessage(), e);
            // }
        } catch (TException e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter TException.", e);
        } catch (Exception e) {
            logger.error("Push MMS image report[uid: " + uid + ", image: " + imgUrl + "] encounter error.", e);
        }

        return null;
    }

    /**
     * 构建视频审核请求
     * 
     */
    private MmsReportReq buildVideoRequest(long uid, String videoUrl, Map<String, Object> extMap) throws Exception {
        MmsReportReq req = new MmsReportReq();
        req.setAppid(video_chid);
        req.setChid(video_chid);

        MmsReportAttc attachment = new MmsReportAttc();
        attachment.setAttcType("VIDEO_FILE");
        attachment.setAttcUrl(videoUrl);
        List<MmsReportAttc> attachments = Lists.newArrayList(attachment);

        List<MmsReport> reports = new ArrayList<MmsReport>();
        MmsReport mmsReport = new MmsReport();

        if (productEnv) {
            mmsReport.setSerial(new ObjectId().toHexString());
        } else {
            mmsReport.setSerial("test@" + new ObjectId().toHexString());

            Map<String, String> jo = Maps.newHashMap();
            jo.put("srcType", "TEST");

            String reportComment = getLocalObjMapper().writeValueAsString(jo);
            mmsReport.setReportComment(reportComment);
        }

        mmsReport.setUid(uid);
        mmsReport.setReportTime(DATETIME_FORMAT.format(new Date()));
        mmsReport.setAttachments(attachments);
        mmsReport.setExtPar(getLocalObjMapper().writeValueAsString(extMap));
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

        String data = buf.toString() + video_appSecret;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        String sign = Hex.encodeHexString(bytes);

        MmsSign mmsSign = new MmsSign(video_appKey, sign, signParNames);

        req.setMmsSign(mmsSign);

        return req;
    }

    /**
     * 构建相册图片审核请求
     * 
     * @param uid
     * @param imgUrls
     * @return
     * @throws Exception
     */
    private MmsReportReq buildRequest(long uid, List<String> imgUrls, Map<String, Object> extMap) throws Exception {
        MmsReportReq req = new MmsReportReq();
        req.setAppid(album_chid);
        req.setChid(album_chid);
        StringBuilder buf = new StringBuilder();
        buf.append(req.getAppid()).append(req.getChid());
        List<MmsReport> reports = new ArrayList<MmsReport>();
        for (String imgUrl : imgUrls) {
            MmsReport mmsReport = new MmsReport();
            if (productEnv) {
                mmsReport.setSerial(new ObjectId().toHexString());
            } else {
                mmsReport.setSerial("test@" + new ObjectId().toHexString());
                Map<String, String> jo = Maps.newHashMap();
                jo.put("srcType", "TEST");
                String reportComment = getLocalObjMapper().writeValueAsString(jo);
                mmsReport.setReportComment(reportComment);
            }
            mmsReport.setUid(uid);
            mmsReport.setReportTime(DATETIME_FORMAT.format(new Date()));
            MmsReportAttc attachment = new MmsReportAttc();
            attachment.setAttcType("IMG");
            attachment.setAttcUrl(imgUrl);
            List<MmsReportAttc> attachments = Lists.newArrayList(attachment);
            mmsReport.setAttachments(attachments);
            extMap.put(MMS_HEAD_URL, imgUrl);
            mmsReport.setExtPar(getLocalObjMapper().writeValueAsString(extMap));
            reports.add(mmsReport);
            buf.append(mmsReport.getSerial()).append(mmsReport.getUid()).append(mmsReport.getReportTime());
            buf.append(attachment.getAttcType()).append(attachment.getAttcUrl());
        }
        req.setReports(reports);
        List<String> signParNames = Lists.newArrayList();
        signParNames.add("MmsReportReq.appid");
        signParNames.add("MmsReportReq.chid");
        signParNames.add("MmsReport.serial");
        signParNames.add("MmsReport.uid");
        signParNames.add("MmsReport.reportTime");
        signParNames.add("MmsReportAttc.attcType");
        signParNames.add("MmsReportAttc.attcUrl");

        String data = buf.toString() + album_appSecret;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        String sign = Hex.encodeHexString(bytes);

        MmsSign mmsSign = new MmsSign(album_appKey, sign, signParNames);

        req.setMmsSign(mmsSign);

        return req;
    }

    /**
     * 构建头像图片审核请求
     * 
     * @param uid
     * @param imgUrl
     * @param exJsonParam
     * @return
     * @throws Exception
     */
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

            Map<String, String> jo = Maps.newHashMap();
            jo.put("srcType", "TEST");

            String reportComment = getLocalObjMapper().writeValueAsString(jo);
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

    private static final String txt_report_content = "{\"nick\":\"%s\",\"sign\":\"%s\"}";

    private com.yy.tinytimes.thrift.mms.txt.MmsReportReq buildTxtRequest(UserInfo userInfo, String exJsonParam, MmsType mmsType) throws Exception {
        com.yy.tinytimes.thrift.mms.txt.MmsReportReq req = new com.yy.tinytimes.thrift.mms.txt.MmsReportReq();
        req.setAppid(txt_chid);
        req.setChid(txt_chid);
        com.yy.tinytimes.thrift.mms.txt.MmsReportAttc attachment = new com.yy.tinytimes.thrift.mms.txt.MmsReportAttc();
        attachment.setAttcType("JSON");
        if (mmsType == MmsType.USER_NICK) {
            attachment.setAttcText(String.format(txt_report_content, StringUtils.isBlank(userInfo.getNick()) ? " " : userInfo.getNick(), " "));
        } else if (mmsType == MmsType.USER_SIGNATURE) {
            attachment.setAttcText(String.format(txt_report_content, " ",
                    StringUtils.isBlank(userInfo.getSignature()) ? " " : userInfo.getSignature()));
        } else {
            throw new IllegalStateException("invalid mmsType");
        }
        List<com.yy.tinytimes.thrift.mms.txt.MmsReportAttc> attachments = Lists.newArrayList(attachment);

        List<com.yy.tinytimes.thrift.mms.txt.MmsReport> reports = new ArrayList<com.yy.tinytimes.thrift.mms.txt.MmsReport>();
        com.yy.tinytimes.thrift.mms.txt.MmsReport mmsReport = new com.yy.tinytimes.thrift.mms.txt.MmsReport();

        if (productEnv) {
            mmsReport.setSerial(new ObjectId().toHexString());
        } else {
            mmsReport.setSerial("test@" + new ObjectId().toHexString());

            Map<String, Object> jo = Maps.newHashMap();
            jo.put("srcType", "TEST");

            String reportComment = getLocalObjMapper().writeValueAsString(jo);
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

    public int getActiveDay(long uid) throws Exception {
        if (!productEnv) {//测试环境不校验活跃天数
             return 10;//
        }
        Map outMap = new HashMap<>();
        outMap.put("v", "0.1");
        outMap.put("appId", "yyLiveIndexRecom_zhoupeiyuan");
        outMap.put("appKey", "oi2340sdfklkjdljlksjdasfjklkj");
        outMap.put("serviceTypeKey", "me_activite_uid");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("uid", uid);
        outMap.put("params", params);
        outMap.put("startindex", 0);
        outMap.put("reqnum", 10);
        outMap.put("requestColumns", null);
        String content = HttpUtil.sendPost(hiidoActiveUrl, JsonUtil.createDefaultMapper().writeValueAsString(outMap), "text/plain");
        JsonNode node = JsonUtil.createDefaultMapper().readTree(content);
        return node.get("data").asInt();
    }

    public void reportGuest(int from, String ip, long uid, long guestUid, String lid, String snapshotUrl, String reason, String gongPing,
            String gongPingViolateText) {
        int rescode = MetricsClient.RESCODE_SUCCESS;
        long start = System.currentTimeMillis();
        try {
            doReport(from, gongPingViolateText, uid, guestUid, null, lid, snapshotUrl, reason, gongPing);// TODO
                                                                                                         // 被举报者多个设备ID
        } catch (Exception e) {
            logger.error("reportGuest,uid:" + uid + ",guestUid:" + guestUid + ",lid:" + lid + "error", e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "MmsReportService", "reportGuest", MaskClock.getCurtime() - start, rescode);
        }
    }

    public void doReport(int from, String gongPingViolateText, long fromUid, long toUid, String deviceId, String lid, String snapshotUrl,
            String reason, String gongPing) throws Exception {
        if (ReportFrom.findByValue(from) == null) {
            logger.info("doReport from invalid,from:{},fromUid:{},toUid:{},deviceId:{},lid:{}", from, fromUid, toUid, deviceId, lid);
            return;
        }
        UserInfo toUser = userHessianService.getClient().getUserByUid(toUid, false);
        if (toUser.getBaned() != null && toUser.getBaned()) {
            logger.info("doReport user is already ban,from:{},fromUid:{},toUid:{},deviceId:{},lid:{}", from, fromUid, toUid, deviceId, lid);
            return;
        }
        ReportResult result = reportCountMongoDBMapper.report(from, gongPingViolateText, fromUid, toUid, snapshotUrl, gongPing, lid, reason,
                getActiveDay(fromUid));
        banService.handleReport(result, fromUid, toUid, deviceId, lid, snapshotUrl, reason, gongPing);
    }

    public Map<String, Object> getAppealStatus(long uid) {
        return reportAppealMongoDBMapper.getAppealStatus(uid);
    }
}
