package com.zjgsu.syt.coursecloud.enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/students/studentId/{studentId}")
    Map<String, Object> getStudent(@PathVariable("studentId") String studentId);
}
