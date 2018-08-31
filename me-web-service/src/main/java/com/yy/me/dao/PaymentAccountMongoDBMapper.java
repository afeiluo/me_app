package com.yy.me.dao;

import static com.yy.me.mongo.MongoUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.yy.me.entity.PaymentAccount;

/**
 * 提现账户信息。
 * 
 * @author cuixiang
 *
 */
@Repository
public class PaymentAccountMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(PaymentAccountMongoDBMapper.class);
    
    /**
     * 提现账户
     */
    public static final String COLLECTION_PAYMENT_ACCOUNT_NAME = "payment_account";
    
    public static final String FIELD_PAYMENT_ACCOUNT_UID = "uid";
    public static final String FIELD_PAYMENT_ACCOUNT_NAME = "name";
    public static final String FIELD_PAYMENT_ACCOUNT_ID_CARD = "idCard";
    public static final String FIELD_PAYMENT_ACCOUNT_TYPE = "type";
    public static final String FIELD_PAYMENT_ACCOUNT_NUM = "account";
    public static final String FIELD_PAYMENT_ACCOUNT_PHONE = "phone";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(PaymentAccount account) throws Exception {
        if (account == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, account, "uid");
            mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACCOUNT_NAME).save(obj);
        } finally {
            logger.info("save - cost time {}ms, account: {}", (System.currentTimeMillis() - start), account);
        }
    }

    public PaymentAccount findById(long uid) throws Exception {
        if (uid <= 0L) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACCOUNT_NAME)
                    .findOne(new BasicDBObject(FIELD_OBJ_ID, uid));
            return db2JavaObj(mongoTemplate, PaymentAccount.class, obj, "uid");
        } finally {
            logger.info("findById - cost time {}ms, uid: {}", (System.currentTimeMillis() - start), uid);
        }
    }

}
