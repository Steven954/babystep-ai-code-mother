package com.yupi.babystepaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 鍥剧墖鏀堕泦宸ュ叿锛堟彃鐢诲浘鐗囷級
 */
@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_API_URL = "https://undraw.co/_next/data/mMWmJSt23qpgo8cLTD_pB/search/%s.json?term=%s";

    @Tool("鎼滅储鎻掔敾鍥剧墖锛岀敤浜庣綉绔欑編鍖栧拰瑁呴グ")
    public List<ImageResource> searchIllustrations(@P("鎼滅储鍏抽敭璇?") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        String apiUrl = String.format(UNDRAW_API_URL, query, query);

        // 浣跨敤 try-with-resources 鑷姩閲婃斁 HTTP 璧勬簮
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "鎻掔敾");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("鎼滅储鎻掔敾澶辫触锛歿}", e.getMessage(), e);
        }
        return imageList;
    }
}
