package com.yy.me.web.util;

import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.google.common.collect.Sets;

public class ReserveIdsUtil {
    private static Logger logger = LoggerFactory.getLogger(ReserveIdsUtil.class);

    private static final Set<String> reserveIds = Sets.newLinkedHashSet();

    static {
        Scanner scanner = null;
        try {
            PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
            Resource resource = prpr.getResource("reserveids.txt");
            scanner = new Scanner(resource.getInputStream(), "utf-8");

            int count = 0;
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                count++;
                if (line.contains("/")) {
                    String[] words = line.split("/");
                    for (String tmp : words) {
                        if (StringUtils.isNotBlank(tmp)) {
                            reserveIds.add(change(tmp));
                        }
                    }
                } else {
                    if (StringUtils.isNotBlank(line)) {
                        reserveIds.add(change(line));
                    }
                }
            }
            logger.info("Total Reserve IDs set is:" + count);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
    
    public static String change(String input) {
        input = PinyinHelper.convertToPinyinString(input.trim(), "", PinyinFormat.WITHOUT_TONE);
        return input.toLowerCase();
    }

    /**
     * 是否为保留的id
     * 
     * @param username
     * @return
     */
    public static boolean goodUsername(String username) {
        System.out.print(username + ": ");
        username = username.toLowerCase();
        // 特殊
        if (ReserveIdsUtil.reserveIds.contains(username)) {
            System.out.print("特殊");
            return true;
        }
        
        byte[] b = username.getBytes();
        int len = b.length;
        // 位数少
        if (len < 5) {
            System.out.print("小于且等于 4位数的任意组合 ");
            return true;
        }
        if (len >= 5 && len <= 12) {
            int e1 = 1;// 连续性
            for (; e1 < len; e1++) {// 顺序
                if (b[e1] != b[e1 - 1] + 1) {
                    break;
                }
            }
            if (e1 == len) {// [5-8位]
                System.out.print("[5-8位] 连续性-顺序");
                return true;
            }

            e1 = 1;
            for (; e1 < len; e1++) {// 倒序
                if (b[e1] != b[e1 - 1] - 1) {
                    break;
                }
            }
            if (e1 == len) {// [5-8位]
                System.out.print("[5-8位] 连续性-倒序");
                return true;
            }

            int e2 = 1;// 重复性
            for (; e2 < len; e2++) {
                if (b[e2] != b[e2 - 1]) {// 含有1个数字/字母的重复组合
                    break;
                }
            }
            if (e2 == len) {// [5-8位]
                System.out.print("[5-8位] 重复性-含有1个数字/字母的重复组合");
                return true;
            }

            e2 = 1;
            byte t1 = b[0];
            byte t2 = t1;
            for (; e2 < len; e2++) {// 含有2个数字/字母的重复组合
                if (t2 != b[e2]) {
                    if (t1 == t2) {// 第一次初始化
                        t2 = b[e2];
                    } else if (b[e2] != t1) {
                        break;
                    }
                }
            }
            if (e2 == len && bothMathOrCharOrDotOrUl(t1, t2)) {// [5-8位] 只含有2个数字/字母的重复组合
                // 波浪重复
                for (e2 = 1; e2 < len; e2++) {
                    if (b[e2] == b[e2 - 1]) {
                        break;
                    }
                }
                if (e2 == len) {
                    System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（波浪）组合");
                    return true;
                }

                // 1带N
                if (t2 == b[1]) {
                    for (e2 = 2; e2 < len; e2++) {
                        if (b[e2] != t2) {
                            break;
                        }
                    }
                    if (e2 == len) {
                        System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（1带N）组合");
                        return true;
                    }
                }

                // N带1 或 各占一半
                if (t2 == b[len - 1]) {
                    // N带1
                    for (e2 = 1; e2 < len - 1; e2++) {
                        if (b[e2] != t1) {
                            break;
                        }
                    }
                    if (e2 == len - 1) {
                        System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（N带1）组合");
                        return true;
                    }
                    // 各占一半
                    for (e2 = 1; e2 < len / 2; e2++) {
                        if (b[e2] != t1) {
                            break;
                        }
                    }
                    if (e2 == len / 2) {
                        for (e2 = len / 2 + (len % 2); e2 < len; e2++) {// 对于奇数个字符，跳开中间的那个
                            if (b[e2] != t2) {
                                break;
                            }
                        }
                        if (e2 == len) {
                            System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（各占一半）组合");
                            return true;
                        }
                    }
                }

                // 对称
                int s2 = 0;
                e2 = len - 1;
                for (; s2 < e2; s2++, e2--) {
                    if (b[s2] != b[e2]) {
                        s2 = -1;
                        break;
                    }
                }
                if (s2 > 0) {
                    System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（对称）组合");
                    return true;
                }
                
                // 重放
                if (len % 2 == 0) {
                    s2 = 0;
                    e2 = len / 2;
                    for (; e2 < len; s2++, e2++) {
                        if (b[s2] != b[e2]) {
                            break;
                        }
                    }
                    if (e2 == len) {
                        System.out.print("[5-8位] 重复性-含有2个数字/字母的重复（重放）组合");
                        return true;
                    }
                }
            }
        }

        if (len == 6 || len == 8 || len == 10 || len == 12) {// [6,8位] 含有3个数字/字母的重复组合,并且具有连续性（顺/倒序）
            // 一类
            int e2 = 1;
            for (; e2 < len / 2; e2++) {// 顺序
                if (b[e2] != b[e2 - 1] + 1) {
                    break;
                }
            }
            if (e2 < len / 2) {// 倒序
                e2 = 1;
                for (; e2 < len / 2; e2++) {
                    if (b[e2] != b[e2 - 1] - 1) {
                        break;
                    }
                }
            }
            if (e2 == len / 2) {
                e2++;
                for (; e2 < len; e2++) {
                    if (b[e2] != b[e2 - 1] + 1) {// 顺序
                        break;
                    }
                }
                if (e2 == len) {
                    System.out.print("[6,8位] 含有3个数字/字母的重复组合,并且具有连续性（一类--顺序结尾）");
                    return true;
                }
                e2 = len / 2 + 1;
                for (; e2 < len; e2++) {// 倒序
                    if (b[e2] != b[e2 - 1] - 1) {
                        break;
                    }
                }
                if (e2 == len) {
                    System.out.print("[6,8位] 含有3个数字/字母的重复组合,并且具有连续性（一类--倒序结尾）");
                    return true;
                }
            }

            // 二类
            e2 = 1;
            for (; e2 < len; e2++) {
                if (e2 % 2 == 1) {// 奇数下标
                    if (b[e2] != b[e2 - 1]) {// 等于
                        break;
                    }
                } else {// 偶数下标
                    if (b[e2] != b[e2 - 1] + 1) {// 顺序
                        break;
                    }
                }
            }
            if (e2 == len) {
                System.out.print("[6,8位] 含有3个数字/字母的重复组合,并且具有连续性（二类--顺序）");
                return true;
            }

            e2 = 1;
            for (; e2 < len; e2++) {
                if (e2 % 2 == 1) {// 奇数下标
                    if (b[e2] != b[e2 - 1]) {// 等于
                        break;
                    }
                } else {// 偶数下标
                    if (b[e2] != b[e2 - 1] - 1) {// 顺序
                        break;
                    }
                }
            }
            if (e2 == len) {
                System.out.print("[6,8位] 含有3个数字/字母的重复组合,并且具有连续性（二类--倒序）");
                return true;
            }

        }
        return false;
    }
    
    private static boolean bothMathOrCharOrDotOrUl(byte t1, byte t2) {
        return (bothMath(t1, t2) || bothLowerChar(t1, t2)) || t1 == 46 || t2 == 46 || t1 == 95 || t2 == 95;
    }
    
    private static boolean bothMath(byte t1, byte t2) {
        return (48 <= t1 && t1 <= 57) && (48 <= t2 && t2 <= 57);
    }
    
    private static boolean bothLowerChar(byte t1, byte t2) {
        return (97 <= t1 && t1 <= 122) && (97 <= t2 && t2 <= 122);
    }

    public static void main(String[] args) {
        for (Iterator<String> iterator = reserveIds.iterator(); iterator.hasNext();) {
            System.out.println(iterator.next());
        }
        
        System.out.println(goodUsername("12345"));
        System.out.println(goodUsername("23456789"));

        System.out.println(goodUsername("54321"));
        System.out.println(goodUsername("98765432"));
        System.out.println(goodUsername("98765"));

        System.out.println(goodUsername("ABCDE"));
        System.out.println(goodUsername("ABCDEF"));
        System.out.println(goodUsername("ABCDEFG"));

        System.out.println(goodUsername("11111"));
        System.out.println(goodUsername("AAAAAA"));

        System.out.println(goodUsername("23333"));
        System.out.println(goodUsername("6666667"));
        System.out.println(goodUsername("2___2"));
        System.out.println(goodUsername("ccceccc"));
        System.out.println(goodUsername("2.2.2.2."));
        System.out.println(goodUsername("ABBBB"));
        System.out.println(goodUsername("CCCCCD"));

        System.out.println(goodUsername("lxlxl"));
        System.out.println(goodUsername("232323"));
        System.out.println(goodUsername("7775555"));
        System.out.println(goodUsername("DEDEDE"));
        System.out.println(goodUsername("DDDDEEEE"));
        System.out.println(goodUsername("abbbabbb"));
        System.out.println(goodUsername("aabaab"));

        System.out.println(goodUsername("345345"));
        System.out.println(goodUsername("345543"));
        System.out.println(goodUsername("BCDBCD"));
        System.out.println(goodUsername("BCDDCB"));

        System.out.println(goodUsername("334455"));
        System.out.println(goodUsername("665544"));
        System.out.println(goodUsername("66AABB"));
        System.out.println(goodUsername("6AB6AB"));
        System.out.println(goodUsername("CCDDEE"));
        System.out.println(goodUsername("EEDDCC"));
        System.out.println(goodUsername("222AAA"));

        System.out.println(goodUsername("520ME"));
        System.out.println(goodUsername("1314ME"));
        System.out.println(goodUsername("ME520"));
        System.out.println(goodUsername("ME1314"));
        System.out.println(goodUsername("zuckerberg"));
        System.out.println(goodUsername("xiena"));
    }
}
