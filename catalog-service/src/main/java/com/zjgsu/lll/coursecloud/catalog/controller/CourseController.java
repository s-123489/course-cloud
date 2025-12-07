package com.zjgsu.lll.coursecloud.catalog.controller;

import com.zjgsu.lll.coursecloud.catalog.model.Course;
import com.zjgsu.lll.coursecloud.catalog.model.Instructor;
import com.zjgsu.lll.coursecloud.catalog.model.ScheduleSlot;
import com.zjgsu.lll.coursecloud.catalog.repository.CourseRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseRepository repository;

    @Value("${server.port}")
    private String currentPort;

    public CourseController(CourseRepository repository) {
        this.repository = repository;
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
            log.warn("Failed to get hostname: {}", e.getMessage());
        }

        // 最后的 fallback
        return "unknown-" + currentPort;
    }

    // ==================== Course Endpoints ====================
    @GetMapping
    public Map<String, Object> listCourses() {
        log.info("Catalog Service [port: {}, hostname: {}] listing all courses",
                currentPort, getHostname());

        List<CourseResponse> courses = repository.findAll()
                .stream()
                .map(CourseResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", courses);
        response.put("count", courses.size());
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCourse(@PathVariable String id) {
        log.info("Catalog Service [port: {}, hostname: {}] getting course: {}",
                currentPort, getHostname(), id);

        return repository.findById(id)
                .map(course -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", CourseResponse.from(course));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Course with id " + id + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCourse(@Valid @RequestBody CourseRequest request) {
        log.info("Catalog Service [port: {}, hostname: {}] creating course: {}",
                currentPort, getHostname(), request.code());

        Course course = new Course(
                request.code(),
                request.title(),
                new Instructor(request.instructorId(), request.instructorName(), request.instructorEmail()),
                new ScheduleSlot(
                        request.dayOfWeek().toDayOfWeek(),
                        LocalTime.parse(request.start()),
                        LocalTime.parse(request.end()),
                        request.expectedAttendance()
                ),
                request.capacity()
        );
        Course saved = repository.save(course);

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", CourseResponse.from(saved));
        response.put("status", "SUCCESS");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== 测试接口（负载均衡验证）====================
//    @GetMapping("/test")
//    public Map<String, Object> test() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("service", "catalog-service");
//        response.put("port", currentPort);
//        //response.put("hostname", getHostname());  // ✅ 关键：添加 hostname
//        response.put("timestamp", LocalDateTime.now());
//        response.put("status", "UP");
//        return response;
//    }
    // ==================== 测试接口(负载均衡验证) ====================
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "catalog-service");
        response.put("port", currentPort);
        response.put("hostname", getHostname());

        // ★ 添加容器IP,用于负载均衡测试
        try {
            response.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            response.put("ip", "unknown");
            log.warn("Failed to get IP address: {}", e.getMessage());
        }

        response.put("timestamp", LocalDateTime.now());
        response.put("status", "UP");
        return response;
    }

    // ==================== 健康检查接口 ====================
    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("port", currentPort);
        health.put("hostname", getHostname());
        health.put("service", "catalog-service");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}