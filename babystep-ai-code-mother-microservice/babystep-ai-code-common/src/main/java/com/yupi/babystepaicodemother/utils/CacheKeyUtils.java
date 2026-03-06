package com.yupi.babystepaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 缓存 key 生成工具�?
 *
 * @author yupi
 */
public class CacheKeyUtils {

    /**
     * 根据对象生成缓存key (JSON + MD5)
     *
     * @param obj 要生成key的对�?
     * @return MD5哈希后的缓存key
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转 JSON，再�?MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}

