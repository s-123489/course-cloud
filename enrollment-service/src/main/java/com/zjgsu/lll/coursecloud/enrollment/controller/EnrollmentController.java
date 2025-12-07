package com.zjgsu.lll.coursecloud.enrollment.controller;

import com.zjgsu.lll.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.lll.coursecloud.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private String currentPort;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // ==================== 辅助方法：获取主机名 ====================
    private String getHostname() {
        // 优先使用环境变量 HOSTNAME (Docker 容器中最可靠)
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }

        // 备用方案：使用 InetAddress
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // 忽略异常
        }

        // 最后的 fallback
        return "unknown-" + currentPort;
    }

    // ==================== Enrollment Endpoints ====================
    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentRecord record = enrollmentService.enroll(request.courseId(), request.studentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EnrollmentResponse(
                        record.getId(),
                        record.getCourseId(),
                        record.getStudentId(),
                        record.getEnrolledAt().toString()
                ));
    }

    @GetMapping("/course/{courseId}")
    public List<EnrollmentResponse> listByCourse(@PathVariable String courseId) {
        return enrollmentService.listByCourse(courseId)
                .stream()
                .map(record -> new EnrollmentResponse(
                        record.getId(),
                        record.getCourseId(),
                        record.getStudentId(),
                        record.getEnrolledAt().toString()
                ))
                .toList();
    }

    @GetMapping("/student/{studentId}")
    public List<EnrollmentResponse> listByStudent(@PathVariable String studentId) {
        return enrollmentService.listByStudent(studentId)
                .stream()
                .map(record -> new EnrollmentResponse(
                        record.getId(),
                        record.getCourseId(),
                        record.getStudentId(),
                        record.getEnrolledAt().toString()
                ))
                .toList();
    }

    @GetMapping
    public List<EnrollmentResponse> listAll() {
        return enrollmentService.listAll()
                .stream()
                .map(record -> new EnrollmentResponse(
                        record.getId(),
                        record.getCourseId(),
                        record.getStudentId(),
                        record.getEnrolledAt().toString()
                ))
                .toList();
    }

//    // ==================== 测试接口（负载均衡验证）====================
//    @GetMapping("/test")
//    public Map<String, Object> test() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("service", "enrollment-service");
//        response.put("port", currentPort);
//        response.put("hostname", getHostname());  // ✅ 添加 hostname
//        response.put("timestamp", LocalDateTime.now());
//
//        // 测试调用 user-service
//        try {
//            Map<String, Object> userTest = restTemplate.getForObject(
//                    "http://user-service/api/students/test",
//                    Map.class
//            );
//            response.put("user-service", userTest);
//        } catch (Exception e) {
//            response.put("user-service-error", e.getMessage());
//        }
//
//        // 测试调用 catalog-service
//        try {
//            Map<String, Object> catalogTest = restTemplate.getForObject(
//                    "http://catalog-service/api/courses/test",
//                    Map.class
//            );
//            response.put("catalog-service", catalogTest);
//        } catch (Exception e) {
//            response.put("catalog-service-error", e.getMessage());
//        }
//
//        return response;
//    }

    // ==================== 测试接口(负载均衡验证) ====================
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "enrollment-service");
        response.put("port", currentPort);
        response.put("hostname", getHostname());

        // ★ 添加容器IP
        try {
            response.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            response.put("ip", "unknown");
        }

        response.put("timestamp", LocalDateTime.now());

        // 测试调用 user-service
        try {
            Map<String, Object> userTest = restTemplate.getForObject(
                    "http://user-service/api/students/test",
                    Map.class
            );
            response.put("user-service", userTest);
        } catch (Exception e) {
            response.put("user-service-error", e.getMessage());
        }

        // 测试调用 catalog-service
        try {
            Map<String, Object> catalogTest = restTemplate.getForObject(
                    "http://catalog-service/api/courses/test",
                    Map.class
            );
            response.put("catalog-service", catalogTest);
        } catch (Exception e) {
            response.put("catalog-service-error", e.getMessage());
        }

        return response;
    }

    // ==================== 健康检查接口 ====================
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");
        healthResponse.put("service", "enrollment-service");
        healthResponse.put("port", currentPort);
        healthResponse.put("hostname", getHostname());
        healthResponse.put("timestamp", System.currentTimeMillis());
        return healthResponse;
    }

    // ==================== Record 定义 ====================
    public record EnrollmentRequest(
            @NotBlank String courseId,
            @NotBlank String studentId
    ) {}

    public record EnrollmentResponse(
            String id,
            String courseId,
            String studentId,
            String enrolledAt
    ) {}
}