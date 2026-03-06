package com.yupi.babystepaicodemother.langgraph4j.ai;

import com.yupi.babystepaicodemother.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码质量检查服�?
 */
public interface CodeQualityCheckService {

    /**
     * 检查代码质�?
     * AI 会分析代码并返回质量检查结�?
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String codeContent);
}
