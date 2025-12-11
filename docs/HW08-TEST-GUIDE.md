# HW08 测试指南

## 环境要求

- Docker 和 Docker Compose
- curl 命令
- jq 命令（可选，用于格式化 JSON 输出）

## 启动服务

### 1. 构建镜像并启动所有服务

```bash
# 进入项目根目录
cd course-cloud-hw07

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 2. 访问 Nacos 控制台

- URL: http://localhost:8848/nacos
- 用户名: nacos
- 密码: nacos

确认以下服务实例已注册：
- user-service (3 个实例)
- catalog-service (3 个实例)
- enrollment-service (1 个实例)

## 运行测试

### 测试 1: 负载均衡测试

使用提供的测试脚本：

```bash
# 在 Windows Git Bash 中运行
bash scripts/test-load-balance.sh

# 或者在 Linux/macOS 中运行
chmod +x scripts/test-load-balance.sh
./scripts/test-load-balance.sh
```

测试脚本将：
1. 检查所有服务健康状态
2. 创建测试数据（学生和课程）
3. 发送 10 次选课请求
4. 显示服务实例日志
5. 验证负载均衡效果

**预期结果**：
- 请求被均匀分配到 3 个 User Service 实例
- 请求被均匀分配到 3 个 Catalog Service 实例
- 日志中显示不同的 hostname (user-service-1, user-service-2, user-service-3)

### 测试 2: 熔断降级测试

使用提供的测试脚本：

```bash
# 在 Windows Git Bash 中运行
bash scripts/test-circuit-breaker.sh

# 或者在 Linux/macOS 中运行
chmod +x scripts/test-circuit-breaker.sh
./scripts/test-circuit-breaker.sh
```

测试脚本将：
1. 测试正常情况
2. 停止所有 User Service 实例
3. 验证熔断降级被触发
4. 重启 User Service 并验证恢复
5. 停止所有 Catalog Service 实例
6. 验证熔断降级被触发
7. 恢复所有服务

**预期结果**：
- User Service 不可用时，返回"用户服务暂时不可用"
- Catalog Service 不可用时，返回"课程服务暂时不可用"
- 服务恢复后，系统自动恢复正常

## 手动测试

### 1. 创建学生

```bash
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
```

### 2. 创建课程

```bash
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
```

### 3. 选课（测试负载均衡）

连续发送多次请求，观察负载均衡效果：

```bash
for i in {1..10}; do
  curl -X POST http://localhost:8083/api/enrollments \
    -H "Content-Type: application/json" \
    -d "{\"courseId\": \"CS101\", \"studentId\": \"student_$i\"}"
  echo ""
  sleep 1
done
```

### 4. 查看服务日志

```bash
# User Service 实例日志
docker logs -f user-service-1
docker logs -f user-service-2
docker logs -f user-service-3

# Catalog Service 实例日志
docker logs -f catalog-service-1
docker logs -f catalog-service-2
docker logs -f catalog-service-3

# Enrollment Service 日志
docker logs -f enrollment-service
```

### 5. 测试熔断降级

**停止 User Service：**
```bash
docker stop user-service-1 user-service-2 user-service-3
```

**发送选课请求：**
```bash
curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId": "CS101", "studentId": "202101001"}'
```

**预期响应：**
```json
{
  "timestamp": "2025-12-11T10:30:18.458+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "user-service unavailable: 用户服务暂时不可用，请稍后再试",
  "path": "/api/enrollments"
}
```

**重启服务：**
```bash
docker start user-service-1 user-service-2 user-service-3
```

## 查看 Nacos 服务列表

访问 Nacos 控制台，在"服务管理" -> "服务列表"中：

1. 点击 `user-service`，查看 3 个实例
2. 点击 `catalog-service`，查看 3 个实例
3. 点击 `enrollment-service`，查看 1 个实例

每个实例应该显示：
- IP 地址
- 端口号
- 健康状态（健康/不健康）
- 权重

## 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 故障排查

### 服务无法启动

1. 检查端口占用：
   ```bash
   netstat -ano | findstr "8081"
   netstat -ano | findstr "8082"
   netstat -ano | findstr "8083"
   ```

2. 查看服务日志：
   ```bash
   docker logs service-name
   ```

### 服务无法注册到 Nacos

1. 检查 Nacos 是否启动：
   ```bash
   docker logs nacos
   ```

2. 检查服务配置：
   - `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` 应该设置为 `nacos:8848`
   - 确保服务在同一 Docker 网络中

### 熔断降级不生效

1. 检查 Feign 配置：
   ```yaml
   feign:
     circuitbreaker:
       enabled: true
   ```

2. 检查 Resilience4j 配置：
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         user-service:
           failureRateThreshold: 50
           slidingWindowSize: 10
   ```

3. 查看日志确认 Fallback 是否被调用

## 性能测试

使用 Apache Bench 或其他工具进行压力测试：

```bash
# 安装 Apache Bench
# Windows: 下载 Apache HTTP Server
# Linux: apt-get install apache2-utils
# macOS: brew install httpd

# 发送 100 个请求，并发数 10
ab -n 100 -c 10 -p enrollment.json -T application/json \
  http://localhost:8083/api/enrollments

# enrollment.json 内容：
# {"courseId": "CS101", "studentId": "202101001"}
```

## 关键配置说明

### OpenFeign 配置

- **连接超时**: 3 秒
- **读取超时**: 5 秒
- **熔断器**: 启用

### Resilience4j 配置

- **滑动窗口类型**: 基于计数 (COUNT_BASED)
- **滑动窗口大小**: 10 次调用
- **失败率阈值**: 50%
- **半开状态允许调用数**: 3 次
- **熔断器打开后等待时间**: 10 秒

### 负载均衡策略

- **默认策略**: 轮询 (Round Robin)
- **实现**: Spring Cloud LoadBalancer

## 参考文档

- [Week08 实现文档](docs/week08-notes.md)
- [OpenFeign 官方文档](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Resilience4j 官方文档](https://resilience4j.readme.io/)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
