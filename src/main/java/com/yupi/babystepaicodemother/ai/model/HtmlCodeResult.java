package com.yupi.babystepaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * HTML 浠ｇ爜缁撴灉
 */
@Description("鐢熸垚 HTML 浠ｇ爜鏂囦欢鐨勭粨鏋?")
@Data
public class HtmlCodeResult {

    /**
     * HTML 浠ｇ爜
     */
    @Description("HTML浠ｇ爜")
    private String htmlCode;

    /**
     * 鎻忚堪
     */
    @Description("鐢熸垚浠ｇ爜鐨勬弿杩?")
    private String description;
}
