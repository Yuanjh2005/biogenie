package com.agent.biogenie.module.admin.service;

import cn.hutool.core.util.StrUtil;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.agent.biogenie.module.admin.mapper.ApiBehaviorLogMapper;
import com.agent.biogenie.config.AdminLogProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ApiBehaviorLogService {
    private static final int EXPORT_LIMIT = 100_000;
    private final ApiBehaviorLogMapper mapper;
    private final ArrayBlockingQueue<ApiBehaviorLog> queue;
    private final int flushSize;
    private final AtomicLong droppedCount = new AtomicLong();

    public ApiBehaviorLogService(ApiBehaviorLogMapper mapper, AdminLogProperties properties) {
        this.mapper = mapper;
        this.queue = new ArrayBlockingQueue<>(Math.max(1000, properties.getBehaviorQueueCapacity()));
        this.flushSize = Math.max(1, properties.getBehaviorFlushSize());
    }

    public void record(ApiBehaviorLog row) {
        if (row == null) return;
        if (row.getCreateTime() == null) row.setCreateTime(LocalDateTime.now());
        if (!queue.offer(row)) {
            long dropped = droppedCount.incrementAndGet();
            if (dropped == 1 || dropped % 100 == 0) {
                log.error("API 行为日志队列已满，累计丢弃 {} 条", dropped);
            }
        }
    }

    @Scheduled(fixedDelayString = "${admin.log.behavior-flush-interval-ms:1000}")
    public synchronized void flush() {
        flushOneBatch(true);
    }

    private boolean flushOneBatch(boolean requeueOnFailure) {
        List<ApiBehaviorLog> batch = new ArrayList<>(flushSize);
        queue.drainTo(batch, flushSize);
        if (batch.isEmpty()) return true;
        try {
            mapper.insertBatch(batch);
            return true;
        } catch (Exception e) {
            log.error("批量保存 API 行为日志失败，batchSize={}", batch.size(), e);
            if (requeueOnFailure) {
                int requeued = 0;
                for (ApiBehaviorLog row : batch) if (queue.offer(row)) requeued++;
                int lost = batch.size() - requeued;
                if (lost > 0) droppedCount.addAndGet(lost);
            }
            return false;
        }
    }

    @PreDestroy
    public synchronized void shutdownFlush() {
        int maxBatches = Math.max(1, (queue.size() + flushSize - 1) / flushSize);
        for (int i = 0; i < maxBatches && !queue.isEmpty(); i++) {
            if (!flushOneBatch(false)) break;
        }
        if (!queue.isEmpty()) log.warn("应用关闭时仍有 {} 条 API 行为日志未写入", queue.size());
    }

    int queueSize() { return queue.size(); }
    long droppedCount() { return droppedCount.get(); }

    public Page<ApiBehaviorLog> page(int current, int size, Long userId, String role,
                                     LocalDateTime from, LocalDateTime to) {
        return mapper.selectPage(new Page<>(current, size), wrapper(userId, role, from, to)
                .orderByDesc(ApiBehaviorLog::getCreateTime));
    }

    public List<ApiBehaviorLog> listForExport(Long userId, String role,
                                              LocalDateTime from, LocalDateTime to) {
        return mapper.selectList(wrapper(userId, role, from, to)
                .orderByDesc(ApiBehaviorLog::getCreateTime).last("LIMIT " + EXPORT_LIMIT));
    }

    private LambdaQueryWrapper<ApiBehaviorLog> wrapper(Long userId, String role,
                                                        LocalDateTime from, LocalDateTime to) {
        return new LambdaQueryWrapper<ApiBehaviorLog>()
                .eq(userId != null, ApiBehaviorLog::getUserId, userId)
                .eq(StrUtil.isNotBlank(role), ApiBehaviorLog::getUserRole, role)
                .ge(from != null, ApiBehaviorLog::getCreateTime, from)
                .le(to != null, ApiBehaviorLog::getCreateTime, to);
    }
}
