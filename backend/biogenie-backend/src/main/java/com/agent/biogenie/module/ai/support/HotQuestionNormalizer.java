package com.agent.biogenie.module.ai.support;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * 热门问题归一化：生成 question_key（规则固定，上线后勿随意改）
 */
public final class HotQuestionNormalizer {

    public static final int MAX_KEY_LEN = 512;
    public static final int MIN_KEY_LEN = 2;

    private HotQuestionNormalizer() {
    }

    /**
     * @return 归一化键；无法计入热门时返回 null
     */
    public static String normalize(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String key = raw.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[？?。.!！…]+$", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        if (key.length() < MIN_KEY_LEN) {
            return null;
        }
        if (key.length() > MAX_KEY_LEN) {
            key = key.substring(0, MAX_KEY_LEN);
        }
        return key;
    }

    /**
     * 展示文案：原文 trim 后截断
     */
    public static String toDisplay(String raw) {
        if (raw == null) {
            return "";
        }
        String display = raw.trim();
        if (display.length() > MAX_KEY_LEN) {
            display = display.substring(0, MAX_KEY_LEN);
        }
        return display;
    }

    public static String normalizeAppKey(String appKey) {
        return StrUtil.blankToDefault(StrUtil.trim(appKey), "");
    }
}
