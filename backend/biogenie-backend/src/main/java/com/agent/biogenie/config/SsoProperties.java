package com.agent.biogenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部系统 SSO 免登配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sso")
public class SsoProperties {

    /** ticket 有效期（秒） */
    private long ticketTtlSeconds = 1800;

    /** 允许接入的外部客户端 */
    private List<Client> clients = new ArrayList<>();

    public Client findClient(String clientId) {
        if (clientId == null || clients == null) {
            return null;
        }
        return clients.stream()
                .filter(c -> clientId.equals(c.getClientId()))
                .findFirst()
                .orElse(null);
    }

    @Data
    public static class Client {
        /** 客户端标识 */
        private String clientId;
        /** 客户端密钥（仅服务端换票使用） */
        private String clientSecret;
        /** 写入用户表的来源标识，如 PARTNER_A */
        private String source;
    }
}
