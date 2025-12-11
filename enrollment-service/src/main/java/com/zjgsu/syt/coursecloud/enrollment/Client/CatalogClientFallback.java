package com.zjgsu.syt.coursecloud.enrollment.client;

import com.zjgsu.syt.coursecloud.enrollment.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
public class CatalogClientFallback implements CatalogClient {

    @Override
    public Map<String, Object> getCourse(String courseId) {
        log.warn("CatalogClient fallback triggered for course: {}", courseId);
        throw new ServiceUnavailableException("课程服务暂时不可用，请稍后再试");
    }
}
