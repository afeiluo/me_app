package com.yy.me.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.web.service.ActivityService;

@RestController
@RequestMapping("/activity")
public class ActivityController {
    
    @Autowired
    private ActivityService activityService;

    /**
     * 拉取后台配置的活动列表
     */
    @RequestMapping(value = "/getList")
    public void getList(@RequestParam Long uid,
                        HttpServletRequest request, HttpServletResponse response) {
        activityService.getValidActivityList(request, response);
    }


    /**
     * 单个主播的配置的营收活动入口
     */
    @RequestMapping(value = "/getAnchorActivity")
    public void getAnchorActivity(@RequestParam Long uid,@RequestParam Long anchorUid,
                        HttpServletRequest request, HttpServletResponse response) {
        activityService.getAnchorActivity(uid,anchorUid,request, response);
    }

}
