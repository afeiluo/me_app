package com.yy.me.web.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.AnchorService;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.anchor.family.entity.ResultInfo;
import com.yy.me.metrics.MetricsClient;

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

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by uid error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_FAIL);
        }

        return anchor;
    }

    public void remove(long uid) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "remove";

        try {
            ResultInfo resultInfo = getClient().remove(uid);
            if (resultInfo.getCode() != ResultInfo.SUCCESS.getCode()) {
                logger.warn("Rmove anchor by uid error. uid: {}, result: {}", uid, resultInfo);
            }

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by uid error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_FAIL);
        }
    }

    public List<Anchor> findByPage(String lastAnchorId, int size) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "findByPage";

        List<Anchor> anchorList = null;

        try {
            anchorList = getClient().findByPage(lastAnchorId, size);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by page error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_FAIL);
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
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by salary error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_FAIL);
        }

        if (anchorList == null) {
            anchorList = Lists.newArrayList();
        }

        return anchorList;
    }

    public List<Anchor> findByArrange(boolean arranged, String lastAnchorId, int size) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "findByArrange";

        List<Anchor> anchorList = null;

        try {
            anchorList = getClient().findByArranged(arranged, lastAnchorId, size);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by arrange error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, "Anchor", METRICS_URI, System.currentTimeMillis() - start,
                    MetricsClient.RESCODE_FAIL);
        }

        if (anchorList == null) {
            anchorList = Lists.newArrayList();
        }

        return anchorList;
    }
}
