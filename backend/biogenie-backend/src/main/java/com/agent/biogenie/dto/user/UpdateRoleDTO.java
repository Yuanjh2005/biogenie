package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 超级管理员调整用户角色
 */
@Data
@Schema(description = "调整用户角色请求")
public class UpdateRoleDTO {

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "USER|ADMIN", message = "角色只能为 USER 或 ADMIN")
    @Schema(description = "目标角色: USER / ADMIN", example = "ADMIN")
    private String role;

    @Size(max = 255, message = "授权备注长度不能超过255")
    @Schema(description = "角色调整原因或备注")
    private String remark;
}
