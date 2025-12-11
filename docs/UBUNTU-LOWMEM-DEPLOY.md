# Ubuntu 低内存环境部署指南

## 系统要求

- **内存**: 3.8GB（最低）/ 6GB（推荐）
- **交换空间**: 4GB
- **磁盘空间**: 至少 10GB 可用空间
- **Docker**: 已安装
- **Java**: OpenJDK 21
- **Maven**: 3.6+

## ⚠️ 重要提示

您的虚拟机内存只有 **3.8GB**，可用内存仅 **783MB**，这会导致系统卡死。

### 推荐解决方案（按优先级）

#### 方案 1: 增加虚拟机内存（强烈推荐）

1. 关闭 Ubuntu 虚拟机
2. 在 VMware 中：右键虚拟机 → 设置 → 内存
3. 将内存调整到 **6GB 或 8GB**
4. 保存并重新启动

#### 方案 2: 使用最小化配置（临时测试）

如果无法增加内存，使用优化后的配置文件。

## 快速部署（一键脚本）

```bash
# 1. 将项目传输到 Ubuntu
cd ~/桌面
# 假设项目已在 ~/桌面/course-cloud-hw07

# 2. 给脚本执行权限
cd course-cloud-hw07
chmod +x scripts/deploy-ubuntu-lowmem.sh

# 3. 运行部署脚本
./scripts/deploy-ubuntu-lowmem.sh
```

脚本会自动：
- ✅ 清理 Docker 环境
- ✅ 本地构建 JAR 包（避免 Docker 中构建）
- ✅ 分步启动服务（避免内存峰值）
- ✅ 检查服务状态
- ✅ 显示内存使用情况

## 手动部署（分步执行）

### 步骤 1: 安装依赖

```bash
# 更新系统
sudo apt update

# 安装 Java 21
sudo apt install openjdk-21-jdk -y

# 安装 Maven
sudo apt install maven -y

# 验证安装
java -version
mvn -version
```

### 步骤 2: 清理环境

```bash
cd ~/桌面/course-cloud-hw07

# 停止所有容器
docker-compose -f docker-compose-minimal.yml down -v

# 清理 Docker
docker system prune -f
```

### 步骤 3: 本地构建 JAR 包

**关键：在本地构建，不在 Docker 中构建**

```bash
# 构建 User Service
cd user-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false
cd ..

# 构建 Catalog Service
cd catalog-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false
cd ..

# 构建 Enrollment Service
cd enrollment-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false
cd ..

# 验证 JAR 文件
ls -lh user-service/target/*.jar
ls -lh catalog-service/target/*.jar
ls -lh enrollment-service/target/*.jar
```

### 步骤 4: 分步启动服务

**不要一次性启动所有服务！**

```bash
# 1. 先启动 Nacos 和数据库
docker-compose -f docker-compose-minimal.yml up -d nacos user-db catalog-db enrollment-db

# 等待 1 分钟
sleep 60

# 检查 Nacos
curl http://localhost:8848/nacos/
docker logs nacos | tail -20

# 2. 启动 User Service
docker-compose -f docker-compose-minimal.yml up -d user-service-1
sleep 30

# 3. 启动 Catalog Service
docker-compose -f docker-compose-minimal.yml up -d catalog-service-1
sleep 30

# 4. 启动 Enrollment Service
docker-compose -f docker-compose-minimal.yml up -d enrollment-service
sleep 30
```

### 步骤 5: 验证部署

```bash
# 查看所有服务状态
docker-compose -f docker-compose-minimal.yml ps

# 查看内存使用
free -h
docker stats --no-stream

# 查看服务日志
docker logs -f enrollment-service
```

## 访问服务

- **Nacos 控制台**: http://localhost:8848/nacos
  - 用户名: `nacos`
  - 密码: `nacos`

- **User Service**: http://localhost:18081/api/students/test
- **Catalog Service**: http://localhost:18091/api/courses/test
- **Enrollment Service**: http://localhost:8083/api/enrollments/test

## 运行测试

### 功能测试

```bash
# 1. 创建学生
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

# 2. 创建课程
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

# 3. 选课
curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId": "CS101", "studentId": "202101001"}'
```

## 内存优化说明

### 最小化配置的优化

与原配置相比：

| 组件 | 原配置 | 最小化配置 | 节省 |
|------|--------|-----------|------|
| Nacos | 256-512MB | 128-256MB | ~200MB |
| MySQL (每个) | ~256MB | ~128MB | ~400MB (3个) |
| Java服务 (每个) | ~400MB | ~256MB | ~600MB (减少实例) |
| **总计** | ~4-5GB | **~1.8GB** | **节省 2-3GB** |

### JVM 参数说明

```bash
-Xms128m          # 初始堆内存 128MB
-Xmx256m          # 最大堆内存 256MB
-XX:MaxMetaspaceSize=128m  # 最大元空间 128MB
```

## 故障排查

### 服务启动失败

```bash
# 查看具体错误
docker logs <container-name>

# 常见问题：
# 1. 端口被占用
sudo netstat -tunlp | grep 8083
sudo kill -9 <PID>

# 2. 内存不足
free -h
# 如果可用内存 < 500MB，重启虚拟机

# 3. Nacos 未启动
docker restart nacos
docker logs -f nacos
```

### 系统卡死恢复

```bash
# 1. 强制重启 Ubuntu 虚拟机

# 2. 清理所有容器
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker system prune -af

# 3. 重新部署（使用最小化配置）
./scripts/deploy-ubuntu-lowmem.sh
```

### Maven 构建失败

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository

# 重新构建
mvn clean package -DskipTests -U
```

## 停止服务

```bash
# 停止所有服务
docker-compose -f docker-compose-minimal.yml down

# 停止并删除数据
docker-compose -f docker-compose-minimal.yml down -v
```

## 完整测试后

如果希望测试完整的负载均衡功能（3 个实例），必须：

1. **增加虚拟机内存到 6-8GB**
2. 使用原配置文件：`docker-compose.yml`
3. 运行完整测试脚本：
   ```bash
   ./scripts/test-load-balance.sh
   ./scripts/test-circuit-breaker.sh
   ```

## 注意事项

⚠️ **最小化配置的限制**：
- ❌ 只有 1 个服务实例，无法测试负载均衡
- ❌ 并发能力有限
- ✅ 可以测试 OpenFeign 基本功能
- ✅ 可以测试熔断降级功能
- ✅ 适合功能验证和调试

📝 **建议**：
- 使用最小化配置验证功能
- 截图保存测试结果
- 最终在高配置环境测试完整功能
- 或使用云服务器（腾讯云/阿里云学生机）
