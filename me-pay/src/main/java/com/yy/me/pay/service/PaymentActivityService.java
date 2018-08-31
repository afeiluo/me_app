package com.yy.me.pay.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.pay.dao.PaymentActivityMongoDBMapper.*;
import static com.yy.me.pay.dao.PaymentActivityUserMongoDBMapper.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.pay.dao.PaymentActivityMongoDBMapper;
import com.yy.me.pay.dao.PaymentActivityUserMongoDBMapper;
import com.yy.me.pay.entity.PaymentActivity;
import com.yy.me.pay.entity.PaymentActivityUser;
import com.yy.me.pay.util.PaymentTokenUtil;

@Service
public class PaymentActivityService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentActivityService.class);

    private static final String STATUS_KEY = "status";

    private static final int STATUS_SOLD_OUT = -1;
    private static final int STATUS_NOT_PURCHASED = 0;
    private static final int STATUS_PURCHASED = 1;
    private static final int STATUS_PROCESSING = 2;

    @Autowired
    private PaymentActivityMongoDBMapper activityMapper;

    @Autowired
    private PaymentActivityUserMongoDBMapper activityUserMapper;
    
    @Value(value = "#{settings['node.productEnv']}")
    private boolean productEnv;

    @Autowired
    private MetricsClient metricsClient;

    public void getActInfos(final long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            List<PaymentActivity> actList = activityMapper.getValidActivity();
            String token = "";

            if (actList == null) {
                logger.info("No available payment activity exists.");

                actList = Lists.newArrayList();
                
                Map<String, Object> ret = Maps.newHashMap();
                ret.put("acts", actList);
                ret.put("token", token);

                sendResponseAuto(request, response, genMsgObj(SUCCESS, null, ret));
                return;
            }

            List<ObjectNode> actJsonList = Lists.transform(actList, new Function<PaymentActivity, ObjectNode>() {
                @Override
                public ObjectNode apply(PaymentActivity act) {
                    ObjectNode jo = getLocalObjMapper().createObjectNode();
                    jo.put(FIELD_PAYMENT_ACTIVITY_ID, act.getActId());
                    jo.put(FIELD_PAYMENT_ACTIVITY_NAME, act.getName());
                    jo.put(FIELD_PAYMENT_ACTIVITY_PROPID, act.getPropId());
                    jo.put(FIELD_PAYMENT_ACTIVITY_PRODUCT_ID, act.getProductId());
                    jo.put(FIELD_PAYMENT_ACTIVITY_POSITION, act.getPosition());
                    jo.put(FIELD_PAYMENT_ACTIVITY_TOTAL_COUNT, act.getTotalCount());
                    jo.put(FIELD_PAYMENT_ACTIVITY_SALED_COUNT, act.getSaledCount());
                    jo.put(FIELD_PAYMENT_ACTIVITY_DISPLAY_SALED_COUNT, act.getDisplaySaledCount());

                    int status = STATUS_SOLD_OUT;
                    if (act.getSaledCount() >= act.getTotalCount()) {
                        status = STATUS_SOLD_OUT;
                    } else {
                        try {
                            long userBuyCount = activityUserMapper.getUserBuyCount(uid, act.getPropId());

                            if (userBuyCount >= act.getBuyCountPerUser()) {
                                status = STATUS_PURCHASED;
                            } else {
                                if (activityUserMapper.checkUserLock(uid, act.getPropId())) {
                                    status = STATUS_PROCESSING;
                                } else {
                                    status = STATUS_NOT_PURCHASED;
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Check user buy count error.", e);
                        }
                    }

                    jo.put(STATUS_KEY, status);

                    return jo;
                }
            });
            
            token = PaymentTokenUtil.genToken(productEnv, uid);
            
            Map<String, Object> ret = Maps.newHashMap();
            ret.put("acts", actJsonList);
            ret.put("token", token);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getActInfos", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, ret));
        } catch (Exception e) {
            logger.error("Get payment activity infos error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getActInfos", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get payment activity infos error."));
        }
    }

    public void checkEligibility(long uid, int propId, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            PaymentActivity activity = activityMapper.findByPropId(propId);
            if (activity == null) {
                logger.error("Cannot found payment activity by propId: " + propId);

                sendResponseAuto(request, response, genMsgObj(FAILED, "activity not found."));
                return;
            }

            int status = STATUS_SOLD_OUT;
            if (activity.getSaledCount() >= activity.getTotalCount()) {
                status = STATUS_SOLD_OUT;
            } else {
                long userBuyCount = activityUserMapper.getUserBuyCount(uid, activity.getPropId());

                if (userBuyCount >= activity.getBuyCountPerUser()) {
                    status = STATUS_PURCHASED;
                } else {
                    if (activityUserMapper.addUserLock(uid, activity.getPropId())) {
                        status = STATUS_NOT_PURCHASED;
                    } else {
                        status = STATUS_PROCESSING;
                    }
                }
            }

            ObjectNode jo = getLocalObjMapper().createObjectNode();
            jo.put(FIELD_PAYMENT_ACTIVITY_PROPID, propId);
            jo.put(FIELD_PAYMENT_ACT_USER_UID, uid);
            jo.put(STATUS_KEY, status);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "checkEligibility", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, jo));
        } catch (Exception e) {
            logger.error("Check payment activity eligibility error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "checkEligibility", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Check payment activity eligibility error."));
        }
    }

    public void updateActInfo(String seq, long uid, int propId, long buyTime, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            PaymentActivity activity = activityMapper.findByPropId(propId);
            if (activity == null) {
                logger.error("Cannot found payment activity by propId: " + propId);

                sendResponse(request, response, genMsgObj(FAILED, "activity not found."));
                return;
            }

            PaymentActivityUser activityUser = new PaymentActivityUser();
            activityUser.setPropId(propId);
            activityUser.setUid(uid);
            activityUser.setBuyTime(new Date(buyTime));
            activityUser.setSeq(seq);

            PaymentActivityUser existsUser = activityUserMapper.insert(activityUser);

            if (existsUser == null) {
                Random random = new Random();
                int incDisplayCount = random.nextInt(10) + 1;

                activityMapper.increaseSoldCount(activity.getActId(), 1, incDisplayCount);
            }

            activityUserMapper.removeUserLock(uid, propId);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "updateActInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS, "success"));
        } catch (Exception e) {
            logger.error("Update payment activity info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "updateActInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Update payment activity info error."));
        }
    }

}
