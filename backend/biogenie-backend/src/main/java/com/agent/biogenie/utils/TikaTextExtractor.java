package com.agent.biogenie.utils;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.dto.file.FileExtractResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 使用 Apache Tika 从文档中抽取纯文本
 */
@Slf4j
@Component
public class TikaTextExtractor {

    /** 抽取后最大保留字符数 */
    public static final int MAX_CHARS = 100_000;

    private final Tika tika = new Tika();

    public FileExtractResultDTO extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        String contentType = StrUtil.blankToDefault(file.getContentType(), "application/octet-stream");
        String rawText;
        try (InputStream in = file.getInputStream()) {
            rawText = tika.parseToString(in);
        } catch (IOException | TikaException e) {
            log.warn("Tika 抽取失败: fileName={}, message={}", fileName, e.getMessage());
            throw new BizException(ResultCode.BAD_REQUEST, "文件内容提取失败，请更换文档后重试");
        } catch (Exception e) {
            log.error("Tika 抽取异常: fileName={}", fileName, e);
            throw new BizException(ResultCode.BAD_REQUEST, "文件内容提取失败，请更换文档后重试");
        }

        String normalized = normalize(rawText);
        if (StrUtil.isBlank(normalized)) {
            throw new BizException(ResultCode.BAD_REQUEST, "未能从文件中提取到文本内容");
        }

        boolean truncated = normalized.length() > MAX_CHARS;
        String text = truncated ? normalized.substring(0, MAX_CHARS) : normalized;

        return FileExtractResultDTO.builder()
                .fileName(fileName)
                .contentType(contentType)
                .charCount(text.length())
                .truncated(truncated)
                .text(text)
                .build();
    }

    /**
     * 供单测：对已有字符串做归一化与截断。
     */
    FileExtractResultDTO fromRawText(String fileName, String contentType, String rawText) {
        String normalized = normalize(rawText);
        boolean truncated = normalized.length() > MAX_CHARS;
        String text = truncated ? normalized.substring(0, MAX_CHARS) : normalized;
        return FileExtractResultDTO.builder()
                .fileName(fileName)
                .contentType(contentType)
                .charCount(text.length())
                .truncated(truncated)
                .text(text)
                .build();
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
