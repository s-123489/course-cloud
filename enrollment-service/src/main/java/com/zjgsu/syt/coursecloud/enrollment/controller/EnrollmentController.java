package com.zjgsu.syt.coursecloud.enrollment.controller;

import com.zjgsu.syt.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.syt.coursecloud.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

    private final EnrollmentService enrollmentService;

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
        log.info("Enrollment Service [port: {}, hostname: {}] processing enrollment: studentId={}, courseId={}",
                currentPort, getHostname(), request.studentId(), request.courseId());

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
        log.info("Enrollment Service [port: {}, hostname: {}] listing enrollments for course: {}",
                currentPort, getHostname(), courseId);

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
        log.info("Enrollment Service [port: {}, hostname: {}] listing enrollments for student: {}",
                currentPort, getHostname(), studentId);

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
        log.info("Enrollment Service [port: {}, hostname: {}] listing all enrollments",
                currentPort, getHostname());

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
        response.put("message", "Enrollment Service is running with OpenFeign integration");

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