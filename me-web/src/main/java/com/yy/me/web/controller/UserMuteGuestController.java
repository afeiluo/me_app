package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.web.service.UserMuteGuestService;

/**
 * 禁言
 * @author Jiang Chengyan
 *
 */
@RestController
@RequestMapping("/user/muteGuest")
public class UserMuteGuestController {

    @Autowired
    private UserMuteGuestService userMuteGuestService;

    /**
     * 禁言
     * http://61.147.186.63:8081/user/muteGuest/mute?auth=no&appId=100001&sign=&data={%22uid%22:1000008,%22guestUid%22:100000904,%22lid%22:%2256dd41360000c963c0739605%22}
     * @param uid 用户id
     * @param guestUid 被禁言的用户id
     * @param lid （可选）在直播中禁言
     * @param request
     */
    @RequestMapping(value = "/mute")
    public void mute(@RequestParam long uid, @RequestParam long guestUid, @RequestParam(required = false) String lid, HttpServletRequest request, HttpServletResponse response) {
        userMuteGuestService.mute(getUserInfo(request), guestUid, lid, request, response);
    }

    /**
     * 取消禁言
     * http://61.147.186.63:8081/user/muteGuest/cancelMute?appId=100001&sign=&data=%7B%22uid%22%3A1000008%2C%22guestUid%22%3A1000009%7D
     * @param uid 用户id
     * @param guestUid 被禁言的用户id
     * @param request
     */
    @RequestMapping(value = "/cancelMute")
    public void cancelMute(@RequestParam long uid, @RequestParam long guestUid, HttpServletRequest request, HttpServletResponse response) {
        userMuteGuestService.cancelMute(getUserInfo(request), guestUid, request, response);
    }
}
