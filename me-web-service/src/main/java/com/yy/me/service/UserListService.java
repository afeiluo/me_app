package com.yy.me.service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.yy.me.dao.UserListMongoDBMapper;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.time.MaskClock;

@Service
public class UserListService {
    private static final Logger logger = LoggerFactory.getLogger(UserListService.class);

    public static final long TO_BL=1l;
    public static final long TO_HIDE=2l;

    @Autowired
    private UserListMongoDBMapper userListMongoDBMapper;

    @Autowired
    private MetricsClient metricsClient;

    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("UserListCacheRefresher-pool-%d")
            .setDaemon(true).build();
    private ExecutorService parentExecutor = Executors.newCachedThreadPool(threadFactory);
    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(parentExecutor);
    private LoadingCache<Long, List<Long>> uidsCache = CacheBuilder.newBuilder().maximumSize(5)
            .refreshAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(5,TimeUnit.MINUTES)
            .build(new CacheLoader<Long, List<Long>>() {
                @Override
                public List<Long> load(Long key) throws Exception {
                    return userListMongoDBMapper.getAll(key);
                }

                @Override
                public ListenableFuture<List<Long>> reload(final Long key, List<Long> oldValue) throws Exception {
                    return executorService.submit(new Callable<List<Long>>() {
                        @Override
                        public List<Long> call() throws Exception {
                            return load(key);
                        }
                    });
                }
            });

    public List<Long> getUserList(long appid) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            return uidsCache.get(appid);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            rescode = MetricsClient.RESCODE_FAIL;
            return null;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.INNER, "userList", this.getClass(), "getList", 1, MaskClock.getCurtime() - t, rescode);
        }
    }


}
