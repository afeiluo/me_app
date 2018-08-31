package com.yy.me.web.util;

import java.net.URLDecoder;

import org.apache.commons.lang3.StringUtils;

import com.yy.me.http.login.UserLoginUtil;

/**
 * Created by wangke on 2016/5/13.
 */
public class CookieTest {
    public static void main(String[] args) throws Exception {
        String token="3FtEaH14k07rrSyC%2F4qYZIY0NTaKjm95%2CeyOo9F2S7LXqyETgAEZN6bRhYsdUQ3kFD7k1IuOslEN9Pr%2FbrHhyxBluhDts%20qjXSLOvz%2BGFvupvRyO9jz1MXjTOq9WzLc8N60NMVOihAkxc%3D";
        token = URLDecoder.decode(token, "utf-8");
        String[] tmp = token.split(",");
        if (tmp.length >= 2 && !StringUtils.isEmpty(tmp[0]) && !StringUtils.isEmpty(tmp[1])) {
            String cauth = tmp[0];
            String stgt = tmp[1];
            String[] auth = UserLoginUtil.auth4Uid(stgt, cauth);
            if (auth != null) {
                if (UserLoginUtil.isLegalTime(auth[0], auth[4], auth[5])) {// 部分path可不验证token时间是否过期，其他都需要
                    System.out.println("legal");
                    return;
                }
            }
        }
        System.out.println("illegal");
    }
}
