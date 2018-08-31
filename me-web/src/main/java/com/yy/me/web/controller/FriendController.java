package com.yy.me.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.web.service.FriendService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

import static com.yy.me.http.BaseServletUtil.*;

/**
 * 好友关系Controller
 * @author Phil
 * @version 2016-10-13
 */
@RestController
@RequestMapping("/friend")
public class FriendController {
    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService friendService;





    /**
     * 好友申请已读
     * <p>http://localhost:8083/friend/apply/markread?appId=100001&sign=&data={%22uid%22:100001414}&auth=no2</p>
     * @param uid 当前用户UID
     */
    @RequestMapping("/apply/markread")
    public void applyMarkRead(Long uid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0) {
            logger.warn("friend apply mark read req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.applyMarkRead(uid, request, response);
    }

    /**
     * 好友申请数量
     * <p>http://localhost:8083/friend/apply/count?appId=100001&sign=&data={%22uid%22:100001414}&auth=no2</p>
     * @param uid 当前用户UID
     */
    @RequestMapping("/apply/count")
    public void applyCount(Long uid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0) {
            logger.warn("friend apply count req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.applyCount(uid, request, response);
    }

    /**
     * 申请回复
     * <p>http://localhost:8083/friend/apply/reply?appId=100001&sign=&data={%22aid%22:%2258083d1b6305b2dbfb69f7fe%22,%22uid%22:100001417,%22friendUid%22:101000463,%22content%22:%22%E4%B8%8D%E8%AE%B0%E5%BE%97%E6%88%91%E4%BA%86%22}&auth=no2</p>
     * @param uid 当前用户UID
     * @param friendUid 好友UID
     * @param content 回复内容
     */
    @RequestMapping("/apply/reply")
    public void applyReply(String aid, Long uid, Long friendUid, String content, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || friendUid == null || friendUid == 0 || StringUtils.isEmpty(content) || (!StringUtils.isEmpty(content) && content.length() > 30)) {
            logger.warn("friend apply reply req param not right. uid: {}, friendUid: {}, content length: {}", uid, friendUid, StringUtils.isEmpty(content) ? 0 : content.length());
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || friendUid ==null || content == null || content's length > 30"));
            return;
        }

        friendService.applyReply(aid, uid, friendUid, content, request, response);
    }



    /**
     * 卡片列表
     * <p>http://localhost:8083/friend/cardList?appId=100001&sign=&data={%22uid%22:100001417,%22page%22:1,%22pageSize%22:20}&auth=no2</p>
     * @param uid 当前用户UID
     * @param lastId 拉取lastId之后的列表,第一次传0
     * @param size 页大小,0返回全部
     * @param tid 将tid的卡片放在返回列表第一位(用于查指定用户卡片)
     */
    @RequestMapping("/cardList")
    public void cardList(Long uid, @RequestParam(required = false, defaultValue = "0") Long lastId,
            @RequestParam(required = false, defaultValue = "200") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long tid,
            HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0) {
            logger.warn("friend apply list req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.cardList(uid, lastId, size, tid, request, response);
    }

    /**
     * 指定卡片
     * <p>http://localhost:8083/friend/card?appId=100001&sign=&data={%22uid%22:100001417,%22page%22:1,%22pageSize%22:20}&auth=no2</p>
     * @param uid 当前用户UID
     * @param tid 指定用户UID
     */
    @RequestMapping("/card")
    public void cardList(Long uid, Long tid,
            HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || tid == null || tid==0) {
            logger.warn("friend apply list req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.card(uid, tid, request, response);
    }

    /**
     * 略过卡片
     * <p>http://localhost:8083/friend/cardSkip?appId=100001&sign=&data={%22uid%22:100001417,%22page%22:1,%22pageSize%22:20}&auth=no2</p>
     * @param uid 当前用户UID
     * @param tid 消息来源uid
     */
    @RequestMapping("/cardSkip")
    public void cardSkip(Long uid,Long tid,
            HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0) {
            logger.warn("friend cardSkip req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.cardSkip(uid, tid, request, response);
    }

    /**
     * 删除好友
     * <p>http://localhost:8083/friend/remove?appId=100001&sign=&data={%22uid%22:100001417,%22friendUid%22:101000463}&auth=no2</p>
     * @param uid 当前用户UID
     * @param friendUid 好友UID
     */
    @RequestMapping("/remove")
    public void remove(Long uid, Long friendUid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || friendUid == null || friendUid == 0) {
            logger.warn("friend remove req param not right. uid: {}, friendUid: {}", uid, friendUid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || friendUid == null"));
            return;
        }

        friendService.remove(uid, friendUid, request, response);
    }



    /**
     * 拉黑好友
     * <p>http://localhost:8083/friend/pullblack?appId=100001&sign=&data={%22uid%22:100001414,friendUid:201000710}&auth=no2</p>
     * @param uid 当前用户UID
     * @param friendUid 好友UID
     */
    @RequestMapping("/pullblack")
    public void pullBlack(Long uid, Long friendUid,String lid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || friendUid == null || friendUid == 0) {
            logger.warn("friend pull black req param not right. uid: {}, friendUid: {}", uid, friendUid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || friendUid == null"));
            return;
        }

        friendService.pullBlack(uid, friendUid, lid, request, response);
    }

    /**
     * 解除拉黑
     * <p>http://localhost:8083/friend/pullblack?appId=100001&sign=&data={%22uid%22:100001414,friendUid:201000710}&auth=no2</p>
     * @param uid 当前用户UID
     * @param tid 要解除的tid
     */
    @RequestMapping("/cancelBlack")
    public void cancelBlack(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || tid == null || tid == 0) {
            logger.warn("friend cancelBlack req param not right. uid: {}, tid: {}", uid, tid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || tid == null"));
            return;
        }

        friendService.cancelBlack(uid, tid, request, response);
    }

    /**
     * 拉黑列表
     * <p>http://localhost:8083/friend/pullblack?appId=100001&sign=&data={%22uid%22:100001414,friendUid:201000710}&auth=no2</p>
     * @param uid 当前用户UID
     * @param lastTime 上一页最后一个记录的createTime
     * @param limit 返回数量，最大100
     */
    @RequestMapping("/blackList")
    public void blackList(Long uid, Long lastTime, int limit, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || limit <=0 ) {
            logger.warn("friend cancelBlack req param not right. uid: {}, limit: {}", uid, limit);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || limit == null"));
            return;
        }
        if(lastTime==null){
            lastTime=0L;
        }
        if(limit>100){
            limit=100;
        }
        friendService.blackList(uid, lastTime, limit, request, response);
    }

    /**
     * 好友列表
     * <p>http://localhost:8083/friend/list?appId=100001&sign=&data={%22uid%22:100001417,%22near%22:1,%22page%22:1,%22pageSize%22:20}&auth=no2</p>
     * @param uid 当前用户UID
     * @param near 亲密好友（0-非亲密，1-亲密）
     * @param page 当前页
     * @param pageSize 页大小
     */
    @RequestMapping("/list")
    public void list(Long uid, Integer near, @RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false, defaultValue = "200") Integer pageSize, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0) {
            logger.warn("friend list req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.list(uid, near, page, pageSize, request, response);
    }

    /**
     * 好友列表(所有好友，不区分亲密/非亲密好友)-暂时不使用
     * <p>http://localhost:8083/friend/listall?appId=100001&sign=&data={%22uid%22:100001417,%22near%22:1,%22page%22:1,%22pageSize%22:20}&auth=no2</p>
     * @param uid 当前用户UID
     */
    /*@RequestMapping("/listall")
    public void listAll(Long uid, @RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false, defaultValue = "200") Integer pageSize, HttpServletRequest request, HttpServletResponse response){
        if (uid == null) {
            logger.warn("friend list req param not right. uid: {}", uid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null"));
            return;
        }

        friendService.list(uid, -1, page, pageSize, request, response);
    }*/

    /**
     * 检测是否好友
     * <p>http://localhost:8083/friend/check?appId=100001&sign=&data={%22uid%22:100001417,%22uids%22:[100002511,100001416,101000463,100001410,100001418,201000735,100001408]}&auth=no2</p>
     * @param uid 当前用户UID
     * @param near 亲密好友（0-非亲密，1-亲密）(Optional)
     * @param uids 检测UID列表
     */
    @RequestMapping("/check")
    public void check(Long uid, @RequestParam(required = false) Integer near, HttpServletRequest request, HttpServletResponse response){
        NYYHttpRequestWrapper nyyRequest = (NYYHttpRequestWrapper) request;
        JsonNode dataNode = nyyRequest.getDataNode();
        ArrayNode uidNodes = (ArrayNode) dataNode.get("uids");

        if (uid == null || uid == 0 || uidNodes == null) {
            logger.warn("friend check req param not right. uid: {}, uids: {}", uid, uidNodes);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || uids == null"));
            return;
        }

        List<Long> uids = Lists.newArrayList();
        for (JsonNode objNode : uidNodes) {
            if(objNode != null)
                uids.add(Long.parseLong(objNode.toString()));
        }

        friendService.check(uid, uids, request, response);
    }

    /**
     * 喜欢/取消喜欢好友
     * <p>http://localhost:8083/friend/like?appId=100001&sign=&data={"uid":201000883,"friendUid":101000860,"cancel":true}&auth=no2</p>
     * @param uid 当前用户UID
     * @param friendUid 好友UID
     * @param cancel 是否取消（可选，默认不取消，即喜欢）
     * @since App(3.1)
     */
    @RequestMapping("/like")
    public void like(Long uid, Long friendUid, @RequestParam(required = false, defaultValue = "false") Boolean cancel,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "") String name,
            HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || friendUid == null || friendUid == 0) {
            logger.warn("friend like req param not right. uid: {}, friendUid: {}", uid, friendUid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || friendUid == null"));
            return;
        }

        friendService.like(uid, friendUid, cancel, from,name,request, response);
    }

    @RequestMapping("/canSuperLike")
    public void canSuperLike(Long uid, Long tid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || tid == null || tid == 0) {
            logger.warn("friend canSuperLike req param not right. uid: {}, tid: {}", uid, tid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || tid == null"));
            return;
        }

        friendService.canSuperLike(uid, tid, request, response);
    }

    /**
     * 获取好友信息
     * <p>http://localhost:8083/friend/like?appId=100001&sign=&data={"uid":201000883,"friendUid":101000860,"cancel":true}&auth=no2</p>
     * @param uid 当前用户UID
     * @param friendUid 好友UID
     * @since App(3.1)
     */
    @RequestMapping("/info")
    public void info(Long uid, Long friendUid, HttpServletRequest request, HttpServletResponse response){
        if (uid == null || uid == 0 || friendUid == null || friendUid == 0) {
            logger.warn("friend info req param not right. uid: {}, friendUid: {}", uid, friendUid);
            sendResponse(request, response, genMsgObj(FAILED, "req param not right: uid == null || friendUid == null"));
            return;
        }

        friendService.info(uid, friendUid, request, response);
    }
}
