package com.agent.biogenie.common.auth;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.common.BizException;
import com.agent.biogenie.common.ResultCode;
import com.agent.biogenie.module.user.entity.User;
import com.agent.biogenie.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 角色权限切面：校验当前用户是否具备管理员 / 超级管理员角色
 */
@Slf4j
@Aspect
@Component
@Order(2)
@RequiredArgsConstructor
public class RoleAuthAspect {

    private final UserMapper userMapper;

    @Before("@annotation(com.agent.biogenie.common.auth.RequiredAdmin) || @within(com.agent.biogenie.common.auth.RequiredAdmin)")
    public void checkAdmin() {
        User user = currentUser();
        if (!AuthConstants.isAdminOrAbove(StrUtil.blankToDefault(user.getRole(), ""))) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @Before("@annotation(com.agent.biogenie.common.auth.RequiredSuperAdmin) || @within(com.agent.biogenie.common.auth.RequiredSuperAdmin)")
    public void checkSuperAdmin() {
        User user = currentUser();
        if (!AuthConstants.isSuperAdmin(StrUtil.blankToDefault(user.getRole(), ""))) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    private User currentUser() {
        Long userId = AuthContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }
}
