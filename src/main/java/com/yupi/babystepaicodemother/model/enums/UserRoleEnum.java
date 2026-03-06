package com.yupi.babystepaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("鐢ㄦ埛", "user"),
    ADMIN("绠＄悊鍛?", "admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 鏍规嵁 value 鑾峰彇鏋氫妇
     *
     * @param value 鏋氫妇鍊肩殑value
     * @return 鏋氫妇鍊?
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
