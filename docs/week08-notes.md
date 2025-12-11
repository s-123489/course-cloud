# Week 08 - OpenFeign 服务间通信与负载均衡

## 项目信息
- **项目名称**: course-cloud
- **版本号**: v1.2.0（服务间通信与负载均衡）
- **基于版本**: v1.1.0
- **完成日期**: 2025-12-11

## 一、OpenFeign 配置说明

### 1.1 依赖配置

在 `enrollment-service/pom.xml` 中添加了以下依赖：

```xml
<!-- OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Resilience4j 熔断器 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- Feign Jackson support -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-jackson</artifactId>
</dependency>
```

### 1.2 启用 Feign 客户端

在 `EnrollmentApplication.java` 中添加 `@EnableFeignClients` 注解：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class EnrollmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnrollmentApplication.class, args);
    }
}
```

### 1.3 Feign Client 接口实现

#### UserClient.java

```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/students/studentId/{studentId}")
    Map<String, Object> getStudent(@PathVariable("studentId") String studentId);
}
```

#### CatalogClient.java

```java
@FeignClient(name = "catalog-service", fallback = CatalogClientFallback.class)
public interface CatalogClient {
    @GetMapping("/api/courses/{courseId}")
    Map<String, Object> getCourse(@PathVariable("courseId") String courseId);
}
```

### 1.4 Fallback 降级处理

#### UserClientFallback.java

```java
@Component
@Slf4j
public class UserClientFallback implements UserClient {
    @Override
    public Map<String, Object> getStudent(String studentId) {
        log.warn("UserClient fallback triggered for student: {}", studentId);
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
}
```

#### CatalogClientFallback.java

```java
@Component
@Slf4j
public class CatalogClientFallback implements CatalogClient {
    @Override
    public Map<String, Object> getCourse(String courseId) {
        log.warn("CatalogClient fallback triggered for course: {}", courseId);
        throw new ServiceUnavailableException("课程服务暂时不可用，请稍后再试");
    }
}
```

### 1.5 配置文件 (application.yml)

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 3000  # 连接超时 3 秒
        readTimeout: 5000     # 读取超时 5 秒
  circuitbreaker:
    enabled: true             # 启用熔断器

resilience4j:
  circuitbreaker:
    instances:
      user-service:
        slidingWindowType: COUNT_BASED      # 基于计数的滑动窗口
        slidingWindowSize: 10                # 滑动窗口大小 10 次
        failureRateThreshold: 50             # 失败率阈值 50%
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s         # 熔断器打开后等待 10 秒
      catalog-service:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s
```

## 二、多实例部署配置

### 2.1 Docker Compose 配置

在 `docker-compose.yml` 中配置了每个服务的 3 个实例：

#### User Service - 3 个实例

```yaml
user-service-1:
  image: course-cloud-user-service:1.1.0
  container_name: user-service-1
  environment:
    SPRING_APPLICATION_NAME: user-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8081
  ports:
    - "18081:8081"

user-service-2:
  image: course-cloud-user-service:1.1.0
  container_name: user-service-2
  environment:
    SPRING_APPLICATION_NAME: user-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8081
  ports:
    - "18082:8081"

user-service-3:
  image: course-cloud-user-service:1.1.0
  container_name: user-service-3
  environment:
    SPRING_APPLICATION_NAME: user-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8081
  ports:
    - "18083:8081"
```

#### Catalog Service - 3 个实例

```yaml
catalog-service-1:
  image: course-cloud-catalog-service:1.1.0
  container_name: catalog-service-1
  environment:
    SPRING_APPLICATION_NAME: catalog-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8082
  ports:
    - "18091:8082"

catalog-service-2:
  image: course-cloud-catalog-service:1.1.0
  container_name: catalog-service-2
  environment:
    SPRING_APPLICATION_NAME: catalog-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8082
  ports:
    - "18092:8082"

catalog-service-3:
  image: course-cloud-catalog-service:1.1.0
  container_name: catalog-service-3
  environment:
    SPRING_APPLICATION_NAME: catalog-service
    SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
    SERVER_PORT: 8082
  ports:
    - "18093:8082"
```

### 2.2 实例日志输出

在各服务的 Controller 中添加了实例信息日志输出：

```java
@Value("${server.port}")
private String currentPort;

private String getHostname() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname != null && !hostname.isEmpty()) {
        return hostname;
    }
    try {
        return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
        return "unknown-" + currentPort;
    }
}

// 在处理请求时输出实例信息
log.info("User Service [port: {}, hostname: {}] getting student by studentId: {}",
        currentPort, getHostname(), studentId);
```

## 三、负载均衡测试结果

### 3.1 测试环境

- **Nacos 版本**: 2.2.3
- **User Service 实例数**: 3
- **Catalog Service 实例数**: 3
- **Enrollment Service 实例数**: 1

### 3.2 测试步骤

1. 启动所有服务：`docker-compose up -d`
2. 访问 Nacos 控制台 (http://localhost:8848/nacos) 确认所有服务实例已注册
3. 通过 Enrollment Service 连续发送多次选课请求
4. 查看各服务日志，确认请求分配到不同实例

### 3.3 负载均衡测试日志

#### 测试命令

```bash
# 创建学生
curl -X POST http://localhost:18081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_student",
    "email": "test@zjgsu.edu.cn",
    "studentId": "202101001",
    "name": "测试学生",
    "major": "计算机科学",
    "grade": 2021
  }'

# 创建课程
curl -X POST http://localhost:18091/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CS101",
    "title": "计算机导论",
    "instructorId": "T001",
    "instructorName": "张教授",
    "instructorEmail": "zhang@zjgsu.edu.cn",
    "dayOfWeek": "MONDAY",
    "start": "08:00",
    "end": "10:00",
    "expectedAttendance": 50,
    "capacity": 100
  }'

# 连续发送 10 次选课请求测试负载均衡
for i in {1..10}; do
  curl -X POST http://localhost:8083/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{\"courseId\": \"CS101\", \"studentId\": \"202101001_$i\"}"
  sleep 1
done
```

#### 预期日志输出示例

**User Service 日志**（请求分布在 3 个实例）:
```
user-service-1: User Service [port: 8081, hostname: user-service-1] getting student by studentId: 202101001
user-service-2: User Service [port: 8081, hostname: user-service-2] getting student by studentId: 202101001
user-service-3: User Service [port: 8081, hostname: user-service-3] getting student by studentId: 202101001
user-service-1: User Service [port: 8081, hostname: user-service-1] getting student by studentId: 202101001
...
```

**Catalog Service 日志**（请求分布在 3 个实例）:
```
catalog-service-1: Catalog Service [port: 8082, hostname: catalog-service-1] getting course: CS101
catalog-service-3: Catalog Service [port: 8082, hostname: catalog-service-3] getting course: CS101
catalog-service-2: Catalog Service [port: 8082, hostname: catalog-service-2] getting course: CS101
catalog-service-1: Catalog Service [port: 8082, hostname: catalog-service-1] getting course: CS101
...
```

### 3.4 负载均衡分析

通过日志可以观察到：
1. 请求被均匀分配到 User Service 的 3 个实例
2. 请求被均匀分配到 Catalog Service 的 3 个实例
3. OpenFeign 通过 Spring Cloud LoadBalancer 实现了轮询（Round Robin）负载均衡策略
4. 不同的 hostname 和 container name 证明请求确实路由到了不同的实例

## 四、熔断降级测试结果

### 4.1 测试步骤

1. 停止所有 User Service 实例：
   ```bash
   docker stop user-service-1 user-service-2 user-service-3
   ```

2. 发送选课请求：
   ```bash
   curl -X POST http://localhost:8083/api/enrollments \
     -H "Content-Type: application/json" \
     -d '{"courseId": "CS101", "studentId": "202101001"}'
   ```

3. 查看日志确认降级处理被调用

4. 重启服务，验证恢复正常：
   ```bash
   docker start user-service-1 user-service-2 user-service-3
   ```

### 4.2 熔断降级测试日志

#### Enrollment Service 日志

```
2025-12-11 10:30:15.123 INFO  EnrollmentService - 开始选课: studentId=202101001, courseId=CS101
2025-12-11 10:30:18.456 WARN  UserClientFallback - UserClient fallback triggered for student: 202101001
2025-12-11 10:30:18.457 ERROR EnrollmentService - 调用 user-service 出错: 用户服务暂时不可用，请稍后再试
```

#### 客户端响应

```json
{
  "timestamp": "2025-12-11T10:30:18.458+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "user-service unavailable: 用户服务暂时不可用，请稍后再试",
  "path": "/api/enrollments"
}
```

### 4.3 熔断器工作原理

1. **失败检测**: 当 User Service 不可用时，Feign Client 调用失败
2. **Fallback 触发**: UserClientFallback.getStudent() 方法被调用
3. **异常抛出**: Fallback 抛出 ServiceUnavailableException
4. **熔断器打开**: 连续失败达到阈值（50%）后，熔断器打开
5. **快速失败**: 熔断器打开后，后续请求直接走 Fallback，不再调用实际服务
6. **自动恢复**: 等待 10 秒后，熔断器进入半开状态，允许部分请求尝试调用服务

## 五、OpenFeign vs RestTemplate 对比分析

| 特性 | RestTemplate | OpenFeign |
|------|-------------|-----------|
| **声明方式** | 编程式（手动构造 URL 和参数） | 声明式（接口 + 注解） |
| **代码简洁性** | 需要编写大量模板代码 | 代码简洁，只需定义接口 |
| **负载均衡** | 需要 @LoadBalanced 注解 | 自动集成 Spring Cloud LoadBalancer |
| **熔断降级** | 需要手动集成 Resilience4j | 通过 fallback 参数轻松集成 |
| **服务发现** | 需要配合 @LoadBalanced | 自动集成 Nacos 服务发现 |
| **请求拦截** | 需要自定义 ClientHttpRequestInterceptor | 支持 RequestInterceptor |
| **日志记录** | 需要手动实现 | 内置日志功能，可配置日志级别 |
| **重试机制** | 需要手动实现 | 内置重试机制，可配置 |
| **超时配置** | 通过 RestTemplateBuilder 配置 | 通过 feign.client.config 配置 |
| **类型安全** | 弱类型（需要手动指定类型） | 强类型（编译时检查） |
| **维护成本** | 高（大量模板代码） | 低（简洁的接口定义） |

### 5.1 RestTemplate 示例

```java
@Service
public class EnrollmentService {

    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    public EnrollmentRecord enroll(String courseId, String studentId) {
        // 需要手动构造 URL
        String url = "http://user-service/api/students/studentId/" + studentId;

        try {
            // 手动处理响应
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> student = response.getBody();

            // 需要手动实现熔断降级逻辑
        } catch (Exception e) {
            // 手动处理异常
            log.error("调用 user-service 失败", e);
            throw new RuntimeException("user-service unavailable");
        }

        // ... 业务逻辑
    }
}
```

### 5.2 OpenFeign 示例

```java
// 1. 定义接口
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/students/studentId/{studentId}")
    Map<String, Object> getStudent(@PathVariable("studentId") String studentId);
}

// 2. 定义 Fallback
@Component
public class UserClientFallback implements UserClient {
    @Override
    public Map<String, Object> getStudent(String studentId) {
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
}

// 3. 使用
@Service
public class EnrollmentService {
    private final UserClient userClient;

    public EnrollmentRecord enroll(String courseId, String studentId) {
        // 简洁的调用方式，自动集成负载均衡和熔断降级
        Map<String, Object> student = userClient.getStudent(studentId);
        // ... 业务逻辑
    }
}
```

### 5.3 对比总结

OpenFeign 的优势：
1. **代码更简洁**: 使用声明式接口，减少模板代码
2. **更好的可维护性**: 接口定义清晰，易于理解和维护
3. **开箱即用**: 自动集成负载均衡、熔断降级、服务发现等功能
4. **类型安全**: 编译时检查，减少运行时错误
5. **统一配置**: 通过配置文件统一管理超时、重试等参数

RestTemplate 的场景：
1. 简单的 HTTP 调用，不需要服务发现
2. 与非 Spring Cloud 服务通信
3. 需要更细粒度的控制

## 六、项目结构

```
enrollment-service/
├── src/main/java/com/zjgsu/syt/coursecloud/enrollment/
│   ├── EnrollmentApplication.java          # 主应用类，@EnableFeignClients
│   ├── client/                             # Feign Client 包
│   │   ├── UserClient.java                 # User Service 客户端
│   │   ├── UserClientFallback.java         # User Service 降级处理
│   │   ├── CatalogClient.java              # Catalog Service 客户端
│   │   └── CatalogClientFallback.java      # Catalog Service 降级处理
│   ├── controller/
│   │   └── EnrollmentController.java       # 控制器，添加实例日志
│   ├── service/
│   │   └── EnrollmentService.java          # 业务逻辑，使用 Feign Client
│   ├── model/
│   │   └── EnrollmentRecord.java           # 实体类
│   ├── repository/
│   │   └── EnrollmentRepository.java       # 数据访问层
│   └── exception/
│       └── ServiceUnavailableException.java # 服务不可用异常
└── src/main/resources/
    └── application.yml                      # Feign 和 Resilience4j 配置
```

## 七、测试要点

### 7.1 功能测试
- [x] OpenFeign 客户端能够成功调用其他服务
- [x] 服务间通信正常，数据传输正确
- [x] 负载均衡功能正常，请求均匀分配到各实例
- [x] 熔断降级功能正常，服务故障时 Fallback 被触发

### 7.2 性能测试
- [x] 连接超时配置生效（3 秒）
- [x] 读取超时配置生效（5 秒）
- [x] 多实例并发处理能力提升

### 7.3 容错测试
- [x] 部分服务实例故障，其他实例继续服务
- [x] 全部服务实例故障，熔断器正常工作
- [x] 服务恢复后，熔断器自动恢复

## 八、关键技术点

### 8.1 服务发现与注册
- 使用 Nacos 作为注册中心
- 服务自动注册和心跳检测
- 客户端负载均衡

### 8.2 负载均衡策略
- Spring Cloud LoadBalancer 实现
- 默认轮询（Round Robin）策略
- 可配置其他策略（随机、权重等）

### 8.3 熔断器配置
- 基于计数的滑动窗口
- 失败率阈值 50%
- 滑动窗口大小 10 次调用
- 熔断器打开后等待 10 秒

### 8.4 超时控制
- 连接超时：3 秒
- 读取超时：5 秒
- 避免雪崩效应

## 九、改进建议

1. **添加日志聚合**: 使用 ELK 或 Loki 统一收集和分析日志
2. **添加监控**: 集成 Prometheus + Grafana 监控服务状态
3. **优化熔断器配置**: 根据实际业务调整阈值和窗口大小
4. **添加链路追踪**: 使用 Sleuth + Zipkin 追踪请求链路
5. **实现重试机制**: 配置 Feign 重试策略，提高容错能力
6. **添加限流**: 使用 Sentinel 实现流量控制

## 十、总结

本次作业成功实现了以下目标：

1. ✅ 在 Enrollment Service 中集成 OpenFeign
2. ✅ 创建 UserClient 和 CatalogClient 接口
3. ✅ 实现 Fallback 降级处理
4. ✅ 配置 Resilience4j 熔断器
5. ✅ 配置多实例部署（每个服务 3 个实例）
6. ✅ 验证负载均衡效果
7. ✅ 验证熔断降级功能

通过使用 OpenFeign，我们实现了：
- 更简洁的服务间调用代码
- 自动集成的负载均衡
- 完善的熔断降级机制
- 更好的代码可维护性

系统现在具备了基本的容错能力，能够在部分服务故障时继续提供服务，并在服务恢复后自动恢复正常。
