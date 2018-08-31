package com.yy.me.pay.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.pay.entity.ReceivedGiftHistory;
import com.yy.me.thread.ThreadUtil;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.tinytimes.thrift.turnover.props.TAppId;
import com.yy.tinytimes.thrift.turnover.props.TCurrencyType;
import com.yy.tinytimes.thrift.turnover.props.TPropsService;
import com.yy.tinytimes.thrift.turnover.props.TWeekPropsRecvInfo;

@Service
public class PropsService {
    private static final Logger logger = LoggerFactory.getLogger(PropsService.class);

    @Autowired
    @Qualifier("turnOverPropsThriftClient")
    private ReferenceFactory<TPropsService> thriftFactory;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;
    
    @Autowired
    private FriendsRpcService friendsService;

    @Autowired
    private MetricsClient metricsClient;
    
    private ThreadFactory threadFactory =ThreadUtil.buildThreadFactory("GiftPicRefresher-pool", true);
    private ExecutorService parentExecutor = new ThreadUtil.CachedThreadPoolBuilder().setThreadFactory(threadFactory).build();
    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(parentExecutor);
    
    private String CACHE_KEY = "gift_cache";
    
    private LoadingCache<String, Map<Integer, String>> giftPicCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(3, TimeUnit.MINUTES).build(new CacheLoader<String, Map<Integer, String>>() {
                @Override
                public Map<Integer, String> load(String key) throws Exception {
                    return queryGiftPic();
                }

                @Override
                public ListenableFuture<Map<Integer, String>> reload(final String key, Map<Integer, String> oldValue)
                        throws Exception {
                    ListenableFuture<Map<Integer, String>> listenableFuture = executorService
                            .submit(new Callable<Map<Integer, String>>() {

                                @Override
                                public Map<Integer, String> call() throws Exception {
                                    return load(key);
                                }
                            });
                    return listenableFuture;
                }
            });

    private TPropsService getClient() {
        return thriftFactory.getClient();
    }

    public List<ReceivedGiftHistory> queryRecvGiftHistory(long uid, int limit, long lastId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            List<TWeekPropsRecvInfo> recvInfoList = getClient().getLastWeekRecvPropsRecByUid(uid, TAppId.TINY_TIME, 1,
                    limit, lastId);
            logger.info("Got gift history response from turnover system. cost time: {}ms",
                    (System.currentTimeMillis() - start));

            List<ReceivedGiftHistory> historyList = Lists.newArrayList();
            if (recvInfoList != null && !recvInfoList.isEmpty()) {
                Map<Integer, String> giftInfo = giftPicCache.get(CACHE_KEY);
                
                List<Long> uidList = Lists.transform(recvInfoList, new Function<TWeekPropsRecvInfo, Long>() {
                    @Override
                    public Long apply(TWeekPropsRecvInfo recvInfo) {
                        return recvInfo.getUid();
                    }
                });

                List<UserInfo> userInfoList = userHessianService.getClient().findUserListByUids(uidList,false);

                Map<Long, UserInfo> userInfoMap = Maps.newHashMapWithExpectedSize(userInfoList.size());
                for (UserInfo userInfo : userInfoList) {
                    userInfoMap.put(userInfo.getUid(), userInfo);
                }
                
                Set<Long> friends = friendsService.check(uid, uidList, null);

                for (TWeekPropsRecvInfo recvInfo : recvInfoList) {
                    ReceivedGiftHistory history = new ReceivedGiftHistory();
                    history.setBagId(recvInfo.getId());
                    history.setUid(recvInfo.getUid());
                    history.setPropId(recvInfo.getPropId());
                    history.setPropName(recvInfo.getPropName());
                    
                    if (giftInfo != null && giftInfo.containsKey(recvInfo.getPropId())) {
                        history.setPropUrl(giftInfo.get(recvInfo.getPropId()));
                    }
                    
                    history.setUsedTime(recvInfo.getUsedTime());
                    history.setPropCount(recvInfo.getPropCnt());
                    history.setIncome(recvInfo.getSumAmount());

                    UserInfo userInfo = userInfoMap.get(recvInfo.getUid());
                    if (userInfo != null) {
                        history.setNick(userInfo.getNick());
                        history.setHeaderUrl(userInfo.getHeaderUrl());
                        history.setMedal(userInfo.getTopMedal());
                    }
                    
                    if (friends.contains(recvInfo.getUid())) {
                        history.setFriend(true);
                    } else {
                        history.setFriend(false);
                    }

                    historyList.add(history);
                }
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryRecvGiftHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return historyList;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryRecvGiftHistory", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover gift history request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }
    
    public Map<Integer, String> queryGiftPic() throws Exception {
        long start = System.currentTimeMillis();
        
        try {
            Map<Integer, String> result = getClient().getPropsUrl(TAppId.TINY_TIME, null, null);
            logger.info("Got gift info response from turnover system. cost time: {}ms",
                    (System.currentTimeMillis() - start));
            
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryGiftPic", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
            
            return result;
        }catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "queryGiftPic", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover gift info request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

    public boolean reportPropsUsedForRank(long sendUid, long recvUid, int price, int income, int propCount, String seq, long usedTime) throws Exception {
        long start = System.currentTimeMillis();

        try {
            int code = getClient().reportPropsUsedForRank(sendUid, recvUid, TAppId.TINY_TIME, TCurrencyType.TINY_TIME__MI_BI, price, propCount, income, 1, usedTime, seq, 0, 0);

            boolean result = false;
            if (code == 1) {
                result = true;
                logger.info("Report props used to turnover rank success. sendUid: {}, recvUid: {}, price: {}, income: {}, propCount: {}, usedTime: {}", sendUid, recvUid, price, income, propCount, usedTime);
            } else if (code == -405) {
                logger.info("Fail to report props used to turnover rank due to seq exists. sendUid: {}, recvUid: {}, price: {}, income: {}, propCount: {}, usedTime: {}", sendUid, recvUid, price, income, propCount, usedTime);
            } else if (code == -400) {
                logger.info("Fail to report props used to turnover rank due to parameter error. sendUid: {}, recvUid: {}, price: {}, income: {}, propCount: {}, usedTime: {}", sendUid, recvUid, price, income, propCount, usedTime);
            } else if (code == -500) {
                logger.info("Fail to report props used to turnover rank due to system error. sendUid: {}, recvUid: {}, price: {}, income: {}, propCount: {}, usedTime: {}", sendUid, recvUid, price, income, propCount, usedTime);
            } else {
                logger.info("Fail to report props used to turnover rank. sendUid: {}, recvUid: {}, price: {}, income: {}, propCount: {}, usedTime: {}", sendUid, recvUid, price, income, propCount, usedTime);
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "reportPropsUsedForRank", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return result;
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "reportPropsUsedForRank", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed report props used to turnover rank request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }

}
