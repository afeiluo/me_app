package com.yy.me.web.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

/**
 * db.device_info.ensureIndex({"osType":1,"deviceId":1},{unique:true})
 * @author JCY
 *
 */
@Repository
public class DeviceMongoDBMapper {
    
    /**
     * 设备集合
     */
    public static final String COLLETION_DEVICE_INFO_NAME = "device_info";
    /**
     * 用户最近登录设备
     */
    public static final String COLLETION_USER_LOGIN_DEVICE = "user_login_device_info";
    
    public static final String FIELD_DEVICE_ID = "deviceId";
    public static final String FIELD_DEVICE_OS_TYPE = "osType";
    public static final String FIELD_DEVICE_BANED = "baned";
    public static final String FIELD_DEVICE_BANED_TYPE = "banedType";
    public static final String FIELD_DEVICE_BANED_ACTION_TIME = "banedActionTime";
    public static final String FIELD_DEVICE_BANED_END_TIME = "banedEndTime";
    public static final String FIELD_DEVICE_BANED_END_DONE = "banedEndDone";
    public static final String FIELD_DEVICE_UID = "uid";
    public static final String FIELD_DEVICE_LID = "lid";
    public static final String FIELD_UPDATE_TIME = "updateTime";

    @Autowired
    @Qualifier("mongoTemplateUser")
    private MongoTemplate mongoTemplate;

    /**
     * 
     * @param osType 操作系统（0代表Android，1代表iOS）
     * @param deviceId 设备ID
     * @throws Exception
     */
    public void insert(int osType, String deviceId) throws Exception {
        DBObject query = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
        DBObject update = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_CREATE_TIME, new Date()));
        mongoTemplate.getDb().getCollection(COLLETION_DEVICE_INFO_NAME)
                .update(query, update, true, false, WriteConcern.UNACKNOWLEDGED);
    }

    /**
     * 查询此deviceId是否被禁止
     * 
     * @param osType 操作系统（0代表Android，1代表iOS）
     * @param deviceId 设备ID
     * @return
     * @throws Exception
     */
    public DBObject checkBanned(int osType, String deviceId) throws Exception {
        DBObject query = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
        DBObject retObje = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_INFO_NAME).findOne(query);
        if (retObje != null && retObje.get(FIELD_DEVICE_BANED) instanceof Boolean && ((Boolean)retObje.get(FIELD_DEVICE_BANED))) {
            return retObje;
        }
        return null;
    }

    /**
     * 记录用户最近登录的设备信息
     * @param uid
     * @param osType
     * @param deviceId
     */
    public void insertUserLoginDevice(Long uid, int osType, String deviceId) {
        BasicDBObject update = new BasicDBObject(OP_SET,
                new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId).append(FIELD_UPDATE_TIME, new Date()));
        mongoTemplate.getDb().getCollection(COLLETION_USER_LOGIN_DEVICE).update(new BasicDBObject(FIELD_DEVICE_UID, uid),
                update, true, false, WriteConcern.UNACKNOWLEDGED);
    }

}
