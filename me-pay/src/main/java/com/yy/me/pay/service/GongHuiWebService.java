package com.yy.me.pay.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.GongHuiMongoDBMapper;
import com.yy.me.dao.GongHuiRuleMongoDBMapper;
import com.yy.me.entity.GongHui;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.pay.dao.GiftListMongoDBMapper;
import com.yy.me.pay.dao.GongHuiAnchorMongoDBMapper;
import com.yy.me.pay.entity.Gift;
import com.yy.me.pay.entity.GiftBagRecord;
import com.yy.me.thread.ThreadUtil;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;

/**
 * Created by Chris on 16/5/6.
 */
@Service
public class GongHuiWebService {
    private static final Logger logger = LoggerFactory.getLogger(GongHuiWebService.class);

    private static final long EXCHANGE_LIMIT = 1000000L;

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private GongHuiMongoDBMapper gongHuiMapper;

    @Autowired
    private GongHuiAnchorMongoDBMapper gongHuiAnchorMapper;

    @Autowired
    private GongHuiRuleMongoDBMapper gongHuiRuleMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private GiftListMongoDBMapper giftListMapper;

    @Autowired
    private SendGiftService paymentService;

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    private PropsService propsService;

    private ExecutorService giftExecutor;

    @PostConstruct
    public void init() {
        giftExecutor = new ThreadUtil.CachedThreadPoolBuilder().setThreadFactory(ThreadUtil.buildThreadFactory("GongHui-sendGift-Executor"))
                .setMinSize(1).setMaxSize(50).build();
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.gracefulShutdown(giftExecutor, 1000);
    }

    public void getInfoJsonp(long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();

            // 获取账户余额
            GongHui gongHui = gongHuiMapper.findById(uid);
            if (gongHui == null) {
                logger.warn("User is not gonghui owner: " + uid);
                
                sendResponseAuto(request, response, genMsgObj(NOT_EXIST, "gonghui not exists."));
                return;
            }

            result.put("mibi", gongHui.getMibiBalance());
            result.put("cash", gongHui.getCashBalance());

            // 获取正在直播的公会主播
            List<ObjectNode> liveAnchors = Lists.newArrayList();

            Set<Long> anchorSet = gongHuiAnchorMapper.findAnchorByGH(uid);
            if (anchorSet != null && !anchorSet.isEmpty()) {
                List<Long> livingUserList = GeneralLiveShowClient.findLivingByUids(anchorSet);

                if (livingUserList != null && !livingUserList.isEmpty()) {
                    List<UserInfo> userInfoList = userHessianService.getClient().findUserListByUids(livingUserList,false);

                    if (userInfoList != null && !userInfoList.isEmpty()) {
                        for (UserInfo userInfo : userInfoList) {
                            ObjectNode anchor = getLocalObjMapper().createObjectNode();
                            anchor.put("uid", userInfo.getUid());
                            anchor.put("nick", userInfo.getNick());
                            liveAnchors.add(anchor);
                        }
                    }
                }
            }

            result.put("liveAnchors", liveAnchors);

            // 获取礼物信息
            List<ObjectNode> giftList = Lists.newArrayList();

            List<Gift> giftInfoList = giftListMapper.findAll();
            for (Gift gift : giftInfoList) {
                ObjectNode giftJo = getLocalObjMapper().createObjectNode();
                giftJo.put("propId", gift.getPropId());
                giftJo.put("propName", gift.getName());
                giftJo.put("supportCombo", gift.isCombo());

                giftList.add(giftJo);
            }

            result.put("gifts", giftList);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/getInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get gonghui info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/getInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get gonghui info error."));
        }
    }

    public void exchangeJsonp(long uid, long amount, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            // 获取账户信息
            GongHui gongHui = gongHuiMapper.findById(uid);
            if (gongHui == null) {
                logger.warn("User is not gonghui owner: " + uid);
                
                sendResponseAuto(request, response, genMsgObj(NOT_EXIST, "gonghui not exists."));
                return;
            }

            Map<String, Object> result = Maps.newHashMap();

            // 获取今日已兑换的M币数
            long todayExchanged = gongHuiRuleMapper.getExchangeAmount(uid);

            if (todayExchanged + amount > EXCHANGE_LIMIT) {
                logger.warn("Gonghui[{}] exchange exceed today limit. today echanged: {}, still need echange: {}", uid, todayExchanged, amount);

                result.put("limit", EXCHANGE_LIMIT);
                result.put("remain", (EXCHANGE_LIMIT - todayExchanged));

                sendResponseAuto(request, response, genMsgObj(EXCHANGE_EXCEEED, null, result));
                return;
            }

            // 计算M币余额和现金余额
            BigDecimal cashDecimal = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(0.1));
            double cash = doHalfUpRound(cashDecimal, 1).doubleValue();
            if (gongHui.getCashBalance() < cash) {
                logger.warn("Cash balance is not enough for exchange. balance: {}, exchange: {}", gongHui.getCashBalance(), cash);
                
                sendResponseAuto(request, response, genMsgObj(BALANCE_NOT_ENOUGH, "balance is not enough."));
                return;
            }

            long mibiBalance = gongHui.getMibiBalance() + amount;
            double cashBalance = BigDecimal.valueOf(gongHui.getCashBalance()).subtract(BigDecimal.valueOf(cash)).doubleValue();

            gongHui.setMibiBalance(mibiBalance);
            gongHui.setCashBalance(cashBalance);

            gongHuiMapper.save(gongHui);

            // 更新每日已兑换的数量
            gongHuiRuleMapper.incrExchangeAmount(uid, amount);

            result.put("mibi", mibiBalance);
            result.put("cash", cashBalance);

            logger.info("User[{}] exchanged cash: {}", uid, cash);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Exchange MiBi error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Exchange MiBi error."));
        }
    }

    public void sendGiftJsonp(final long uid, final long anchorUid, final int propId, final int propAmount, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            // 获取账户信息
            GongHui gongHui = gongHuiMapper.findById(uid);
            if (gongHui == null) {
                logger.warn("User is not gonghui owner: " + uid);
                
                sendResponseAuto(request, response, genMsgObj(NOT_EXIST, "gonghui not exists."));
                return;
            }

            // 检查礼物是否存在,以及是否支持连送
            final Gift gift = giftListMapper.findByPropId(propId);
            if (gift == null) {
                logger.warn("Gift not exists: " + propId);
                
                sendResponseAuto(request, response, genMsgObj(NOT_EXIST, "Gift not exists."));
                return;
            }

            if (gift.isCombo() == false && propAmount > 1) {
                logger.warn("Gift is not supoort combo. propId: {}, propAmount: {}", propId, propAmount);
                
                sendResponseAuto(request, response, genMsgObj(GIFT_COMBO_UNSUPPORTED, "Gift not support combo."));
                return;
            }

            // 检查M币余额是否足够
            long mibiBalance = gongHui.getMibiBalance() - gift.getPrice() * propAmount;
            if (mibiBalance < 0) {
                logger.warn("Mibi balance is not enough for buy gift. balance: {}, price: {}, amount: {}", gongHui.getMibiBalance(), gift.getPrice(), propAmount);
                
                sendResponseAuto(request, response, genMsgObj(MIBI_BALANCE_NOT_ENOUGH, "balance is not enough."));
                return;
            }

            // 获取直播信息
            final LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(anchorUid);
            if (liveShow == null) {
                logger.warn("Liveshow is stoped. anchor: {}", anchorUid);
                
                sendResponseAuto(request, response, genMsgObj(LS_STOP, "Liveshow is stoped."));
                return;
            }

            Map<String, Object> result = Maps.newHashMap();

            // 更新余额
            BigDecimal giftDecimal = BigDecimal.valueOf(gift.getPrice() * propAmount).multiply(BigDecimal.valueOf(0.1));
            BigDecimal calDecimal = BigDecimal.valueOf(gongHui.getCashBalance()).add(giftDecimal);
            double cashBalance = doHalfUpRound(calDecimal, 1).doubleValue();
            gongHui.setCashBalance(cashBalance);
            gongHui.setMibiBalance(mibiBalance);
            gongHuiMapper.save(gongHui);

            result.put("mibi", mibiBalance);
            result.put("cash", cashBalance);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/sendGift", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            final boolean reportSwitch = cntConfService.getGonghuiReportSwitch();

            // 送礼物
            giftExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String comboId = new ObjectId().toHexString();

                    for (int i = 0; i < propAmount; i++) {
                        long usedTime = System.currentTimeMillis();
                        String seq = uid + "_" + usedTime;

                        GiftBagRecord giftBag = new GiftBagRecord();
                        giftBag.setSeq(seq);
                        giftBag.setUid(uid);
                        giftBag.setRecvUid(anchorUid);
                        giftBag.setUsedTime(new Date(usedTime));
                        giftBag.setLid(liveShow.getLid());
                        giftBag.setPropId(propId);
                        giftBag.setPropCount(1);
                        giftBag.setIncome(gift.getIncome());
                        giftBag.setIntimacy(gift.getIntimacy());

                        try {
                            Map<String, Object> expand = Maps.newHashMap();
                            expand.put(GIFT_EXPAND_KEY_COMBO_ID, comboId);
                            expand.put(GIFT_EXPAND_KEY_LID, liveShow.getLid());
                            expand.put(GIFT_EXPAND_KEY_ROBOT, true);
                            giftBag.setExpand(getLocalObjMapper().writeValueAsString(expand));
                            
                            paymentService.sendGiftBroadcast(giftBag);

                            if (reportSwitch) {
                                propsService.reportPropsUsedForRank(uid, anchorUid, gift.getPrice(), gift.getIncome(), 1, seq, usedTime);
                            }

                            ThreadUtil.sleep(500);
                        } catch (Exception e) {
                            logger.error("GongHui send gift error.", e);
                        }
                    }
                }
            });

            metricsClient.report(MetricsClient.ProtocolType.INNER, "gonghui/sendGift", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Send gift error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "gonghui/sendGift", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Send gift error."));
        }
    }

    private BigDecimal doHalfUpRound(BigDecimal arg, int scale) {
        if (null == arg) {
            return null;
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale must be larger than zero");
        }
        BigDecimal result = arg.divide(BigDecimal.ONE, scale, BigDecimal.ROUND_HALF_UP);

        return result;
    }
}
