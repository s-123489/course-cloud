package com.zjgsu.syt.coursecloud.enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "catalog-service", fallback = CatalogClientFallback.class)
public interface CatalogClient {

    @GetMapping("/api/courses/{courseId}")
    Map<String, Object> getCourse(@PathVariable("courseId") String courseId);
}
