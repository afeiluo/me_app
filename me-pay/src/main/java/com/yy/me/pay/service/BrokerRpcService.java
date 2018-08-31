package com.yy.me.pay.service;

import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.BrokerService;
import com.yy.me.anchor.family.entity.Broker;
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
public class BrokerRpcService {
    private static final Logger logger = LoggerFactory.getLogger(BrokerRpcService.class);

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    @Qualifier("brokerServiceHessianClient")
    private ReferenceFactory<BrokerService> hessianFactory;

    private BrokerService getClient() {
        return hessianFactory.getClient();
    }

    public Broker findByBrokerId(String brokerId) {
        long start = System.currentTimeMillis();
        String METRICS_URI = "Broker/findByBrokerId";

        Broker broker = null;

        try {
            broker = getClient().findById(brokerId);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("Find broker by id error.", e);

            metricsClient.report(MetricsClient.ProtocolType.HTTP, METRICS_URI, System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);
        }

        return broker;
    }
}
