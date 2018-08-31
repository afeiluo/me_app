package com.yy.me.open.service;

import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.AnchorService;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.metrics.MetricsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Chris on 16/7/2.
 */
@Service
public class AnchorRpcService {
    private static final Logger logger = LoggerFactory.getLogger(AnchorRpcService.class);

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    @Qualifier("anchorServiceHessianClient")
    private ReferenceFactory<AnchorService> hessianFactory;

    private AnchorService getClient() {
        return hessianFactory.getClient();
    }

    public Anchor findByUid(long uid) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "findByUid";

        Anchor anchor = null;

        try {
            anchor = getClient().findByUid(uid);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by uid error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

        return anchor;
    }

    public List<Anchor> findByPage(String lastAnchorId, int size) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "findByPage";

        List<Anchor> anchorList = null;

        try {
            anchorList = getClient().findByPage(lastAnchorId, size);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by page error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

        if (anchorList == null) {
            anchorList = Lists.newArrayList();
        }

        return anchorList;
    }

    public List<Anchor> findBySalary(boolean hasSalary, String lastAnchorId, int size) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "findBySalary";

        List<Anchor> anchorList = null;

        try {
            anchorList = getClient().findBySalary(hasSalary, lastAnchorId, size);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by salary error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

        if (anchorList == null) {
            anchorList = Lists.newArrayList();
        }

        return anchorList;
    }
}
