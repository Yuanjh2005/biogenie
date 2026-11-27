package com.agent.biogenie.dto.user;

import com.agent.biogenie.utils.LogMaskUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 超级管理员添加管理员账户
 */
@Data
@Schema(description = "添加管理员账户请求")
public class CreateAdminDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Size(max = 64, message = "昵称长度不能超过64")
    @Schema(description = "昵称，不传则使用默认昵称", example = "运营管理员")
    private String nickname;

    @Size(max = 255, message = "授权备注长度不能超过255")
    @Schema(description = "授权原因或备注", example = "运营团队负责人")
    private String remark;

    @Override
    public String toString() {
        return "CreateAdminDTO(phone=" + LogMaskUtil.maskPhone(phone) + ", nickname=" + nickname + ")";
    }
}
