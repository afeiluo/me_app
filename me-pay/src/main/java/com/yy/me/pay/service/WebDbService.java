package com.yy.me.pay.service;

import com.yy.cs.service.ImService;
import com.yy.cs.webdb.service.WebdbGatewayService;
import com.yy.cs.webdb.service.bean.UserInfo;
import com.yy.me.metrics.MetricsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Phil
 * @version 2017-01-05
 */
@Service
public class WebDbService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private WebdbGatewayService webdbGatewayService;

    @Autowired
    private ImService imService;

    public String getYyNick(long imId) throws Exception{
        long start = System.currentTimeMillis();

        try {
            long yyUid = imService.getUidByImid(imId);
            UserInfo info = webdbGatewayService.getUserNick(yyUid);

            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getYyNick", System.currentTimeMillis() - start, MetricsClient.RESCODE_SUCCESS);

            return info.getNick();
        } catch (Exception e) {
            metricsClient.report(MetricsClient.ProtocolType.THRIFT, "getYyNick", System.currentTimeMillis() - start, MetricsClient.RESCODE_FAIL);

            throw e;
        } finally {
            logger.info("Processed getYyNick request. yyUid:{}, cost time: {}ms", imId, (System.currentTimeMillis() - start));
        }
    }
}
