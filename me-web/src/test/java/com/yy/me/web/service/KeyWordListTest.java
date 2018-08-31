package com.yy.me.web.service;

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class KeyWordListTest {

    private static final String openUrl = "https://ourtimespicture.bs2dl-ssl.yy.com/remote_censor_words.txt";
    private static final String url = "http://keywords.zbase.yy.com:8198/999980000/key.txt";

    public static void main(String[] args) throws Exception {
        List<String> wordList = getWordList();
        for (String word : wordList) {
            System.out.println(word);
        }
    }

    private static List<String> getWordList() throws Exception {
        HttpURLConnection conn = null;
        InputStream urlStream = null;
        BufferedReader reader = null;
        List<String> keywordList = new LinkedList<String>();

        try {
            URL requestUrl = new URL(openUrl + "?r=" + System.currentTimeMillis());
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            urlStream = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(urlStream, "UTF-8"));

            StringBuffer response = new StringBuffer();
            String line = null;
            do {
                line = reader.readLine();
                if (line != null) {
                    response.append(line).append('\n');
                }
            } while (line != null);

            byte[] lb = Base64.decodeBase64(response.toString());
            String ls = new String(lb, "utf-8");
            String[] ws = ls.split("\n");
            for (String t : ws) {
                keywordList.add(t.trim());
            }
            return keywordList;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }

                if (urlStream != null) {
                    urlStream.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
    }

}
