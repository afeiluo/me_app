package com.yy.me.pay.service;

import static com.yy.me.dao.PaymentAccountMongoDBMapper.*;
import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.http.BaseServletUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.mongodb.DuplicateKeyException;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.anchor.family.entity.Broker;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.PaymentAccountMongoDBMapper;
import com.yy.me.entity.PaymentAccount;
import com.yy.me.json.JsonUtil;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.message.Message;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.message.MsgDataType;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.pay.dao.GiftBagRecordMongoDBMapper;
import com.yy.me.pay.entity.ExchangeConfig;
import com.yy.me.pay.entity.GiftBagRecord;
import com.yy.me.pay.entity.GiftCallbackReq;
import com.yy.me.pay.entity.ReceivedGiftHistory;
import com.yy.me.pay.entity.UserExchangeHistory;
import com.yy.me.pay.entity.UserIncomeHistory;
import com.yy.me.pay.entity.UserWithdrawHistory;
import com.yy.me.pay.util.PaymentTokenUtil;
import com.yy.me.service.inner.MessageService;
import com.yy.me.thread.ThreadUtil;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.tinytimes.thrift.turnover.currency.TCurrencyType;
import com.yy.tinytimes.thrift.turnover.currency.UsedChannelType;

@Service
public class PaymentWebService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentWebService.class);

    private final static int BULLET_PROP_ID = 210000;

    private Queue<GiftBagRecord> giftBagQueue = new LinkedBlockingQueue<GiftBagRecord>();

    @Autowired
    private PaymentAccountMongoDBMapper paymentAccountMapper;

    @Autowired
    private GiftBagRecordMongoDBMapper giftBagMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private MessageMongoDBMapper messageMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SettleService settleService;

    @Autowired
    private PropsService propsService;

    @Autowired
    private TurnOverRankService turnOverRankService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private SendGiftService paymentService;

    @Value(value = "#{settings['node.productEnv']}")
    private boolean productEnv;

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    private MetricsClient metricsClient;


    @Autowired
    @Qualifier("liveShowThriftService")
    private ReferenceFactory<LiveShowThriftService> liveShowThriftService;

    @Autowired
    private BrokerRpcService brokerService;

    @Autowired
    private AnchorRpcService anchorService;

    @Autowired
    private WebDbService webDbService;

    @Autowired
    private FriendsRpcService friendService;

    private LoadingCache<Long, RateLimiter> giftRateCache = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterAccess(3, TimeUnit.MINUTES).build(new CacheLoader<Long, RateLimiter>() {
                @Override
                public RateLimiter load(Long anchorUid) throws Exception {
                    Double rateLimit = null;

                    try {
                        rateLimit = cntConfService.getGiftBcRateLimit();
                    } catch (Exception e) {
                        logger.error("Get gift broadcast rate limit error.", e);
                    }

                    if (rateLimit == null) {
                        rateLimit = new Double(10);
                    }

                    return RateLimiter.create(rateLimit);
                }
            });

    private ExecutorService giftQueueThreadPool;
    
    private ExecutorService taskExecutor;

    @PostConstruct
    public void init() {
        int threadCount = 10;

        giftQueueThreadPool = new ThreadUtil.FixedThreadPoolBuilder()
                .setThreadFactory(ThreadUtil.buildThreadFactory("Gift-Processor")).setPoolSize(threadCount).build();

        for (int i = 0; i < threadCount; i++) {
            giftQueueThreadPool.execute(new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        GiftBagRecord giftBag = giftBagQueue.poll();
                        if (giftBag == null) {
                            ThreadUtil.sleep(100);
                            continue;
                        }

                        processGiftBag(giftBag);
                    }
                }
            });
        }
        
        taskExecutor = new ThreadUtil.CachedThreadPoolBuilder()
                .setThreadFactory(ThreadUtil.buildThreadFactory("TurnOver-Executor")).setMinSize(1).setMaxSize(300)
                .build();
    }

    @PreDestroy
    public void destroy() {
        int waitCount = 0;
        int duration = 10;

        while (giftBagQueue.size() > 0) {
            logger.info("Still has {} gift bag in queue need to process.", giftBagQueue.size());

            if (waitCount++ > 1000) {
                break;
            }

            ThreadUtil.sleep(duration);
        }

        ThreadUtil.gracefulShutdown(giftQueueThreadPool, 3000);
        ThreadUtil.gracefulShutdown(taskExecutor, 3000);
    }

    public void genToken(long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            String token = PaymentTokenUtil.genToken(productEnv, uid);

            Map<String, Object> result = Maps.newHashMap();
            result.put("uid", uid);
            result.put("token", token);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "genToken", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Generate payment token error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "genToken", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Generate payment token error."));
        }
    }

    public void getUserAccount(long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            PaymentAccount account = paymentAccountMapper.findById(uid);

            Map<String, Object> result = Maps.newHashMap();
            result.put(FIELD_PAYMENT_ACCOUNT_UID, uid);

            if (account != null) {
                result.put(FIELD_PAYMENT_ACCOUNT_NAME, account.getName());
                result.put(FIELD_PAYMENT_ACCOUNT_ID_CARD, account.getIdCard());
                result.put(FIELD_PAYMENT_ACCOUNT_NUM, account.getAccount());
                result.put(FIELD_PAYMENT_ACCOUNT_PHONE, account.getPhone());
            }

            Anchor anchor = anchorService.findByUid(uid);
            if (anchor != null && StringUtils.isNotBlank(anchor.getSignChannel())) {
                Broker broker = brokerService.findByBrokerId(anchor.getSignChannel());

                if (broker != null) {
                    result.put("brokerName", broker.getName());
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getUserAccount", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get payment account error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getUserAccount", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get payment account error."));
        }
    }

    public void saveAccount(PaymentAccount account, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            paymentAccountMapper.save(account);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "saveAccount", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Save payment account error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "saveAccount", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Save payment account error."));
        }
    }

    @SuppressWarnings("unchecked")
    public void receiveSuperLike(GiftCallbackReq giftReq, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        GiftBagRecord giftBag = new GiftBagRecord();

        try {
            giftBag.setSeq(giftReq.getSeq());
            giftBag.setUid(giftReq.getUid());
            giftBag.setRecvUid(giftReq.getRecvUid());
            giftBag.setUsedTime(new Date(giftReq.getUsedTimestamp()));
            giftBag.setPlatform(giftReq.getPlatform());
            giftBag.setExpand(giftReq.getExpand());

            HashMap<String, Object> expandMap = JsonUtil.instance.fromJson(giftReq.getExpand(), HashMap.class);
            String msg;
            if (expandMap == null || expandMap.isEmpty() || !expandMap.containsKey(GIFT_EXPAND_KEY_MSG)) {
                msg=null;
            }else {
                msg= (String) expandMap.get(GIFT_EXPAND_KEY_MSG);
            }

            try {
                friendService.superLike(giftBag.getUid(),giftBag.getRecvUid(),msg);
            } catch (Exception e) {
                logger.error("superLike error: " + giftBag, e);

                metricsClient.report(MetricsClient.ProtocolType.INNER, "receiveSuperLike_logic", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "receiveSuperLike", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            // 先返回响应给营收
            sendResponse(request, response, genMsgObj(SUCCESS, "success"));

        } catch (Exception e) {
            logger.error("Process received superLike error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "receiveSuperLike", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Process received superLike error."));
        }

    }

    @SuppressWarnings("unchecked")
    public void receiveGiftBag(GiftCallbackReq giftReq, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        GiftBagRecord giftBag = new GiftBagRecord();

        try {
            giftBag.setSeq(giftReq.getSeq());
            giftBag.setUid(giftReq.getUid());
            giftBag.setRecvUid(giftReq.getRecvUid());
            giftBag.setUsedTime(new Date(giftReq.getUsedTimestamp()));
            giftBag.setPlatform(giftReq.getPlatform());
            giftBag.setExpand(giftReq.getExpand());

            HashMap<String, Object> expandMap = JsonUtil.instance.fromJson(giftReq.getExpand(), HashMap.class);
            
            if (expandMap == null || expandMap.isEmpty() || !expandMap.containsKey(GIFT_EXPAND_KEY_LID)) {
                logger.error("Gift bag expand info is invalid. giftReq: {}", giftReq);
                
                sendResponse(request, response, genMsgObj(FAILED, "expand is invalid."));
                return;
            }
            String lid = expandMap.get(GIFT_EXPAND_KEY_LID).toString();
            giftBag.setLid(lid);

            if (expandMap.containsKey(GIFT_EXPAND_KEY_COMBO_ID)) {
                String comboId = expandMap.get(GIFT_EXPAND_KEY_COMBO_ID).toString();
                giftBag.setComboId(comboId);
            }

            int propCount = 0;
            int amount = 0;
            int income = 0;
            int intimacy = 0;
            List<GiftCallbackReq.UseInfo> useInfoList = giftReq.getUseInfos();
            for (GiftCallbackReq.UseInfo useInfo : useInfoList) {
                giftBag.setPropId(useInfo.getPropId());
                propCount += useInfo.getPropCount();
                income += useInfo.getIncome();
                intimacy += useInfo.getIntimacy();

                if (useInfo.getCurrencyType() == GiftCallbackReq.CurrencyType.MIBI.getValue()) {
                    amount = useInfo.getAmount();
                }
            }

            giftBag.setPropCount(propCount);
            giftBag.setAmount(amount);
            giftBag.setIncome(income);
            giftBag.setIntimacy(intimacy);

            // 先发送广播给客户端，不影响动画效果
            try {
                boolean sendBC = true;

                if (cntConfService.getGiftBcRateAnchors().contains(giftBag.getRecvUid())) {
                    RateLimiter rateLimit = giftRateCache.get(giftBag.getRecvUid());

                    if (!rateLimit.tryAcquire()) {
                        logger.warn("Exceed gift broadcast limit: {}, ignore gift bag: {}", rateLimit.getRate(), giftBag);

                        metricsClient.report(MetricsClient.ProtocolType.INNER, "receiveGiftBag_ExceedBCRate", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

                        sendBC = false;
                    }
                }

                if (sendBC) {
                    // 发送直播间广播
                    paymentService.sendGiftBroadcast(giftBag);
                }
            } catch (Exception e) {
                logger.error("Send gift bag broadcast to live room error: " + giftBag, e);

                metricsClient.report(MetricsClient.ProtocolType.INNER, "receiveGiftBag_BC", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "receiveGiftBag", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            // 先返回响应给营收
            sendResponse(request, response, genMsgObj(SUCCESS, "success"));

            if (giftBagQueue.size() >= 10000) {
                logger.error("Gift bag queue size exceed limit: {}, ignore gift bag: {}", giftBagQueue.size(), giftBag);

                metricsClient.report(MetricsClient.ProtocolType.INNER, "receiveGiftBag_ExceedLimit", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

                return;
            }

            giftBagQueue.add(giftBag);
        } catch (Exception e) {
            logger.error("Process received gift bag error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "receiveGiftBag", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Process received gift bag error."));
        }

    }

    private void processGiftBag(GiftBagRecord giftBag) {
        long start = System.currentTimeMillis();

        try {
            // 保存礼包信息
            try {
                giftBagMapper.save(giftBag);
            } catch (DuplicateKeyException e) {
                logger.warn("Giftbag already exist, ignore it. " + giftBag);

                return;
            } catch (Exception e) {
                logger.error("Save received gift bag error.", e);
            }


            // 弹幕不计算收益
            if (giftBag.getPropId() == BULLET_PROP_ID) {
                logger.info("Ignore to update bullet gift bag related info: {}", giftBag);

                return;
            }

            try {
                // 更新直播米豆收益
                liveShowThriftService.getClient().updateLsIncome(null, giftBag.getLid(), giftBag.getIncome());
            } catch (Exception e) {
                logger.error("Update live gift income error.", e);
            }

            metricsClient.report(MetricsClient.ProtocolType.INNER, "processGiftBag", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Process gift bag error.", e);

            metricsClient.report(MetricsClient.ProtocolType.INNER, "processGiftBag", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

    }

    public void unicastToUser(long uid, String data, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Message msg = new Message();
            msg.setUid(uid);

            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.U_PAY_REMOTE, null, data);
            // msgData.put(FIELD_MSG_UID, uid);
            msg.setMsgData(msgData);

            // 发送单播
            messageMapper.insertAndPushUserMsg(msg);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "unicastToUser", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS, "success"));
        } catch (Exception e) {
            logger.error("Unicast to user error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "unicastToUser", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Unicast to user error."));
        }

    }

    public void getMyIncomeDetail(final long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();
            
            Future<Long> todayBeansIncomeFuture = taskExecutor.submit(new Callable<Long>() {

                @Override
                public Long call() throws Exception {
                    try {
                        return turnOverRankService.getUserTodayBeansIncome(uid);
                    } catch (Exception e) {
                        logger.error("Query user today beans income error.", e);
                    }

                    return null;
                }
            });

            Future<Map<TCurrencyType, Long>> userAccountFuture = taskExecutor
                    .submit(new Callable<Map<TCurrencyType, Long>>() {

                        @Override
                        public Map<TCurrencyType, Long> call() throws Exception {
                            try {
                                return currencyService.getUserAccountInfo(uid);
                            } catch (Exception e) {
                                logger.error("Query user account info error.", e);
                            }

                            return null;
                        }
                    });
            
            Long todayBeansIncome = todayBeansIncomeFuture.get(2, TimeUnit.SECONDS);
            if (todayBeansIncome == null) {
                todayBeansIncome = 0L;
            }
            
            long totalBeansIncome = 0L;
            double cashBalance = 0L;
            double totalCashIncome = 0L;
            
            Map<TCurrencyType, Long> accountMap = userAccountFuture.get(2, TimeUnit.SECONDS);
            if (accountMap != null && !accountMap.isEmpty()) {
                Long amount = accountMap.get(TCurrencyType.TINY_TIME__EDOU);
                if (amount != null && amount > 0) {
                    totalBeansIncome = amount;
                }

                amount = accountMap.get(TCurrencyType.TINY_TIME__PROFIT);
                if (amount != null && amount > 0) {
                    BigDecimal decimal = BigDecimal.valueOf(amount / 100.0);
                    decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
                    cashBalance = decimal.doubleValue();
                }

                amount = accountMap.get(TCurrencyType.TINY_TIME__MI_DOU);
                if (amount != null && amount > 0) {
                    BigDecimal decimal = BigDecimal.valueOf(amount / 100.0);
                    decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
                    totalCashIncome = decimal.doubleValue();
                }
            }

            result.put("todayBeansIncome", todayBeansIncome);
            result.put("totalBeansIncome", totalBeansIncome);
            result.put("cashBalance", cashBalance);
            result.put("totalCashIncome", totalCashIncome);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMyIncomeDetail", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user income info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMyIncomeDetail", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user income info error."));
        }
    }

    public void getBeansHistory(long uid, int limit, long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            List<ReceivedGiftHistory> historyList = propsService.queryRecvGiftHistory(uid, limit, lastRecId);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getBeansHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, historyList));
        } catch (Exception e) {
            logger.error("Get user beans history error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getBeansHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user beans history error"));
        }
    }

    public void getIncomeHistory(final long uid, final int limit, final long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();

            Future<Anchor> anchorFuture = taskExecutor.submit(new Callable<Anchor>() {

                @Override
                public Anchor call() throws Exception {
                    Anchor anchor = null;
                    try {
                        anchor = anchorService.findByUid(uid);
                    } catch (Exception e) {
                        logger.error("Query anchor info error.", e);
                    }

                    return anchor;
                }
            });

            Future<List<UserIncomeHistory>> incomeHistoryFuture = taskExecutor
                    .submit(new Callable<List<UserIncomeHistory>>() {

                        @Override
                        public List<UserIncomeHistory> call() throws Exception {
                            try {
                                return currencyService.queryIncomeHistory(uid, limit, lastRecId);
                            } catch (Exception e) {
                                logger.error("Query user income history error.", e);
                            }

                            return null;
                        }
                    });

            Anchor anchor = anchorFuture.get(2, TimeUnit.SECONDS);
            boolean hasBroker = false;
            int apportionRation = 0;
            if (anchor != null) {
                if (StringUtils.isNotBlank(anchor.getSignChannel())) {
                    hasBroker = true;
                }

                if (anchor.getApportionRation() != null) {
                    apportionRation = anchor.getApportionRation();
                }
            }

            result.put("hasBroker", hasBroker);
            result.put("apportionRation", apportionRation);

            List<UserIncomeHistory> incomeHistoryList = incomeHistoryFuture.get(2, TimeUnit.SECONDS);
            if (incomeHistoryList == null) {
                incomeHistoryList = Lists.newArrayList();
            }
            result.put("incomeList", incomeHistoryList);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getIncomeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user beans history error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getIncomeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user beans history error."));
        }
    }

    public void getWithdrawInfo(final long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();

            Future<String> payAccountFuture = taskExecutor.submit(new Callable<String>() {

                @Override
                public String call() throws Exception {
                    try {
                        PaymentAccount payAccount = paymentAccountMapper.findById(uid);
                        if (payAccount != null) {
                            return maskAccount(payAccount.getAccount());
                        }
                    } catch (Exception e) {
                        logger.error("Query user payment account error.", e);
                    }

                    return null;
                }
            });

            Future<Map<TCurrencyType, Long>> userAccountFuture = taskExecutor
                    .submit(new Callable<Map<TCurrencyType, Long>>() {

                        @Override
                        public Map<TCurrencyType, Long> call() throws Exception {
                            try {
                                return currencyService.getUserAccountInfo(uid);
                            } catch (Exception e) {
                                logger.error("Query user account info error.", e);
                            }

                            return null;
                        }
                    });

            String payAccount = payAccountFuture.get(2, TimeUnit.SECONDS);
            if (payAccount != null) {
                result.put("account", payAccount);
            }

            double balance = 0;

            Map<TCurrencyType, Long> accountMap = userAccountFuture.get(2, TimeUnit.SECONDS);
            if (accountMap != null && !accountMap.isEmpty()) {
                Long amount = accountMap.get(TCurrencyType.TINY_TIME__PROFIT);
                if (amount != null && amount > 0) {
                    BigDecimal decimal = BigDecimal.valueOf(amount / 100.0);
                    decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
                    balance = decimal.doubleValue();
                }
            }

            result.put("balance", balance);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getWithdrawInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user withdraw info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getWithdrawInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user withdraw info error."));
        }
    }

    public void withdraw(long uid, double amount, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            PaymentAccount payAccount = paymentAccountMapper.findById(uid);
            if (payAccount == null) {
                logger.warn("Payment account not set for user: {}", uid);
                
                sendResponseAuto(request, response, genMsgObj(PAY_ACCOUNT_NOT_BINDING, "account not set."));
                return;
            }

            long withDrawAmont = new Double(amount * 100).longValue();
            int code = settleService.withdraw(payAccount, withDrawAmont);
            if (code != SUCCESS) {
                sendResponseAuto(request, response, genMsgObj(code, "withdraw error."));
                return;
            }

            Map<String, Object> result = Maps.newHashMap();
            result.put("account", maskAccount(payAccount.getAccount()));
            result.put("amount", amount);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "withdraw", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Withdraw account error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "withdraw", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Withdraw account error."));
        }
    }

    public void getWithdrawHistory(long uid, int limit, long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            List<UserWithdrawHistory> result = currencyService.queryWithdrawHistory(uid, limit, lastRecId);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getWithdrawHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user withdraw history error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getWithdrawHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user withdraw history error."));
        }
    }

    public void getExchangeHistory(long uid, int limit, long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            List<UserExchangeHistory> result = currencyService.queryExchangeHistory(uid, limit, lastRecId);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getExchangeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user exchange history error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getExchangeHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user exchange history error."));
        }
    }

    public void getExchangeInfo(final long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();

            Future<Map<TCurrencyType, Long>> userAccountFuture = taskExecutor
                    .submit(new Callable<Map<TCurrencyType, Long>>() {

                        @Override
                        public Map<TCurrencyType, Long> call() throws Exception {
                            try {
                                return currencyService.getUserAccountInfo(uid);
                            } catch (Exception e) {
                                logger.error("Query user account info error.", e);
                            }
                            return null;
                        }
                    });

            Future<List<ExchangeConfig>> exchangeFuture = taskExecutor.submit(new Callable<List<ExchangeConfig>>() {

                @Override
                public List<ExchangeConfig> call() throws Exception {
                    try {
                        return currencyService.queryExchangeConfigList();
                    } catch (Exception e) {
                        logger.error("Query exchange config list error.", e);
                    }
                    return null;
                }
            });

            long mbiBalance = 0L;
            double cashBalance = 0;
            Map<TCurrencyType, Long> accountMap = userAccountFuture.get(2, TimeUnit.SECONDS);
            if (accountMap != null && !accountMap.isEmpty()) {
                Long amount = accountMap.get(TCurrencyType.TINY_TIME__MI_BI);
                if (amount != null && amount > 0) {
                    mbiBalance = amount;
                }

                amount = accountMap.get(TCurrencyType.TINY_TIME__PROFIT);
                if (amount != null && amount > 0) {
                    BigDecimal decimal = BigDecimal.valueOf(amount / 100.0);
                    decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
                    cashBalance = decimal.doubleValue();
                }
            }

            result.put("mbiBalance", mbiBalance);
            result.put("cashBalance", cashBalance);

            List<ExchangeConfig> configList = exchangeFuture.get(2, TimeUnit.SECONDS);
            if (configList == null) {
                configList = Lists.newArrayList();
            }
            result.put("items", configList);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getExchangeInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user exchange info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getExchangeInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user exchange info error."));
        }
    }

    public void exchange(long uid, long itemId, String userIp, String deviceType, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            UsedChannelType channel = UsedChannelType.WEB;
            if ("iOS".equals(deviceType)) {
                channel = UsedChannelType.IOS;
            } else if ("Android".equals(deviceType)) {
                channel = UsedChannelType.ANDROID;
            }

            if(StringUtils.isEmpty(userIp)) {
                userIp = "0.0.0.0";
            }

            boolean result = currencyService.exchange(uid, itemId, userIp, channel);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            if (result) {
                sendResponseAuto(request, response, genMsgObj(SUCCESS, "success"));
            } else {
                sendResponseAuto(request, response, genMsgObj(FAILED, "exchange failed."));
            }

        } catch (Exception e) {
            logger.error("Exchange error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "exchange", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Exchange error."));
        }
    }

    public void genToken4RedEnvelope(long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            String token = PaymentTokenUtil.genEnvelopeToken(productEnv, uid, lid);

            Map<String, Object> result = Maps.newHashMap();
            result.put("uid", uid);
            result.put("token", token);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "genToken4RedEnvelope", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "genToken4RedEnvelope", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            logger.error("Generate envelope token error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "generate envelop token error."));
        }
    }

    public void getMyIncome(final long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newHashMap();

            long totalBeansIncome = 0L;

            Map<TCurrencyType, Long> accountMap = currencyService.getUserAccountInfo(uid);
            if (accountMap != null && !accountMap.isEmpty()) {
                Long amount = accountMap.get(TCurrencyType.TINY_TIME__EDOU);
                if (amount != null && amount > 0) {
                    totalBeansIncome = amount;
                }
            }

            result.put("totalBeansIncome", totalBeansIncome);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMyIncome", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user income info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMyIncome", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "Get user income error."));
        }
    }
    
    private String maskAccount(String realAccount) {
        if (StringUtils.isBlank(realAccount)) {
            return realAccount;
        }

        int signIndex = -1;
        if ((signIndex = StringUtils.indexOf(realAccount, "@")) != -1) {
            int maskLength = (signIndex + 1) / 2;

            String prefix = StringUtils.substringBeforeLast(realAccount, "@");

            String realStr = StringUtils.substring(prefix, 0, prefix.length() - maskLength);
            StringBuilder maskStr = new StringBuilder(128);
            maskStr.append(realStr);
            for (int i = 0; i < maskLength; i++) {
                maskStr.append("*");
            }
            maskStr.append(StringUtils.substring(realAccount, signIndex));

            return maskStr.toString();
        }

        StringBuilder maskStr = new StringBuilder(128);
        maskStr.append(StringUtils.substring(realAccount, 0, 3));
        maskStr.append("****");
        if (realAccount.length() > 7) {
            maskStr.append(StringUtils.substring(realAccount, 7));
        }

        return maskStr.toString();
    }

    public void turnoverBroadcast(ObjectNode msg, List<String> uidKeyList, List<Long> uidList,
            HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            fillUserInfo(msg, uidKeyList);
            // broadcast msg
            messageService.broadcastTurnoverMessage(getLocalObjMapper().writeValueAsString(msg), uidList);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "turnoverBroadcast", System.currentTimeMillis()
                    - start, MetricsClient.RESCODE_SUCCESS);

            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Broadcast turnover msg error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "turnoverBroadcast", System.currentTimeMillis()
                    - start, MetricsClient.RESCODE_FAIL);

            sendResponse(request, response, genMsgObj(FAILED, "broadcase error."));
        }
    }

    private void fillUserInfo(ObjectNode msg, List<String> uidKeyList) throws Exception {
        if (uidKeyList.isEmpty()) {
            return;
        }
        List<Long> uids = Lists.newArrayListWithCapacity(uidKeyList.size());
        Map<String, Long> key2Uid = new HashMap<>();
        for (String s : uidKeyList) {
            JsonNode element = msg.get(s);
            if (element != null) {
                long uid = element.asLong();
                uids.add(uid);
                key2Uid.put(s, uid);
            }
        }
        List<UserInfo> list = userHessianService.getClient().findUserListByUids(uids, false);
        Map<Long, UserInfo> map = Maps.uniqueIndex(list, new Function<UserInfo, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable UserInfo input) {
                return input.getUid();
            }
        });
        for (Map.Entry<String, Long> entry : key2Uid.entrySet()) {
            fillUserInfoValue(msg, entry.getKey(), map.get(entry.getValue()));
        }
    }

    private void fillUserInfoValue(ObjectNode msg, String uidKey, UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        int index = uidKey.indexOf("uid");
        if (index < 0) {
            return;
        }
        if (index == 0) {
            msg.put("nick", userInfo.getNick());
            msg.put("headerUrl", userInfo.getHeaderUrl());
        } else {
            String prefix = uidKey.substring(0, index);
            msg.put(prefix + "Nick", userInfo.getNick());
            msg.put(prefix + "HeaderUrl", userInfo.getHeaderUrl());
        }
    }

    public void getMBiBalance(final Long uid, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newLinkedHashMap();

            Future<Map<TCurrencyType, Long>> accountInfoFuture = taskExecutor.submit(new Callable<Map<TCurrencyType, Long>>() {
                @Override
                public Map<TCurrencyType, Long> call() throws Exception {
                    return currencyService.getUserAccountInfo(uid);
                }
            });

            UserInfo userInfo = getUserInfo(request);

            if(userInfo != null) {
                result.put("nick", userInfo.getNick());
                result.put("username", userInfo.getUsername());
                result.put("headerUrl", userInfo.getHeaderUrl());
            }

            Map<TCurrencyType, Long> accountInfo = accountInfoFuture.get(3, TimeUnit.SECONDS);
            if(accountInfo != null && accountInfo.containsKey(TCurrencyType.TINY_TIME__MI_BI)){
                result.put("miBi", accountInfo.get(TCurrencyType.TINY_TIME__MI_BI));
            }else{
                result.put("miBi", 0);
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMbi", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get user mbi error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getMbi", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get user mbi error."));
        }
    }

    public void getYYInfo(final long yy, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> result = Maps.newLinkedHashMap();

            result.put("yy", yy);
            result.put("yyNick", webDbService.getYyNick(yy));

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getYYInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(SUCCESS, null, result));
        } catch (Exception e) {
            logger.error("Get yy info error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "getYYInfo", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Get yy info error."));
        }
    }

    public void m2y(Long uid, String yy, Long amount, HttpServletRequest request, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        try {
            int errorCode = currencyService.m2y(UUID.randomUUID().toString(), uid, amount, yy);
            String message;
            if(errorCode != 1){
                switch (errorCode){
                    case -500:
                        message = "服务端异常";
                        break;
                    case -400:
                        message = "参数错误";
                        break;
                    case -405:
                        message = "流水号已存在";
                        break;
                    case -22:
                        message = "帐号不存在";
                        break;
                    case -23:
                        message = "余额不足";
                        break;
                    default:
                        message = "未知异常";
                }
            }else{
                message = "兑换成功";
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "m2y", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            sendResponseAuto(request, response, genMsgObj(errorCode, message));
        } catch (Exception e) {
            logger.error("Mbi to Ybi error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "m2y", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Mbi to Ybi error."));
        }
    }
}
