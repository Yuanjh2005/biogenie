package com.agent.biogenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BioGenie Backend 启动类
 */
@SpringBootApplication
@EnableScheduling
public class BiogenieBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiogenieBackendApplication.class, args);
    }
}
