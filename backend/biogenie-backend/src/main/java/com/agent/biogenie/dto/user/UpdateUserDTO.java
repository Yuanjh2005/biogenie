package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求（仅本人，用户 ID 由登录上下文确定）
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserDTO {

    @Size(max = 64, message = "昵称长度不能超过64")
    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Min(value = 0, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    @Schema(description = "性别: 0未知 1男 2女", example = "1")
    private Integer gender;
}
