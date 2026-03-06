package com.yupi.babystepaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logo 鍥剧墖鐢熸垚宸ュ叿
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    @Tool("鏍规嵁鎻忚堪鐢熸垚 Logo 璁捐鍥剧墖锛岀敤浜庣綉绔欏搧鐗屾爣璇?")
    public List<ImageResource> generateLogos(@P("Logo 璁捐鎻忚堪锛屽鍚嶇О銆佽涓氥€侀鏍肩瓑锛屽敖閲忚缁?") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 鏋勫缓 Logo 璁捐鎻愮ず璇?
            String logoPrompt = String.format("鐢熸垚 Logo锛孡ogo 涓姝㈠寘鍚换浣曟枃瀛楋紒Logo 浠嬬粛锛?s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 鐢熸垚 1 寮犺冻澶燂紝鍥犱负 AI 涓嶇煡閬撳摢寮犳渶濂?
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String imageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(imageUrl)) {
                        logoList.add(ImageResource.builder()
                                .category(ImageCategoryEnum.LOGO)
                                .description(description)
                                .url(imageUrl)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("鐢熸垚 Logo 澶辫触: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
