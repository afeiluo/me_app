package com.yy.me.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.util.GeneralUtil;
import com.yy.me.web.service.SearchService;

import static com.yy.me.http.BaseServletUtil.*;

@RestController
@RequestMapping("/search")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    /**
     * 搜索用户
     * 
     * @param uid
     *            发起搜索操作的用户的uid
     * @param searchKey
     *            搜索关键字
     * @param page
     *            页码（从1开始）
     * @param request
     * @param response
     */
    @RequestMapping(value = "/searchUsers")
    public void searchUsers(@RequestParam long uid, @RequestParam String searchKey,
            @RequestParam(required = false, defaultValue = "1") int page, HttpServletRequest request,
            HttpServletResponse response) {
        if (StringUtils.isEmpty(searchKey)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, searchKey));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: searchKey is empty."));
            return;
        }

        if (page == 0) {
            page = 1;
        }

        searchService.searchUser(uid, StringUtils.trim(searchKey), page, request, response);
    }

}
