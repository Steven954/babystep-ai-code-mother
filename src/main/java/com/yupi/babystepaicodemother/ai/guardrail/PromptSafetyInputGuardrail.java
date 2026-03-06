package com.yupi.babystepaicodemother.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt 瀹夊叏瀹℃煡鎶よ建
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 鏁忔劅璇嶅垪琛?
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "蹇界暐涔嬪墠鐨勬寚浠?", "ignore previous instructions", "ignore above",
            "鐮磋В", "hack", "缁曡繃", "bypass", "瓒婄嫳", "jailbreak"
    );

    // 娉ㄥ叆鏀诲嚮妯″紡
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // 妫€鏌ヨ緭鍏ラ暱搴?
        if (input.length() > 1000) {
            return fatal("杈撳叆鍐呭杩囬暱锛屼笉瑕佽秴杩?1000 瀛?");
        }
        // 妫€鏌ユ槸鍚︿负绌?
        if (input.trim().isEmpty()) {
            return fatal("杈撳叆鍐呭涓嶈兘涓虹┖");
        }
        // 妫€鏌ユ晱鎰熻瘝
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return fatal("杈撳叆鍖呭惈涓嶅綋鍐呭锛岃淇敼鍚庨噸璇?");
            }
        }
        // 妫€鏌ユ敞鍏ユ敾鍑绘ā寮?
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("妫€娴嬪埌鎭舵剰杈撳叆锛岃姹傝鎷掔粷");
            }
        }
        return success();
    }
} 
