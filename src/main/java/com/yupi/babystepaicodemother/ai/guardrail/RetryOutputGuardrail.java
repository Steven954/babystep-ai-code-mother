package com.yupi.babystepaicodemother.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * 閲嶈瘯杈撳嚭鎶よ建
 */
public class RetryOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String response = responseFromLLM.text();
        // 妫€鏌ュ搷搴旀槸鍚︿负绌烘垨杩囩煭
        if (response == null || response.trim().isEmpty()) {
            return reprompt("鍝嶅簲鍐呭涓虹┖", "璇烽噸鏂扮敓鎴愬畬鏁寸殑鍐呭");
        }
        if (response.trim().length() < 10) {
            return reprompt("鍝嶅簲鍐呭杩囩煭", "璇锋彁渚涙洿璇︾粏鐨勫唴瀹?");
        }
        // 妫€鏌ユ槸鍚﹀寘鍚晱鎰熶俊鎭垨涓嶅綋鍐呭
        if (containsSensitiveContent(response)) {
            return reprompt("鍖呭惈鏁忔劅淇℃伅", "璇烽噸鏂扮敓鎴愬唴瀹癸紝閬垮厤鍖呭惈鏁忔劅淇℃伅");
        }
        return success();
    }
    
    /**
     * 妫€鏌ユ槸鍚﹀寘鍚晱鎰熷唴瀹?
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
            "瀵嗙爜", "password", "secret", "token", 
            "api key", "绉侀挜", "璇佷功", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
