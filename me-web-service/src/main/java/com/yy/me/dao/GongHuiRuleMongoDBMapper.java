package com.yy.me.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.yy.me.mongo.MongoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import static com.yy.me.mongo.MongoUtil.*;

/**
 * Created by Chris on 16/5/9.
 */
@Repository
public class GongHuiRuleMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(GongHuiRuleMongoDBMapper.class);

    public static final String COLLECTION_GONGHUI_RULE_NAME = "gonghui_rule";

    public static final String FIELD_GONGHUI_UID = "ghUid";
    public static final String FIELD_EXCHANGE_AMOUNT = "exchangeAmt";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void rebuildCollection() {
        // 删除公会规则表
        mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_RULE_NAME).drop();

        // 重新创建索引
        mongoTemplate
                .getDb()
                .getCollection(COLLECTION_GONGHUI_RULE_NAME)
                .createIndex(
                        new BasicDBObject(FIELD_GONGHUI_UID, 1), null, true);

        logger.info("Rebuild collection[{}] successful.", COLLECTION_GONGHUI_RULE_NAME);
    }

    public long getExchangeAmount(long ghUid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBObject dbObj = mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_RULE_NAME).findOne(new BasicDBObject(FIELD_GONGHUI_UID, ghUid));

            if (dbObj == null) {
                return 0L;
            }

            long amount = MongoUtil.getLong(dbObj.get(FIELD_EXCHANGE_AMOUNT), 0L);

            return amount;
        } finally {
            logger.info("getExchangeAmount - cost time {}ms, ghUid: {}", (System.currentTimeMillis() - start), ghUid);
        }
    }

    public void incrExchangeAmount(long ghUid, long amount) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBObject incObj = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_EXCHANGE_AMOUNT, amount));

            mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_RULE_NAME).update(new BasicDBObject(FIELD_GONGHUI_UID, ghUid), incObj, true, false, WriteConcern.UNACKNOWLEDGED);
        } finally {
            logger.info("incrExchangeAmount - cost time {}ms, ghUid: {}, amount: {}",
                    (System.currentTimeMillis() - start), ghUid, amount);
        }
    }
}
