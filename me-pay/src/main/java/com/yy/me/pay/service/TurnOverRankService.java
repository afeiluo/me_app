package com.yy.me.pay.service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.me.metrics.MetricsClient;
import com.yy.tinytimes.thrift.turnover.rank.TRank;
import com.yy.tinytimes.thrift.turnover.rank.TRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Created by Chris on 16/4/5.
 */
@Service
public class TurnOverRankService {
    private static final Logger logger = LoggerFactory.getLogger(TurnOverRankService.class);

    @Autowired
    @Qualifier("turnOverRankThriftClient")
    private ReferenceFactory<TRankService> thriftFactory;

    @Autowired
    private MetricsClient metricsClient;

    private TRankService getClient() {
        return thriftFactory.getClient();
    }

    public long getUserTodayBeansIncome(long uid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            TRank rank = getClient().getOneRank("tinyTimePropsIncome", "incr", "day", true, String.valueOf(uid), 0);
            logger.info("Got user today beans income response from turnover system: " + rank);

            if (rank == null) {
                return 0L;
            }

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getUserTodayBeansIncome", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return rank.getValue();
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getUserTodayBeansIncome", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed turnover get user today beans income request. cost time: {}ms", (System.currentTimeMillis() - start));
        }
    }
}
