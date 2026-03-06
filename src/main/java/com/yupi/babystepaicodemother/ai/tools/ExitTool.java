package com.yupi.babystepaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 鍛婅瘔 AI 瑕侀€€鍑虹殑宸ュ叿
 */
@Slf4j
@Component
public class ExitTool extends BaseTool {

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getDisplayName() {
        return "閫€鍑哄伐鍏疯皟鐢?";
    }

    /**
     * 閫€鍑哄伐鍏疯皟鐢?
     * 褰撲换鍔″畬鎴愭垨鏃犻渶缁х画浣跨敤宸ュ叿鏃惰皟鐢ㄦ鏂规硶
     *
     * @return 閫€鍑虹‘璁や俊鎭?
     */
    @Tool("褰撲换鍔″凡瀹屾垚鎴栨棤闇€缁х画璋冪敤宸ュ叿鏃讹紝浣跨敤姝ゅ伐鍏烽€€鍑烘搷浣滐紝闃叉寰幆")
    public String exit() {
        log.info("AI 璇锋眰閫€鍑哄伐鍏疯皟鐢?");
        return "涓嶈缁х画璋冪敤宸ュ叿锛屽彲浠ヨ緭鍑烘渶缁堢粨鏋滀簡";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return "\n\n[鎵ц缁撴潫]\n\n";
    }
}
