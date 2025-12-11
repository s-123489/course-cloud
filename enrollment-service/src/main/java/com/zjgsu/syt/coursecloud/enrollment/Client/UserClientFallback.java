package com.zjgsu.syt.coursecloud.enrollment.client;

import com.zjgsu.syt.coursecloud.enrollment.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public Map<String, Object> getStudent(String studentId) {
        log.warn("UserClient fallback triggered for student: {}", studentId);
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
}
