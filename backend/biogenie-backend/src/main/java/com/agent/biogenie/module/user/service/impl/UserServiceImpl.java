package com.agent.biogenie.module.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.dto.user.CreateAdminDTO;
import com.agent.biogenie.dto.user.AdminUserDTO;
import com.agent.biogenie.dto.user.UpdateRoleDTO;
import com.agent.biogenie.dto.user.UpdateUserDTO;
import com.agent.biogenie.dto.user.UserInfoDTO;
import com.agent.biogenie.module.auth.service.UserSessionService;
import com.agent.biogenie.module.admin.service.AdminAuditService;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import com.agent.biogenie.module.user.service.UserService;
import com.agent.biogenie.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final long MAX_AVATAR_SIZE = 20 * 1024 * 1024;

    private final UserMapper userMapper;
    private final MinioUtil minioUtil;
    private final UserSessionService userSessionService;
    private final AdminAuditService adminAuditService;

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public UserInfoDTO getUserInfo(Long currentUserId, Long targetUserId) {
        if (!currentUserId.equals(targetUserId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return getCurrentUser(targetUserId);
    }

    @Override
    public Page<AdminUserDTO> page(int current, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        Set<Long> grantorIds = page.getRecords().stream().map(User::getAdminGrantedBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> grantors = grantorIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(grantorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Page<AdminUserDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(user -> toAdminUserDto(user, grantors)).toList());
        return result;
    }

    @Override
    public void update(Long currentUserId, UpdateUserDTO updateUserDTO) {
        User existUser = getById(currentUserId);
        if (updateUserDTO.getNickname() != null) {
            existUser.setNickname(updateUserDTO.getNickname());
        }
        if (updateUserDTO.getGender() != null) {
            existUser.setGender(updateUserDTO.getGender());
        }
        userMapper.updateById(existUser);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = getById(userId);
        validateAvatar(file);
        String permanentUrl = minioUtil.upload(file, "avatars/" + userId + "/");
        user.setAvatar(permanentUrl);
        userMapper.updateById(user);
        return minioUtil.toAccessUrl(permanentUrl);
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像文件不能为空");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "只支持 jpg/png/gif/webp/bmp 格式");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED, "头像大小不能超过20MB");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long operatorId, Long userId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "状态值只能为0或1");
        }
        if (operatorId.equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能修改自己的登录状态");
        }
        User operator = getById(operatorId);
        User user = getById(userId);
        assertCanManage(operator, user);
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            userSessionService.removeAllSessions(userId);
        }
        adminAuditService.record(operatorId, status == 0 ? "DISABLE_USER" : "ENABLE_USER",
                "USER", String.valueOf(userId), "targetRole=" + user.getRole());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO createAdmin(Long operatorId, CreateAdminDTO dto) {
        User operator = getById(operatorId);
        if (!AuthConstants.isSuperAdmin(operator.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (existing == null) {
            User user = new User();
            user.setPhone(dto.getPhone());
            user.setNickname(StrUtil.blankToDefault(dto.getNickname(),
                    "管理员" + dto.getPhone().substring(7)));
            user.setStatus(1);
            user.setRole(AuthConstants.ROLE_ADMIN);
            user.setAdminGrantedBy(operatorId);
            user.setAdminGrantedAt(LocalDateTime.now());
            user.setAdminRemark(dto.getRemark());
            userMapper.insert(user);
            adminAuditService.record(operatorId, "CREATE_ADMIN", "USER", String.valueOf(user.getId()), dto.getRemark());
            return getCurrentUser(user.getId());
        }
        if (AuthConstants.isSuperAdmin(existing.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能修改超级管理员角色");
        }
        if (AuthConstants.ROLE_ADMIN.equalsIgnoreCase(existing.getRole())) {
            throw new BizException(ResultCode.USER_EXISTED, "该手机号已是管理员");
        }
        existing.setRole(AuthConstants.ROLE_ADMIN);
        existing.setAdminGrantedBy(operatorId);
        existing.setAdminGrantedAt(LocalDateTime.now());
        existing.setAdminRemark(dto.getRemark());
        if (StrUtil.isNotBlank(dto.getNickname())) {
            existing.setNickname(dto.getNickname());
        }
        userMapper.updateById(existing);
        adminAuditService.record(operatorId, "PROMOTE_ADMIN", "USER", String.valueOf(existing.getId()), dto.getRemark());
        return getCurrentUser(existing.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO updateRole(Long operatorId, Long userId, UpdateRoleDTO dto) {
        if (operatorId.equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能修改自己的角色");
        }
        User operator = getById(operatorId);
        if (!AuthConstants.isSuperAdmin(operator.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        User user = getById(userId);
        if (AuthConstants.isSuperAdmin(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能修改超级管理员角色");
        }
        String targetRole = dto.getRole().toUpperCase();
        if (targetRole.equalsIgnoreCase(StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER))) {
            return getCurrentUser(userId);
        }
        String oldRole = StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER);
        user.setRole(targetRole);
        if (AuthConstants.ROLE_ADMIN.equals(targetRole)) {
            user.setAdminGrantedBy(operatorId);
            user.setAdminGrantedAt(LocalDateTime.now());
            user.setAdminRemark(dto.getRemark());
        } else {
            user.setAdminGrantedBy(null);
            user.setAdminGrantedAt(null);
            user.setAdminRemark(null);
        }
        userMapper.updateById(user);
        userSessionService.removeAllSessions(userId);
        adminAuditService.record(operatorId, "UPDATE_ROLE", "USER", String.valueOf(userId),
                oldRole + "->" + targetRole + "; remark=" + StrUtil.blankToDefault(dto.getRemark(), ""));
        return getCurrentUser(userId);
    }

    private void assertCanManage(User operator, User target) {
        String targetRole = StrUtil.blankToDefault(target.getRole(), AuthConstants.ROLE_USER);
        if (AuthConstants.isSuperAdmin(targetRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能限制超级管理员登录");
        }
        if (AuthConstants.ROLE_ADMIN.equalsIgnoreCase(operator.getRole())
                && AuthConstants.isAdminOrAbove(targetRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "管理员不能限制其他管理员登录");
        }
    }

    @Override
    public UserInfoDTO getCurrentUser(Long userId) {
        User user = getById(userId);
        String avatar = user.getAvatar();
        if (StrUtil.isNotBlank(avatar)) {
            avatar = minioUtil.toAccessUrl(avatar);
        }
        return UserInfoDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(avatar)
                .phone(user.getPhone() != null && user.getPhone().length() >= 11
                        ? user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7)
                        : user.getPhone())
                .gender(user.getGender())
                .role(StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER))
                .build();
    }

    private AdminUserDTO toAdminUserDto(User user, Map<Long, User> grantors) {
        User grantor = user.getAdminGrantedBy() == null ? null : grantors.get(user.getAdminGrantedBy());
        return AdminUserDTO.builder()
                .id(user.getId()).nickname(user.getNickname()).phone(maskPhone(user.getPhone()))
                .avatar(safeAvatarUrl(user))
                .gender(user.getGender()).status(user.getStatus())
                .role(StrUtil.blankToDefault(user.getRole(), AuthConstants.ROLE_USER))
                .lastLoginTime(user.getLastLoginTime()).adminGrantedBy(user.getAdminGrantedBy())
                .adminGrantedByName(grantor == null ? null : grantor.getNickname())
                .adminGrantedAt(user.getAdminGrantedAt()).adminRemark(user.getAdminRemark())
                .createTime(user.getCreateTime()).build();
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) return null;
        return phone.length() >= 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "****";
    }

    private String safeAvatarUrl(User user) {
        if (StrUtil.isBlank(user.getAvatar())) return null;
        try {
            return minioUtil.toAccessUrl(user.getAvatar());
        } catch (Exception e) {
            log.warn("用户头像链接生成失败，已降级为空头像: userId={}, err={}", user.getId(), e.getMessage());
            return null;
        }
    }
}
