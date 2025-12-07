package com.zjgsu.lll.coursecloud.user.controller;

import com.zjgsu.lll.coursecloud.user.model.Student;
import com.zjgsu.lll.coursecloud.user.model.Teacher;
import com.zjgsu.lll.coursecloud.user.service.UserService;
import jakarta.validation.Valid;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Value("${server.port}")
    private String currentPort;

    public UserController(UserService userService) {
        this.userService = userService;
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

    // ==================== Student Endpoints ====================
    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createStudent(@Valid @RequestBody StudentRequest request) {
        log.info("User Service [port: {}, hostname: {}] creating student: {}",
                currentPort, getHostname(), request.studentId());

        Student student = new Student(
                request.username(),
                request.email(),
                request.studentId(),
                request.name(),
                request.major(),
                request.grade()
        );
        Student created = userService.createStudent(student);

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", StudentResponse.from(created));
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/students")
    public Map<String, Object> getAllStudents() {
        log.info("User Service [port: {}, hostname: {}] getting all students",
                currentPort, getHostname());

        List<StudentResponse> students = userService.getAllStudents().stream()
                .map(StudentResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", students);
        response.put("count", students.size());
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable String id) {
        log.info("User Service [port: {}, hostname: {}] getting student by id: {}",
                currentPort, getHostname(), id);

        return userService.getStudentById(id)
                .map(student -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", StudentResponse.from(student));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Student with id " + id + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @GetMapping("/students/studentId/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentByStudentId(@PathVariable String studentId) {
        log.info("User Service [port: {}, hostname: {}] getting student by studentId: {}",
                currentPort, getHostname(), studentId);

        return userService.getStudentByStudentId(studentId)
                .map(student -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", StudentResponse.from(student));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Student with studentId " + studentId + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable String id,
            @Valid @RequestBody StudentRequest request) {
        log.info("User Service [port: {}, hostname: {}] updating student: {}",
                currentPort, getHostname(), id);

        return userService.getStudentById(id)
                .map(existing -> {
                    existing.setUsername(request.username());
                    existing.setEmail(request.email());
                    existing.setStudentId(request.studentId());
                    existing.setName(request.name());
                    existing.setMajor(request.major());
                    existing.setGrade(request.grade());
                    Student updated = userService.updateStudent(existing);

                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", StudentResponse.from(updated));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Student with id " + id + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable String id) {
        log.info("User Service [port: {}, hostname: {}] deleting student: {}",
                currentPort, getHostname(), id);
        userService.deleteStudent(id);
    }

    // ==================== Teacher Endpoints ====================
    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTeacher(@Valid @RequestBody TeacherRequest request) {
        log.info("User Service [port: {}, hostname: {}] creating teacher: {}",
                currentPort, getHostname(), request.teacherId());

        Teacher teacher = new Teacher(
                request.username(),
                request.email(),
                request.teacherId(),
                request.name(),
                request.department(),
                request.title()
        );
        Teacher created = userService.createTeacher(teacher);

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", TeacherResponse.from(created));
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/teachers")
    public Map<String, Object> getAllTeachers() {
        log.info("User Service [port: {}, hostname: {}] getting all teachers",
                currentPort, getHostname());

        List<TeacherResponse> teachers = userService.getAllTeachers().stream()
                .map(TeacherResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("port", currentPort);
        response.put("hostname", getHostname());
        response.put("data", teachers);
        response.put("count", teachers.size());
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/teachers/{id}")
    public ResponseEntity<Map<String, Object>> getTeacherById(@PathVariable String id) {
        log.info("User Service [port: {}, hostname: {}] getting teacher by id: {}",
                currentPort, getHostname(), id);

        return userService.getTeacherById(id)
                .map(teacher -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", TeacherResponse.from(teacher));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Teacher with id " + id + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @GetMapping("/teachers/teacherId/{teacherId}")
    public ResponseEntity<Map<String, Object>> getTeacherByTeacherId(@PathVariable String teacherId) {
        log.info("User Service [port: {}, hostname: {}] getting teacher by teacherId: {}",
                currentPort, getHostname(), teacherId);

        return userService.getTeacherByTeacherId(teacherId)
                .map(teacher -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("port", currentPort);
                    response.put("hostname", getHostname());
                    response.put("data", TeacherResponse.from(teacher));
                    response.put("status", "SUCCESS");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("port", currentPort);
                    errorResponse.put("hostname", getHostname());
                    errorResponse.put("status", "ERROR");
                    errorResponse.put("message", "Teacher with teacherId " + teacherId + " not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                });
    }

    @DeleteMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable String id) {
        log.info("User Service [port: {}, hostname: {}] deleting teacher: {}",
                currentPort, getHostname(), id);
        userService.deleteTeacher(id);
    }

    // ==================== Health Check ====================
    @GetMapping("/actuator/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("port", currentPort);
        health.put("hostname", getHostname());
        health.put("service", "user-service");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    // ==================== 测试接口（负载均衡验证）====================
//    @GetMapping("/students/test")
//    public Map<String, Object> testStudent() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("service", "user-service");
//        response.put("endpoint", "/api/students/test");
//        response.put("port", currentPort);
//        //response.put("hostname", getHostname());  // ✅ 关键：添加 hostname
//        response.put("timestamp", LocalDateTime.now());
//        response.put("status", "UP");
//        return response;
//    }

    // ==================== 测试接口(负载均衡验证) ====================
    @GetMapping("/students/test")
    public Map<String, Object> testStudent() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("endpoint", "/api/students/test");
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
    // ==================== Record 定义 ====================
    public record StudentRequest(
            String username,
            String email,
            String studentId,
            String name,
            String major,
            Integer grade
    ) {}

    public record StudentResponse(
            String id,
            String username,
            String email,
            String studentId,
            String name,
            String major,
            Integer grade,
            String createdAt
    ) {
        public static StudentResponse from(Student student) {
            return new StudentResponse(
                    student.getId(),
                    student.getUsername(),
                    student.getEmail(),
                    student.getStudentId(),
                    student.getName(),
                    student.getMajor(),
                    student.getGrade(),
                    student.getCreatedAt().toString()
            );
        }
    }

    public record TeacherRequest(
            String username,
            String email,
            String teacherId,
            String name,
            String department,
            String title
    ) {}

    public record TeacherResponse(
            String id,
            String username,
            String email,
            String teacherId,
            String name,
            String department,
            String title
    ) {
        public static TeacherResponse from(Teacher teacher) {
            return new TeacherResponse(
                    teacher.getId(),
                    teacher.getUsername(),
                    teacher.getEmail(),
                    teacher.getTeacherId(),
                    teacher.getName(),
                    teacher.getDepartment(),
                    teacher.getTitle()
            );
        }
    }
}