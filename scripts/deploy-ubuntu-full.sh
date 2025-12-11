#!/bin/bash

# Ubuntu 完整部署脚本（8GB内存环境）
# 自动完成从构建到测试的全过程

set -e

echo "======================================"
echo "Course Cloud 完整部署脚本"
echo "适用于 8GB 内存环境"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# 检查内存
total_mem=$(free -m | awk '/^Mem:/{print $2}')
echo -e "${BLUE}检测到系统内存: ${total_mem}MB${NC}"
if [ "$total_mem" -lt 6000 ]; then
    echo -e "${YELLOW}警告: 内存小于 6GB，建议增加到 8GB${NC}"
fi
echo ""

# 步骤1: 检查依赖
echo -e "${YELLOW}步骤 1: 检查依赖${NC}"
echo "--------------------------------------"

if ! command -v java &> /dev/null; then
    echo -e "${RED}Java 未安装！${NC}"
    echo "请执行: sudo apt install openjdk-21-jdk -y"
    exit 1
fi
echo -e "${GREEN}✓ Java 已安装: $(java -version 2>&1 | head -1)${NC}"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven 未安装！${NC}"
    echo "请执行: sudo apt install maven -y"
    exit 1
fi
echo -e "${GREEN}✓ Maven 已安装: $(mvn -version | head -1)${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker 未安装！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker 已安装: $(docker --version)${NC}"
echo ""

# 步骤2: 清理环境
echo -e "${YELLOW}步骤 2: 清理Docker环境${NC}"
echo "--------------------------------------"
docker-compose down -v 2>/dev/null || true
docker system prune -f
echo -e "${GREEN}✓ 环境清理完成${NC}"
echo ""

# 步骤3: 构建JAR包
echo -e "${YELLOW}步骤 3: 构建JAR包${NC}"
echo "--------------------------------------"

echo "构建 User Service..."
cd user-service
mvn clean package -DskipTests -q
if [ ! -f "target/user-service.jar" ]; then
    echo -e "${RED}✗ User Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ User Service 构建完成${NC}"
cd ..

echo "构建 Catalog Service..."
cd catalog-service
mvn clean package -DskipTests -q
if [ ! -f "target/catalog-service.jar" ]; then
    echo -e "${RED}✗ Catalog Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Catalog Service 构建完成${NC}"
cd ..

echo "构建 Enrollment Service..."
cd enrollment-service
mvn clean package -DskipTests -q
if [ ! -f "target/enrollment-service.jar" ]; then
    echo -e "${RED}✗ Enrollment Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Enrollment Service 构建完成${NC}"
cd ..
echo ""

# 步骤4: 启动所有服务
echo -e "${YELLOW}步骤 4: 启动所有服务（完整配置）${NC}"
echo "--------------------------------------"
echo "启动 9 个容器（Nacos + 3个数据库 + 3个User + 3个Catalog + 1个Enrollment）"
docker-compose up -d

echo ""
echo "等待所有服务启动（120秒）..."
for i in {120..1}; do
    printf "\r剩余时间: %3d 秒" $i
    sleep 1
done
echo ""
echo ""

# 步骤5: 验证服务
echo -e "${YELLOW}步骤 5: 验证服务状态${NC}"
echo "--------------------------------------"

# 检查Nacos
if curl -s http://localhost:8848/nacos/ > /dev/null; then
    echo -e "${GREEN}✓ Nacos 运行正常${NC}"
else
    echo -e "${RED}✗ Nacos 未启动${NC}"
    exit 1
fi

# 检查容器状态
echo ""
echo "所有容器状态："
docker-compose ps
echo ""

# 显示内存使用
echo -e "${YELLOW}当前系统资源使用:${NC}"
free -h
echo ""

echo -e "${YELLOW}Docker 容器资源使用:${NC}"
docker stats --no-stream
echo ""

# 步骤6: 创建测试数据
echo -e "${YELLOW}步骤 6: 创建测试数据${NC}"
echo "--------------------------------------"

echo "创建测试学生..."
curl -s -X POST http://localhost:18081/api/students \
    -H "Content-Type: application/json" \
    -d '{
        "username": "test_student",
        "email": "test@zjgsu.edu.cn",
        "studentId": "202101001",
        "name": "测试学生",
        "major": "计算机科学",
        "grade": 2021
    }' > /dev/null && echo -e "${GREEN}✓ 学生创建成功${NC}" || echo -e "${RED}✗ 学生创建失败${NC}"

echo "创建测试课程..."
curl -s -X POST http://localhost:18091/api/courses \
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
    }' > /dev/null && echo -e "${GREEN}✓ 课程创建成功${NC}" || echo -e "${RED}✗ 课程创建失败${NC}"
echo ""

# 完成
echo "======================================"
echo -e "${GREEN}部署完成！${NC}"
echo "======================================"
echo ""
echo "🌐 访问地址："
echo "  - Nacos 控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)"
echo "  - User Service: http://localhost:18081"
echo "  - Catalog Service: http://localhost:18091"
echo "  - Enrollment Service: http://localhost:8083"
echo ""
echo "📊 Nacos 服务列表应显示："
echo "  - user-service: 3 个实例"
echo "  - catalog-service: 3 个实例"
echo "  - enrollment-service: 1 个实例"
echo ""
echo "🧪 运行测试："
echo "  ./scripts/test-load-balance.sh       # 负载均衡测试"
echo "  ./scripts/test-circuit-breaker.sh    # 熔断降级测试"
echo ""
echo "📝 查看日志："
echo "  docker logs -f user-service-1"
echo "  docker logs -f catalog-service-1"
echo "  docker logs -f enrollment-service"
echo ""
echo "🛑 停止所有服务："
echo "  docker-compose down"
echo ""
