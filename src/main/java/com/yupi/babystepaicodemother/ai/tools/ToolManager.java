package com.yupi.babystepaicodemother.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 宸ュ叿绠＄悊鍣?
 * 缁熶竴绠＄悊鎵€鏈夊伐鍏凤紝鎻愪緵鏍规嵁鍚嶇О鑾峰彇宸ュ叿鐨勫姛鑳?
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * 宸ュ叿鍚嶇О鍒板伐鍏峰疄渚嬬殑鏄犲皠
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * 鑷姩娉ㄥ叆鎵€鏈夊伐鍏?
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 鍒濆鍖栧伐鍏锋槧灏?
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("娉ㄥ唽宸ュ叿: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("宸ュ叿绠＄悊鍣ㄥ垵濮嬪寲瀹屾垚锛屽叡娉ㄥ唽 {} 涓伐鍏?", toolMap.size());
    }


    /**
     * 鏍规嵁宸ュ叿鍚嶇О鑾峰彇宸ュ叿瀹炰緥
     *
     * @param toolName 宸ュ叿鑻辨枃鍚嶇О
     * @return 宸ュ叿瀹炰緥
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 鑾峰彇宸叉敞鍐岀殑宸ュ叿闆嗗悎
     *
     * @return 宸ュ叿瀹炰緥闆嗗悎
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}

