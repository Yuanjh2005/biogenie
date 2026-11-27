package com.agent.biogenie.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agent.biogenie.common.PageResult;
import com.agent.biogenie.common.Result;
import com.agent.biogenie.common.auth.AuthContext;
import com.agent.biogenie.common.auth.RequiredAdmin;
import com.agent.biogenie.common.auth.RequiredSuperAdmin;
import com.agent.biogenie.dto.user.CreateAdminDTO;
import com.agent.biogenie.dto.user.AdminUserDTO;
import com.agent.biogenie.dto.user.UpdateRoleDTO;
import com.agent.biogenie.dto.user.UpdateUserDTO;
import com.agent.biogenie.dto.user.UserInfoDTO;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.admin.service.AdminSecondFactorService;
import com.agent.biogenie.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户CRUD、头像上传等接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminSecondFactorService secondFactorService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/profile")
    public Result<UserInfoDTO> currentUser() {
        UserInfoDTO userInfo = userService.getCurrentUser(AuthContext.getUserId());
        return Result.ok(userInfo);
    }

    @Deprecated
    @Operation(summary = "根据ID查询用户（仅本人）")
    @GetMapping("/{id}")
    public Result<UserInfoDTO> getUser(@PathVariable Long id) {
        UserInfoDTO userInfo = userService.getUserInfo(AuthContext.getUserId(), id);
        return Result.ok(userInfo);
    }

    @RequiredAdmin
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<PageResult<AdminUserDTO>> page(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int size,
            @Parameter(description = "搜索关键词(昵称/手机号)")
            @RequestParam(required = false) String keyword) {
        Page<AdminUserDTO> page = userService.page(current, size, keyword);
        PageResult<AdminUserDTO> pageResult = PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
        return Result.ok(pageResult);
    }

    @Operation(summary = "更新当前登录用户信息")
    @PutMapping("/update")
    public Result<String> update(@Valid @RequestBody UpdateUserDTO updateUserDTO) {
        userService.update(AuthContext.getUserId(), updateUserDTO);
        return Result.ok("更新成功");
    }

    @Operation(summary = "更新用户头像")
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> updateAvatar(
            @Parameter(description = "头像图片文件") @RequestParam("file") MultipartFile file) {
        String url = userService.updateAvatar(AuthContext.getUserId(), file);
        return Result.ok("头像更新成功", url);
    }

    @RequiredAdmin
    @Operation(summary = "禁用/启用用户",
            description = "status=0 禁止登录并踢出全部会话。管理员可限制普通用户；超级管理员可限制管理员。")
    @PutMapping("/status/{id}")
    public Result<String> updateStatus(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Confirm-Token") String confirmToken,
            @Parameter(description = "状态: 0禁用 1正常")
            @RequestParam @NotNull @Min(0) @Max(1) Integer status) {
        secondFactorService.consume(AuthContext.getUserId(), AuthContext.getSessionId(), confirmToken);
        userService.updateStatus(AuthContext.getUserId(), id, status);
        return Result.ok(status == 1 ? "已启用" : "已禁用");
    }

    @RequiredSuperAdmin
    @Operation(summary = "添加管理员账户",
            description = "按手机号新建管理员，或将已有普通用户提升为管理员。新账户可通过短信验证码登录。")
    @PostMapping("/admin")
    public Result<UserInfoDTO> createAdmin(
            @RequestHeader("X-Admin-Confirm-Token") String confirmToken,
            @Valid @RequestBody CreateAdminDTO dto) {
        secondFactorService.consume(AuthContext.getUserId(), AuthContext.getSessionId(), confirmToken);
        return Result.ok(userService.createAdmin(AuthContext.getUserId(), dto));
    }

    @RequiredSuperAdmin
    @Operation(summary = "调整用户角色", description = "仅可设置为 USER 或 ADMIN，不能操作超级管理员。")
    @PutMapping("/role/{id}")
    public Result<UserInfoDTO> updateRole(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Confirm-Token") String confirmToken,
            @Valid @RequestBody UpdateRoleDTO dto) {
        secondFactorService.consume(AuthContext.getUserId(), AuthContext.getSessionId(), confirmToken);
        return Result.ok(userService.updateRole(AuthContext.getUserId(), id, dto));
    }
}
