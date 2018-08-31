package com.yy.me.web.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.yy.me.util.ContentMatcher;

import static org.junit.Assert.*;

public class ContentMatcherTest {

    @Test
    public void testIdCardMatch() {
        String cardNum = "436001198806110755";
        boolean result = ContentMatcher.checkIdCard(cardNum);
        assertTrue(result);

        cardNum = "43600119880611075X";
        result = ContentMatcher.checkIdCard(cardNum);
        assertTrue(result);

        cardNum = "436001198$0611075X";
        result = ContentMatcher.checkIdCard(cardNum);
        assertFalse(result);
    }

    @Test
    public void testAccountMatch() {
        String account = "13578664433";
        boolean result = ContentMatcher.checkAccount(account);
        assertTrue(result);

        account = "aaa23344@qq.com";
        result = ContentMatcher.checkAccount(account);
        assertTrue(result);

        account = "fefe$%*4234@qq.com";
        result = ContentMatcher.checkAccount(account);
        assertFalse(result);

        account = "y.fg1_+@ghh";
        result = ContentMatcher.checkAccount(account);
        assertTrue(result);

        account = "a$fe@gg";
        result = ContentMatcher.checkAccount(account);
        assertFalse(result);
    }

    @Test
    public void testContainPattern() {
        Pattern p = ContentMatcher.genContainPattern("中国");
        String test = "我是中国人";
        Matcher m = p.matcher(test);
        if (m.find()) {
            System.out.println("匹配成功");
        } else {
            System.out.println("匹配失败");
        }
    }
}
