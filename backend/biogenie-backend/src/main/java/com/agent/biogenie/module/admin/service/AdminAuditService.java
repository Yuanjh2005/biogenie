package com.agent.biogenie.module.admin.service;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.module.admin.entity.AdminAuditLog;
import com.agent.biogenie.module.admin.mapper.AdminAuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private static final int EXPORT_LIMIT = 100_000;
    private final AdminAuditLogMapper mapper;

    public void record(Long operatorId, String action, String targetType, String targetId, String detail) {
        AdminAuditLog row = new AdminAuditLog();
        row.setOperatorId(operatorId);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setDetail(StrUtil.maxLength(detail, 1000));
        row.setIp(currentIp());
        mapper.insert(row);
    }

    public Page<AdminAuditLog> page(int current, int size, String action) {
        return mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<AdminAuditLog>()
                        .eq(StrUtil.isNotBlank(action), AdminAuditLog::getAction, action)
                        .orderByDesc(AdminAuditLog::getCreateTime));
    }

    public List<AdminAuditLog> listForExport(String action, LocalDateTime from, LocalDateTime to) {
        return mapper.selectList(new LambdaQueryWrapper<AdminAuditLog>()
                .eq(StrUtil.isNotBlank(action), AdminAuditLog::getAction, action)
                .ge(from != null, AdminAuditLog::getCreateTime, from)
                .le(to != null, AdminAuditLog::getCreateTime, to)
                .orderByDesc(AdminAuditLog::getCreateTime)
                .last("LIMIT " + EXPORT_LIMIT));
    }

    private String currentIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip)) ip = request.getHeader("X-Real-IP");
        if (StrUtil.isBlank(ip)) ip = request.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.substring(0, ip.indexOf(',')).trim() : ip;
    }
}
