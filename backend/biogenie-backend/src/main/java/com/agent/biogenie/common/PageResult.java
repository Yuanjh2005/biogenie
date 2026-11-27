package com.agent.biogenie.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回类
 *
 * @param <T> 数据类型
 */
@Data
@Schema(description = "分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总条数", example = "100")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private long page;

    @Schema(description = "每页条数", example = "10")
    private long size;

    @Schema(description = "数据列表")
    private List<T> records;

    private PageResult() {}

    public static <T> PageResult<T> of(long total, long page, long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.page = page;
        result.size = size;
        result.records = records;
        return result;
    }
}
