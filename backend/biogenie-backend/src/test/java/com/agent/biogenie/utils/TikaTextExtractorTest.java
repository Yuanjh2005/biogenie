package com.agent.biogenie.utils;

import com.agent.biogenie.common.BizException;
import com.agent.biogenie.dto.file.FileExtractResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TikaTextExtractorTest {

    private TikaTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TikaTextExtractor();
    }

    @Test
    void extract_txt_shouldReturnText() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "Hello BioGenie\n第二行内容".getBytes(StandardCharsets.UTF_8)
        );

        FileExtractResultDTO result = extractor.extract(file);

        assertEquals("sample.txt", result.getFileName());
        assertFalse(result.isTruncated());
        assertTrue(result.getText().contains("Hello BioGenie"));
        assertTrue(result.getText().contains("第二行内容"));
        assertEquals(result.getText().length(), result.getCharCount());
    }

    @Test
    void extract_emptyFile_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );
        assertThrows(BizException.class, () -> extractor.extract(file));
    }

    @Test
    void fromRawText_shouldTruncateWhenTooLong() {
        String longText = "a".repeat(TikaTextExtractor.MAX_CHARS + 50);
        FileExtractResultDTO result = extractor.fromRawText("big.txt", "text/plain", longText);

        assertTrue(result.isTruncated());
        assertEquals(TikaTextExtractor.MAX_CHARS, result.getCharCount());
        assertEquals(TikaTextExtractor.MAX_CHARS, result.getText().length());
    }
}
