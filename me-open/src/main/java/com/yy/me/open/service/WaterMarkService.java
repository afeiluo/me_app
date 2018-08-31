package com.yy.me.open.service;

import com.google.common.collect.Maps;
import com.yy.me.open.dao.LiveShowWaterMarkMongoDBMapper;
import com.yy.me.open.entity.LiveShowWaterMark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangke on 2016/4/21.
 */
@Service
public class WaterMarkService {

    private static Logger logger = LoggerFactory.getLogger(WaterMarkService.class);

    @Autowired
    private LiveShowWaterMarkMongoDBMapper waterMarkMongoDBMapper;

    public Map<LiveShowWaterMark, Set<Long>> getCurrentWaterMark() throws Exception {
        List<LiveShowWaterMark> list = waterMarkMongoDBMapper.getCurrentWaterMark();
        Map<LiveShowWaterMark, Set<Long>> retMap = Maps.newHashMap();
        for (LiveShowWaterMark waterMark : list) {
            if (waterMark.getAll() != null && waterMark.getAll()) {
                retMap.put(waterMark, null);
            } else {
                List<Long> whiteList = waterMark.getWhiteList();
                if (whiteList == null || whiteList.isEmpty()) {
                    continue;
                }
                Set<Long> whiteSet = new HashSet<>(whiteList);
                //清除内存引用
                waterMark.setWhiteList(null);
                retMap.put(waterMark, whiteSet);
            }
        }
        return retMap;
    }

    public void replaceWhiteList(String id, List<Long> uids) {
        waterMarkMongoDBMapper.insertWhiteList(id, uids);
    }

    public void addWhiteList(String id, List<Long> uids) {
        waterMarkMongoDBMapper.addWhiteList(id, uids);
    }

    public void delWhiteList(String id, List<Long> uids) {
        waterMarkMongoDBMapper.delWhiteList(id, uids);
    }
}
