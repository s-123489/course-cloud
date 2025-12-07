#!/bin/bash
# run.sh - 构建并启动 course-cloud 微服务

set -e

echo "========================================"
echo "Course Cloud 微服务启动脚本"
echo "========================================"

# 编译所有服务
echo ""
echo ">>> 步骤 1/3: 编译服务..."
for service in user-service catalog-service enrollment-service; do
    echo "  - 编译 $service..."
    cd $service && mvn clean package -DskipTests > /dev/null 2>&1 && cd ..
done
echo "✓ 所有服务编译完成"

# 构建并启动 Docker 容器
echo ""
echo ">>> 步骤 2/3: 构建并启动 Docker 容器..."
docker-compose up -d --build

# 等待服务启动
echo ""
echo ">>> 步骤 3/3: 等待服务启动..."
echo "  等待数据库健康检查（约15秒）..."
sleep 15

# 显示服务状态
echo ""
echo "========================================"
echo "✓ 所有服务已启动"
echo "========================================"
docker-compose ps

# 显示访问信息
echo ""
echo "服务访问地址："
echo "  - User Service:       http://localhost:8081/api/users/students"
echo "  - Catalog Service:    http://localhost:8082/api/courses"
echo "  - Enrollment Service: http://localhost:8083/api/enrollments"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: docker-compose down"
echo "删除数据: docker-compose down -v"
