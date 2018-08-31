package com.yy.me.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

public class ContentMatcher {
    // #([^\s@#]+)(\s|$)
    private static String topicPattern = "#([^@#\\*\\s]+)(\\s|$)";// 话题（不包含@）

    // (?<=@)(?P<json>{.*})(?=@})
    private static String atPattern = "(?<=@)(\\{.*?\\})(?=@\\})";// @

    private static Pattern idCardPattern = Pattern.compile("[1-9]\\d{13,16}[0-9xX]{1}"); // 15-18位，最后一位可能是字母X

    private static Pattern accountPatter = Pattern.compile("[a-zA-Z0-9.@_+-]+"); // 简单的账户判断，主要针对手机号和邮箱

    /**
     * $符号会影响replacement!!!
     */
    public static String replaceAllPatternCharsExceptStar(String input) {
        return input.replaceAll("/", "").replaceAll("\\?", "").replaceAll("\\^", "").replaceAll("\\.", "").replaceAll("\\$", "\\\\\\$");
    }

    public static String replaceAllPatternChars(String input) {
        return input.replaceAll("/", "").replaceAll("\\*", "").replaceAll("\\?", "").replaceAll("\\^", "").replaceAll("\\.", "")
                .replaceAll("\\$", "\\\\\\$");
    }

    public static Pattern genStartLikePattern(String content) {
        // String pattern = "/^" + content + ".*/";
        String pattern = "^" + content + ".*";
        return Pattern.compile(pattern);
    }

    public static Pattern genContainPattern(String content) {
        // String pattern = "/^" + content + ".*/";
        String pattern = ".*" + content + ".*";
        return Pattern.compile(pattern);
    }

    public static List<String> matchTopic(String contentText) {
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(topicPattern);
        // 现在创建 matcher 对象
        Matcher m = r.matcher(contentText);
        List<String> ret = Collections.emptyList();
        while (m.find()) {
            if (ret.isEmpty()) {
                ret = Lists.newArrayList();
            }
            ret.add(m.group(1));
        }
        return ret;
    }

    public static List<String> matchAt(String contentText) {
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(atPattern);
        // 现在创建 matcher 对象
        Matcher m = r.matcher(contentText);
        List<String> ret = Collections.emptyList();
        while (m.find()) {
            if (ret.isEmpty()) {
                ret = Lists.newArrayList();
            }
            ret.add(m.group(1));
        }
        return ret;
    }

    public static boolean checkIdCard(String idCard) {
        Matcher matcher = idCardPattern.matcher(idCard);

        return matcher.matches();
    }

    public static boolean checkAccount(String account) {
        Matcher matcher = accountPatter.matcher(account);

        return matcher.matches();
    }

    public static String replaceAt(String contentText, List<String> replaceStr) {
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(atPattern);
        // 现在创建 matcher 对象
        Matcher m = r.matcher(contentText);
        Iterator<String> it = replaceStr.iterator();
        StringBuffer sb = new StringBuffer();
        int lastIndex = -1;
        while (m.find() && it.hasNext()) {
            lastIndex = m.end();
            String tmp = it.next();
            m.appendReplacement(sb, tmp);
        }
        if (lastIndex >= 0) {// 最后至少有2个字符
            sb.append(contentText.substring(lastIndex));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // System.out.println(matchTopic("#**newTop"));
        // String p = "our://([0-9A-Za-z]+)/([0-9A-Za-z]+)";
        // Pattern r = Pattern.compile(p);
        // // 现在创建 matcher 对象
        // Matcher m = r.matcher("our://123456/54212");
        // while (m.find()) {
        // for (int i = 1; i <= m.groupCount(); i++) {
        // System.out.println(m.group(i));
        // }
        // }
        Pattern p = genContainPattern("中国");
        String test = "我是中国人";
        Matcher m = p.matcher(test);
        if (m.find()) {
            System.out.println("匹配成功");
        } else {
            System.out.println("匹配失败");
        }
        // System.out.println(replaceAllPatternChars("/^sad.*/"));
        // System.out.println(matchAt("@@} "));
        // System.out.println(matchAt("@@{uid:1000008,nickname:\"昵称\"}@}xxxxxx@}"));
        // System.out.println(matchAt("@{uid:1000008,nickname:\"昵称\"}@}xxxxxx@}"));
        // System.out.println(matchTopic("#newTop. test"));
        // System.out.println(new Locale("zh-Hant_CN"));
        // System.out.println(matchTopic("zxkcvhlk;ajsdf#aldskfjas asd;flkjavnzxcm,.vnafj##jlksa aaa#ooo"));
        // System.out.println(genStartLikePattern("haha"));
        // System.out.println(replaceAt("asdfaaaaaa@{uid:1000008,nickname:\"昵称\"}@}kxxxxxx@}cbbbbpxxxx",
        // Lists.newArrayList("{\"uid\":1000009}","")));
        // System.out.println(replaceAt("asdfaaaaaa@{uid:1000008,nickname:\"昵称\"}@}kxxxxxx@}cbbbb@{dssssss}@}pxxxx",
        // Lists.newArrayList("{\"uid\":1000009}","")));
        // System.out.println("\n\n");
        // System.out.println(replaceAt("@{\"uid\":100001248}@} ", Lists.newArrayList("aaaaa")));
        // System.out.println(replaceAt("@{\"uid\":100001248}@} ",
        // Lists.newArrayList("{\"uid\":100001248,\"username\":\"fomn___58.....\",\"nick\":\"了解释了。斤斤计较看看除fgfs，\\$%\u0026(\"}")));
    }
}
