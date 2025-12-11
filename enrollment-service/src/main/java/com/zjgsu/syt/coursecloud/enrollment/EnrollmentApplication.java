package com.zjgsu.syt.coursecloud.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients  // ✅ 启用 Feign 客户端扫描
public class EnrollmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnrollmentApplication.class, args);
    }
}
