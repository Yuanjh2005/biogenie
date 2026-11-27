package com.agent.biogenie.module.ai.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HotQuestionNormalizerTest {

    @Test
    void normalize_shouldTrimCollapseSpaceLowercaseStripTrailingPunct() {
        String raw = "  " + "什么是" + "  " + "EGFR" + "？  ";
        assertEquals("什么是 egfr", HotQuestionNormalizer.normalize(raw));
    }

    @Test
    void normalize_withoutSpace_shouldKeepCompactForm() {
        assertEquals("什么是egfr", HotQuestionNormalizer.normalize("什么是EGFR"));
    }

    @Test
    void normalize_differentWording_shouldBeDifferentKeys() {
        assertNotEquals(
                HotQuestionNormalizer.normalize("什么是EGFR"),
                HotQuestionNormalizer.normalize("EGFR是什么")
        );
        assertEquals("egfr是什么", HotQuestionNormalizer.normalize("EGFR是什么"));
    }

    @Test
    void normalize_blankOrTooShort_shouldReturnNull() {
        assertNull(HotQuestionNormalizer.normalize(null));
        assertNull(HotQuestionNormalizer.normalize("  "));
        assertNull(HotQuestionNormalizer.normalize("a"));
    }

    @Test
    void toDisplay_shouldKeepOriginalCasing() {
        String raw = "  " + "什么是 EGFR？" + "  ";
        assertEquals("什么是 EGFR？", HotQuestionNormalizer.toDisplay(raw));
    }

    @Test
    void normalizeAppKey_blank_shouldBeEmpty() {
        assertEquals("", HotQuestionNormalizer.normalizeAppKey(null));
        assertEquals("", HotQuestionNormalizer.normalizeAppKey("  "));
        assertEquals("smartdrgf-fast", HotQuestionNormalizer.normalizeAppKey(" smartdrgf-fast "));
    }
}
