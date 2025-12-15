package com.zjgsu.syt.coursecloud.user.api;

import com.zjgsu.syt.coursecloud.user.model.User;
import com.zjgsu.syt.coursecloud.user.repository.UserRepository;
import com.zjgsu.syt.coursecloud.user.util.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 认证控制器
 * 处理用户登录和注册请求
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        // 1. 验证用户名和密码
        Optional<User> userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User not found - {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户名或密码错误"));
        }

        User user = userOpt.get();

        // 验证密码（注意：这里使用明文比较，生产环境应该使用加密）
        if (!user.getPassword().equals(request.password())) {
            log.warn("Login failed: Invalid password for user - {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户名或密码错误"));
        }

        // 2. 生成 JWT Token
        String role = user.getUserType().name();  // STUDENT 或 TEACHER
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                role
        );

        log.info("Login successful for user: {} (ID: {}, Role: {})", user.getUsername(), user.getId(), role);

        // 3. 返回 Token 和用户信息
        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                role
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册（可选）
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register attempt for username: {}", request.username());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed: Username already exists - {}", request.username());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "用户名已存在"));
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: Email already exists - {}", request.email());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "邮箱已存在"));
        }

        log.info("User registered successfully: {}", request.username());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "用户注册成功（完整实现需要创建具体的 Student/Teacher 对象）"));
    }

    /**
     * 登录请求
     */
    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}

    /**
     * 登录响应
     */
    public record LoginResponse(
            String token,
            String userId,
            String username,
            String email,
            String role
    ) {}

    /**
     * 注册请求
     */
    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password,
            @NotBlank(message = "邮箱不能为空") String email
    ) {}
}
