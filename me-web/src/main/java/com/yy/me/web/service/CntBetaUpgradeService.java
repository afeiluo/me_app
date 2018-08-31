package com.yy.me.web.service;

import com.yy.cnt.ControlCenterService;
import com.yy.cnt.domain.Constant;
import com.yy.cnt.listen.NodeChangCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Created by ben on 16/8/12.
 * 从配置中心获取配置的内部版本强升的参数
 */
@Service
public class CntBetaUpgradeService {
    private static final Logger logger = LoggerFactory.getLogger(CntBetaUpgradeService.class);

    @Autowired
    private ControlCenterService controlCenterService;

    private volatile Map<String, Object> updateConfMap = new HashMap<>();
    public static String KEY_ANDROIDDES = "androidDes";
    public static String KEY_IOSDES = "iosDes";
    public static String KEY_ANDROIDVER = "androidVer";
    public static String KEY_IOSVER = "iosVer";
    public static String KEY_ANDROIDDOWNLOADURL = "androidDownloadUrl";
    public static String KEY_IOSDOWNLOADURL="iosDownloadUrl";

    @PostConstruct
    public void init() throws IOException {
        String confStr = controlCenterService.get("beta-upgrade");
        loadData(confStr);
        addCallBack();
    }

    private void loadData(String confStr) throws IOException {
        Properties properties = new Properties();
        StringReader reader = new StringReader(confStr);
        properties.load(reader);
        reader.close();
        for (Object key : properties.keySet()) {
            updateConfMap.put((String) key, properties.get(key));
        }
    }

    private void addCallBack() {
        controlCenterService.addNodeDataCallback("beta-upgrade", new NodeChangCallback() {
            @Override
            public void callback(String key, Constant.EventType eventType, String data) {
                try {
                    logger.info("cntConf Event: key:{}, eventType:{}, data:{}", key, eventType, data);
                    switch (eventType) {
                        case DELETE:
                            break;
                        case DATA_CHANGE:
                            loadData(data);
                            break;
                        case CREATE:
                            loadData(data);
                            break;
                        case CHILDREN_CHANGE:
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    logger.error("cntConf Reload Error:{}", e.getMessage(), e);
                }
            }
        });
    }

    public Map<String, Object> getConfMap() {
        return updateConfMap;
    }
    public Map<String,Object> getAndroidConfMap(){
        Map<String,Object> retMap=new HashMap<>();
        retMap.put("des",updateConfMap.get(KEY_ANDROIDDES));
        retMap.put("ver",updateConfMap.get(KEY_ANDROIDVER));
        retMap.put("downloadUrl",updateConfMap.get(KEY_ANDROIDDOWNLOADURL));
        return retMap;
    }
    public Map<String,Object> getIosConfMap(){
        Map<String,Object> retMap=new HashMap<>();
        retMap.put("des",updateConfMap.get(KEY_IOSDES));
        retMap.put("ver",updateConfMap.get(KEY_IOSVER));
        retMap.put("downloadUrl",updateConfMap.get(KEY_IOSDOWNLOADURL));
        return retMap;
    }
    public Object getConf(String key) {
        return updateConfMap.get(key);
    }
}
