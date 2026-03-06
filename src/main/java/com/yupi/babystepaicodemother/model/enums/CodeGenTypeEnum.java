package com.yupi.babystepaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 浠ｇ爜鐢熸垚绫诲瀷鏋氫妇
 */
@Getter
public enum CodeGenTypeEnum {

    HTML("鍘熺敓 HTML 妯″紡", "html"),
    MULTI_FILE("鍘熺敓澶氭枃浠舵ā寮?", "multi_file"),
    VUE_PROJECT("Vue 宸ョ▼妯″紡", "vue_project");

    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 鏍规嵁 value 鑾峰彇鏋氫妇
     *
     * @param value 鏋氫妇鍊肩殑value
     * @return 鏋氫妇鍊?
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
