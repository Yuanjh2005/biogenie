package com.agent.biogenie.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agent.biogenie.dto.user.CreateAdminDTO;
import com.agent.biogenie.dto.user.AdminUserDTO;
import com.agent.biogenie.dto.user.UpdateRoleDTO;
import com.agent.biogenie.dto.user.UpdateUserDTO;
import com.agent.biogenie.dto.user.UserInfoDTO;
import com.agent.biogenie.module.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据ID查询用户
     */
    User getById(Long id);

    /**
     * 查询用户信息（仅允许查看本人）
     */
    UserInfoDTO getUserInfo(Long currentUserId, Long targetUserId);

    /**
     * 分页查询用户列表
     */
    Page<AdminUserDTO> page(int current, int size, String keyword);

    /**
     * 更新用户信息（仅允许更新本人）
     */
    void update(Long currentUserId, UpdateUserDTO updateUserDTO);

    /**
     * 上传并更新用户头像
     */
    String updateAvatar(Long userId, MultipartFile file);

    /**
     * 禁用/启用用户（限制登录）
     */
    void updateStatus(Long operatorId, Long userId, Integer status);

    /**
     * 超级管理员添加管理员：已有手机号则提升为 ADMIN，否则新建账户。
     */
    UserInfoDTO createAdmin(Long operatorId, CreateAdminDTO dto);

    /**
     * 超级管理员调整用户角色（仅 USER / ADMIN）
     */
    UserInfoDTO updateRole(Long operatorId, Long userId, UpdateRoleDTO dto);

    /**
     * 获取当前登录用户信息
     */
    UserInfoDTO getCurrentUser(Long userId);
}
