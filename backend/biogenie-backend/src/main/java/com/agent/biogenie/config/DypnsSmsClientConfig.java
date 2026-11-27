package com.agent.biogenie.config;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云号码认证短信 AsyncClient
 */
@Configuration
@RequiredArgsConstructor
public class DypnsSmsClientConfig {

    private final SmsProperties smsProperties;

    @Bean(destroyMethod = "close")
    public AsyncClient dypnsAsyncClient() {
        StaticCredentialProvider provider = StaticCredentialProvider.create(
                Credential.builder()
                        .accessKeyId(smsProperties.getAccessKeyId())
                        .accessKeySecret(smsProperties.getAccessKeySecret())
                        .build()
        );

        return AsyncClient.builder()
                .region(smsProperties.getRegion())
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(smsProperties.getEndpoint())
                )
                .build();
    }
}
