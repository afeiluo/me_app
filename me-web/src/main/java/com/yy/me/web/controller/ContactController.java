package com.yy.me.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yy.me.contact.entity.UploadContact;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.web.service.ContactService;
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

@RestController
@RequestMapping("/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    private final static Logger logger = LoggerFactory.getLogger(ContactController.class);

    /**
     * 上传通讯录
     */
    @RequestMapping(value = "/uploadContact")
    public void uploadContact(@RequestParam Long uid,@RequestParam String deviceId,@RequestParam int firstBucket,
                              HttpServletRequest request, HttpServletResponse response) {
        List<UploadContact> contactList = null;
        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                JsonNode tmp = dataNode.get("contactList");
                if (tmp != null && tmp.isArray() && ((ArrayNode) tmp).size() > 0) {
                    contactList = getLocalObjMapper().convertValue(tmp, new TypeReference<List<UploadContact>>() {
                    });
                } else {
                    logger.error("Empty contactList data in body,uid:{}", uid);
                    sendResponseAuto(request, response, genMsgObj(FAILED, "Empty contactList data in body"));
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
                return;
            }
        }
        contactService.uploadContact(uid,deviceId,firstBucket,contactList,request,response);
    }

    /**
     * 查找通讯录里已经使用ME的用户
     * @param uid
     * @param deviceId
     * @param request
     * @param response
     */
    @RequestMapping(value = "/findContactFriend")
    public void findContactFriend(@RequestParam Long uid,@RequestParam String deviceId,
                              HttpServletRequest request, HttpServletResponse response) {
        contactService.findContactFriend(uid,deviceId,request,response);
    }

    /**
     * 查找通讯录里没使用ME的用户
     * @param uid
     * @param deviceId
     * @param request
     * @param response
     */
    @RequestMapping(value = "/findContactNotFriend")
    public void findContactNotFriend(@RequestParam Long uid,@RequestParam String deviceId,
                                  HttpServletRequest request, HttpServletResponse response) {
        contactService.findContactNotFriend(uid,deviceId,request,response);
    }

    /**
     * 设置通讯录隐身开关
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/hideContact")
    public void hideContact(@RequestParam Long uid,@RequestParam Integer status,
                                     HttpServletRequest request, HttpServletResponse response) {
        contactService.hideContact(uid,status,request,response);
    }
    /**
     * 获取通讯录隐身开关
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getHideContact")
    public void getHideContact(@RequestParam Long uid,
                            HttpServletRequest request, HttpServletResponse response) {
        contactService.getHideContact(uid,request,response);
    }

    /**
     * 是否上传过通讯录
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/hasUpload")
    public void hasUpload(@RequestParam Long uid,@RequestParam String deviceId,
                               HttpServletRequest request, HttpServletResponse response) {
        contactService.hasUpload(uid,deviceId,request,response);
    }
    /**
     * 通讯录中有新好友用手机号绑定ME的提示
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getFriendTips")
    public void getFriendTips(@RequestParam Long uid,@RequestParam String deviceId,
                          HttpServletRequest request, HttpServletResponse response) {
        contactService.getFriendTips(uid,deviceId,request,response);
    }

}
