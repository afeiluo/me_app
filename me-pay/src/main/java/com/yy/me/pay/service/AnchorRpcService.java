package com.yy.me.pay.service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.AnchorService;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.metrics.MetricsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
        String METRICS_URI = "Anchor/findByUid";

        Anchor anchor = null;

        try {
            anchor = getClient().findByUid(uid);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find anchor by uid error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

        return anchor;
    }
}
