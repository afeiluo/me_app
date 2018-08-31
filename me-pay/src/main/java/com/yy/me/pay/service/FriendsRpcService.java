package com.yy.me.pay.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yy.me.friend.entity.FriendRelation;
import com.yy.me.friend.entity.LikeExtra;
import com.yy.me.friend.entity.LikeType;
import com.yy.me.friend.entity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.friend.FriendService;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.time.MaskClock;

@Service
public class FriendsRpcService {
    private static final Logger logger = LoggerFactory.getLogger(FriendsRpcService.class);

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<FriendService> hessianFactory;

    private FriendService getClient() {
        return hessianFactory.getClient();
    }

    public Set<Long> check(Long uid, List<Long> friendUids, Boolean active) {
        long startTime = MaskClock.getCurtime();
        String METRICS_URI = "Friends/check";

        Map<Long,FriendRelation> friendUidSet = null;

        try {
            friendUidSet = getClient().getUserRelations(uid, friendUids);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, MaskClock.getCurtime() - startTime,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Check friends error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, MaskClock.getCurtime() - startTime,
                    MetricsClient.RESCODE_FAIL);
        }
        
            return Sets.newHashSet();
    }

    public void superLike(Long uid, Long tid, String msg) {
        long startTime = MaskClock.getCurtime();
        String METRICS_URI = "Friends/superLike";

        try {
            Response response = getClient()
                    .like(uid, tid, LikeType.SUPER_LIKE, new LikeExtra.Builder().msg(msg).build());
            logger.info("superLike response:{}", response);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, MaskClock.getCurtime() - startTime,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("superLike error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, MaskClock.getCurtime() - startTime,
                    MetricsClient.RESCODE_FAIL);
        }

    }

}
