package com.yy.me.open.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.anchor.family.entity.AnchorRecommendType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ArrangeAnchorService {
    public static final Logger logger = LoggerFactory.getLogger(ArrangeAnchorService.class);

    private static String cacheKeyArrangeAnchor = "AllArrangeAnchor";
    private static String cacheKeySalaryAnchor = "salaryAnchor";

    @Autowired
    private AnchorRpcService anchorService;

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate generalMongoTemplate;

    private LoadingCache<String, Set<Long>> anchorCache = CacheBuilder.newBuilder().maximumSize(10).expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<Long>>() {
                @Override
                public Set<Long> load(String key) throws Exception {
                    if (StringUtils.equals(key, cacheKeyArrangeAnchor)) {
                        return getAllArrangeAnchor();
                    } else if (StringUtils.equals(key, cacheKeySalaryAnchor)) {
                        return getAllSalaryAnchor();
                    }
                    return null;
                }
            });

    /**
     * 获取所有签约主播
     * 
     * @return
     */
    public Set<Long> getAllArrangeAnchorWithCache() {
        Set<Long> arrangeAnchorSet = null;

        try {
            arrangeAnchorSet = anchorCache.get(cacheKeyArrangeAnchor);
        } catch (Exception e) {
            arrangeAnchorSet = Sets.newHashSet();

            logger.error("Get all arrange anchor from cache error.", e);
        }
        return arrangeAnchorSet;
    }

    public boolean isContractAnchor(Long uid) throws Exception {// 是否是签约主播
        Set<Long> anchorSet = getAllArrangeAnchorWithCache();
        if (anchorSet == null) {
            return false;
        } else {
            return anchorSet.contains(uid);
        }
    }

    /**
     * 采用同步方式 查询db获取所有签约主播
     * 
     * @return
     */
    private Set<Long> getAllArrangeAnchor() {
        Set<Long> set = Sets.newHashSet();

        try {
            int limit = 500;
            String lastAnchorId = null;

            List<Anchor> anchorList = anchorService.findByPage(lastAnchorId, limit);

            while (!anchorList.isEmpty()) {
                for (Anchor anchor : anchorList) {
                    if (anchor.getRecommandType() != AnchorRecommendType.NormalAnchor.getType()) {
                        set.add(anchor.getUid());
                    }
                }

                lastAnchorId = anchorList.get(anchorList.size() - 1).getAnchorId();

                anchorList = anchorService.findByPage(lastAnchorId, limit);
            }
        } catch (Exception e) {
            logger.error("Get all arrange anchor error.", e);
        }

        return set;
    }


    /**
     * 获取有底薪的主播
     * 
     * @return
     */
    public Set<Long> getAllSalaryAnchorWithCache() {
        Set<Long> salaryAnchorSet = null;

        try {
            salaryAnchorSet = anchorCache.get(cacheKeySalaryAnchor);
        } catch (Exception e) {
            salaryAnchorSet = Sets.newHashSet();

            logger.error("Get all salary anchor from cache error.", e);
        }
        return salaryAnchorSet;
    }

    /**
     * 采用同步方式 查询db获取所有签约主播
     * 
     * @return
     */
    private Set<Long> getAllSalaryAnchor() {
        Set<Long> set = Sets.newHashSet();

        try {
            boolean hasSalary = true;
            int limit = 500;
            String lastAnchorId = null;

            List<Anchor> anchorList = anchorService.findBySalary(hasSalary, lastAnchorId, limit);

            while (!anchorList.isEmpty()) {
                for (Anchor anchor : anchorList) {
                    set.add(anchor.getUid());
                }

                lastAnchorId = anchorList.get(anchorList.size() - 1).getAnchorId();

                anchorList = anchorService.findBySalary(hasSalary, lastAnchorId, limit);
            }
        } catch (Exception e) {
            logger.error("Get all salary anchor error.", e);
        }

        return set;
    }

}
