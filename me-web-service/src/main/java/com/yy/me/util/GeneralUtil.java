package com.yy.me.util;

import java.nio.charset.StandardCharsets;

import com.yy.me.json.JsonUtil;

public class GeneralUtil {
    
    public static String checkAndFilter(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        if (str.length() > maxLen) {
            return str.substring(0, maxLen);
        }
        return str;
    }
    
    /**
     * 不带换行
     * @param str
     * @param maxLen
     * @return
     */
    public static String checkAndFilterWithoutWrap(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        if (str.length() > maxLen) {
            return str.substring(0, maxLen);
        }
        return str.replaceAll("\n", "");
    }
    
    /**
     * 查找在转换为UTF-8后，在不超过maxByteLen字节情况下，求最长截断字符串
     * @param str
     * @return
     */
    public static String findMaxLengthStr(String str, int maxByteLen) {
        byte b[] = str.getBytes(StandardCharsets.UTF_8);
        int j = 0;
        for (int i = 0; i < b.length && j < maxByteLen; i++) {
            if (b[i] < 0x80 || b[i] >= 0xc0) {
                j++;
            }
        }
        return str.substring(0, j);
    }

    public static String genMethodParamStr(Object... objs) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        if (st == null || st.length <= 2) {
            return null;
        }
        StackTraceElement tmp = st[2];
        String method = tmp.getClassName() + "." + tmp.getMethodName();

        if (objs != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("(");
            appendObj(sb, JsonUtil.instance.toJson(objs[0]));
            for (int i = 1; i < objs.length; i++) {
                sb.append(", ");
                appendObj(sb, JsonUtil.instance.toJson(objs[i]));
            }
            sb.append(")" + ":" + tmp.getLineNumber() + "----");
            method += sb.toString();
        } else {
            method += "()" + ":" + tmp.getLineNumber() + "----";
        }

        return method;
    }
    
    private static void appendObj(StringBuffer sb, Object obj) {
        if (obj instanceof String) {
            sb.append("\"");
            sb.append(obj);
            sb.append("\"");
        } else if (obj instanceof Object[]) {
            for (Object subObj : (Object[]) obj) {
                appendObj(sb, subObj);
            }
        } else if (obj != null) {
            sb.append(obj.toString());
        } else {
            sb.append("null");
        }
    }
}
