package com.agent.biogenie.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理端用户列表：不暴露 OpenID、UnionID、外部账号和完整手机号。 */
@Data
@Builder
public class AdminUserDTO {
    private Long id;
    private String nickname;
    /** 为了兼容现有前端字段名，phone 仅返回脱敏值。 */
    private String phone;
    private String avatar;
    private Integer gender;
    private Integer status;
    private String role;
    private LocalDateTime lastLoginTime;
    private Long adminGrantedBy;
    private String adminGrantedByName;
    private LocalDateTime adminGrantedAt;
    private String adminRemark;
    private LocalDateTime createTime;
}
