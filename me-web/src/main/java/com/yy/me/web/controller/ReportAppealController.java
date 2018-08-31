package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponseAuto;

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
import com.yy.me.web.service.ReportAppealService;

/**
 * 用户申诉
 * @author dudusmida
 *
 */
@RestController
@RequestMapping("/web/userAppeal")
public class ReportAppealController {
	
	private static Logger logger = LoggerFactory.getLogger(ReportAppealController.class);
	
	@Autowired
	private ReportAppealService reportAppealService;
	
	/**
	 * 用户提交申诉
	 * @param uid
	 * @param appealReason
	 * @param request
	 * @param response
	 */
	@RequestMapping("/postAppeal")
	public void postAppeal(@RequestParam long uid,@RequestParam(required = false) String appealReason,
			HttpServletRequest request, HttpServletResponse response){
		if (uid <= 0L) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, appealReason));
            sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0L || StringUtils.isBlank(appealReason)"));
            return;
        }
		if(appealReason==null){
			appealReason="";
		}
		
		reportAppealService.postAppeal(uid,appealReason,request,response);
	}
	

}
