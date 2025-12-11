#!/bin/bash

# Ubuntu 低内存环境部署脚本
# 适用于 3.8GB 内存的虚拟机

set -e  # 遇到错误立即退出

echo "======================================"
echo "Course Cloud 低内存环境部署脚本"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 检查内存
total_mem=$(free -m | awk '/^Mem:/{print $2}')
echo -e "${YELLOW}检测到系统内存: ${total_mem}MB${NC}"
if [ "$total_mem" -lt 3500 ]; then
    echo -e "${RED}警告: 内存不足！建议至少 4GB 内存${NC}"
    read -p "是否继续? (y/n): " continue
    if [ "$continue" != "y" ]; then
        exit 1
    fi
fi
echo ""

# 步骤1: 清理环境
echo -e "${YELLOW}步骤 1: 清理Docker环境${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml down -v 2>/dev/null || true
docker system prune -f
echo -e "${GREEN}✓ 环境清理完成${NC}"
echo ""

# 步骤2: 构建JAR包
echo -e "${YELLOW}步骤 2: 构建JAR包（本地构建，不在Docker中）${NC}"
echo "--------------------------------------"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven 未安装！${NC}"
    echo "请执行: sudo apt install maven -y"
    exit 1
fi

echo "构建 User Service..."
cd user-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false -q
if [ ! -f "target/user-service.jar" ]; then
    echo -e "${RED}✗ User Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ User Service 构建完成${NC}"
cd ..

echo "构建 Catalog Service..."
cd catalog-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false -q
if [ ! -f "target/catalog-service.jar" ]; then
    echo -e "${RED}✗ Catalog Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Catalog Service 构建完成${NC}"
cd ..

echo "构建 Enrollment Service..."
cd enrollment-service
mvn clean package -DskipTests -Dmaven.compiler.fork=false -q
if [ ! -f "target/enrollment-service.jar" ]; then
    echo -e "${RED}✗ Enrollment Service 构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Enrollment Service 构建完成${NC}"
cd ..

echo ""

# 步骤3: 分步启动服务
echo -e "${YELLOW}步骤 3: 启动基础设施（Nacos + 数据库）${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml up -d nacos user-db catalog-db enrollment-db

echo "等待 Nacos 启动（60秒）..."
for i in {60..1}; do
    echo -ne "剩余时间: $i 秒\r"
    sleep 1
done
echo ""

# 检查Nacos是否启动成功
echo "检查 Nacos 状态..."
if curl -s http://localhost:8848/nacos/ > /dev/null; then
    echo -e "${GREEN}✓ Nacos 启动成功${NC}"
else
    echo -e "${RED}✗ Nacos 启动失败${NC}"
    docker logs nacos | tail -20
    exit 1
fi
echo ""

# 步骤4: 启动微服务
echo -e "${YELLOW}步骤 4: 启动 User Service${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml up -d user-service-1
echo "等待 User Service 启动（30秒）..."
sleep 30
echo -e "${GREEN}✓ User Service 已启动${NC}"
echo ""

echo -e "${YELLOW}步骤 5: 启动 Catalog Service${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml up -d catalog-service-1
echo "等待 Catalog Service 启动（30秒）..."
sleep 30
echo -e "${GREEN}✓ Catalog Service 已启动${NC}"
echo ""

echo -e "${YELLOW}步骤 6: 启动 Enrollment Service${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml up -d enrollment-service
echo "等待 Enrollment Service 启动（30秒）..."
sleep 30
echo -e "${GREEN}✓ Enrollment Service 已启动${NC}"
echo ""

# 步骤5: 检查服务状态
echo -e "${YELLOW}步骤 7: 检查所有服务状态${NC}"
echo "--------------------------------------"
docker-compose -f docker-compose-minimal.yml ps
echo ""

# 显示内存使用
echo -e "${YELLOW}当前内存使用情况:${NC}"
free -h
echo ""

echo -e "${YELLOW}Docker 容器资源使用:${NC}"
docker stats --no-stream
echo ""

# 完成
echo "======================================"
echo -e "${GREEN}部署完成！${NC}"
echo "======================================"
echo ""
echo "访问地址："
echo "  - Nacos 控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)"
echo "  - User Service: http://localhost:18081"
echo "  - Catalog Service: http://localhost:18091"
echo "  - Enrollment Service: http://localhost:8083"
echo ""
echo "查看日志："
echo "  docker logs -f nacos"
echo "  docker logs -f user-service-1"
echo "  docker logs -f catalog-service-1"
echo "  docker logs -f enrollment-service"
echo ""
echo "停止所有服务："
echo "  docker-compose -f docker-compose-minimal.yml down"
echo ""
