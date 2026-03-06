package com.yupi.babystepaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 澶氭枃浠朵唬鐮佺粨鏋?
 */
@Description("鐢熸垚澶氫釜浠ｇ爜鏂囦欢鐨勭粨鏋?")
@Data
public class MultiFileCodeResult {

    /**
     * HTML 浠ｇ爜
     */
    @Description("HTML浠ｇ爜")
    private String htmlCode;

    /**
     * CSS 浠ｇ爜
     */
    @Description("CSS浠ｇ爜")
    private String cssCode;

    /**
     * JS 浠ｇ爜
     */
    @Description("JS浠ｇ爜")
    private String jsCode;

    /**
     * 鎻忚堪
     */
    @Description("鐢熸垚浠ｇ爜鐨勬弿杩?")
    private String description;
}
