# 校园选课系统 - 微服务版

**项目名称**: course-cloud
**版本**: v1.0.0
**基于**: course v1.1.0 (单体应用)

## 项目简介

本项目是将单体选课系统拆分为微服务架构的实践项目。通过服务拆分、独立数据库、HTTP通信等技术，实现了课程管理、学生管理和选课管理的解耦。

## 架构图

```
客户端
  ↓
  ├─→ user-service (8081) → user_db (3306)
  │   └── 学生/用户管理
  │
  ├─→ catalog-service (8082) → catalog_db (3307)
  │   └── 课程管理
  │
  └─→ enrollment-service (8083) → enrollment_db (3308)
      ├── 选课管理
      ├── HTTP调用 → user-service（验证学生）
      └── HTTP调用 → catalog-service（验证课程）
```

## 技术栈

- **Spring Boot**: 3.5.7
- **Java**: 25
- **MySQL**: 8.4
- **Docker & Docker Compose**: 容器化部署
- **RestTemplate**: 服务间通信

## 服务说明

### user-service (用户服务)

- **端口**: 8081
- **数据库**: user_db (3306)
- **功能**: 学生/用户管理
- **API端点**:
  - `GET /api/students` - 获取所有学生
  - `GET /api/students/{id}` - 获取单个学生
  - `GET /api/students/studentId/{studentId}` - 按学号查询
  - `POST /api/students` - 创建学生
  - `PUT /api/students/{id}` - 更新学生
  - `DELETE /api/students/{id}` - 删除学生

### catalog-service (课程目录服务)

- **端口**: 8082
- **数据库**: catalog_db (3307)
- **功能**: 课程管理
- **API端点**:
  - `GET /api/courses` - 获取所有课程
  - `GET /api/courses/{id}` - 获取单个课程
  - `GET /api/courses/code/{code}` - 按课程代码查询
  - `POST /api/courses` - 创建课程
  - `PUT /api/courses/{id}` - 更新课程
  - `DELETE /api/courses/{id}` - 删除课程

### enrollment-service (选课服务)

- **端口**: 8083
- **数据库**: enrollment_db (3308)
- **功能**: 选课管理，通过RestTemplate调用user-service和catalog-service
- **API端点**:
  - `GET /api/enrollments` - 获取所有选课记录
  - `GET /api/enrollments/course/{courseId}` - 按课程查询选课
  - `GET /api/enrollments/student/{studentId}` - 按学生查询选课
  - `POST /api/enrollments` - 学生选课
  - `DELETE /api/enrollments/{id}` - 学生退课

## 环境要求

- JDK 25+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+

## 构建和运行步骤

### 快速启动（推荐）

使用 `run.sh` 脚本一键构建并启动所有服务：

```bash
# 赋予执行权限（首次运行需要）
chmod +x run.sh

# 构建并启动所有服务
./run.sh
```

脚本会自动完成以下操作：
1. 编译所有服务的 JAR 文件
2. 构建 Docker 镜像并启动容器
3. 等待服务启动完成
4. 显示服务状态和访问地址

### 手动构建

#### 1. 构建所有服务

```bash
# 构建 user-service
cd user-service
mvn clean package -DskipTests
cd ..

# 构建 catalog-service
cd catalog-service
mvn clean package -DskipTests
cd ..

# 构建 enrollment-service
cd enrollment-service
mvn clean package -DskipTests
cd ..
```

#### 2. 使用 Docker Compose 部署

```bash
# 启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

#### 3. 验证服务

```bash
# 检查 user-service
curl http://localhost:8081/api/students

# 检查 catalog-service
curl http://localhost:8082/api/courses

# 检查 enrollment-service
curl http://localhost:8083/api/enrollments
```

## 测试说明

运行测试脚本：

```bash
chmod +x test-services.sh
./test-services.sh
```

测试脚本会执行以下操作：

1. 创建学生（user-service）
2. 获取所有学生
3. 创建课程（catalog-service）
4. 获取所有课程
5. 学生选课（验证服务间通信）
6. 查询选课记录
7. 测试学生不存在的错误处理
8. 测试课程不存在的错误处理

## 服务间通信示例

enrollment-service 通过 RestTemplate 调用其他服务：

```java
// 验证学生是否存在
String userUrl = userServiceUrl + "/api/students/studentId/" + studentId;
Map<String, Object> studentResponse = restTemplate.getForObject(userUrl, Map.class);

// 验证课程是否存在
String courseUrl = catalogServiceUrl + "/api/courses/" + courseId;
Map<String, Object> courseResponse = restTemplate.getForObject(courseUrl, Map.class);
```

## 数据库配置

| 服务 | 数据库 | 端口 | 用户名 | 密码 |
|------|--------|------|--------|------|
| user-service | user_db | 3306 | user_user | user_pass |
| catalog-service | catalog_db | 3307 | catalog_user | catalog_pass |
| enrollment-service | enrollment_db | 3308 | enrollment_user | enrollment_pass |

## 常见问题

### Q: 服务启动失败？

A: 检查端口是否被占用，确保 8081/8082/8083 和 3306/3307/3308 端口可用。

### Q: 服务间调用失败？

A: 确保所有服务都已启动，检查 docker logs 查看具体错误。

### Q: 数据库连接失败？

A: 等待数据库健康检查完成，通常需要 10-15 秒。

## 项目结构

```
course-cloud/
├── README.md
├── docker-compose.yml
├── run.sh                # 一键启动脚本
├── test-services.sh
├── user-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── catalog-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
└── enrollment-service/
    ├── src/
    ├── Dockerfile
    └── pom.xml
```

## 作业完成情况

- ✅ 服务拆分：三个独立微服务
- ✅ 独立数据库：每个服务有独立的数据库
- ✅ 服务间通信：使用 RestTemplate 实现 HTTP 调用
- ✅ Docker 容器化：每个服务都有 Dockerfile
- ✅ Docker Compose：一键部署所有服务
- ✅ 测试脚本：完整的功能测试

## 许可证

MIT License
