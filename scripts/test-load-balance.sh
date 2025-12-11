#!/bin/bash

# 测试负载均衡脚本
# 用于验证 OpenFeign 负载均衡和服务间通信功能

echo "======================================"
echo "OpenFeign 负载均衡测试脚本"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 服务端点
ENROLLMENT_SERVICE="http://localhost:8083"
USER_SERVICE_1="http://localhost:18081"
USER_SERVICE_2="http://localhost:18082"
USER_SERVICE_3="http://localhost:18083"
CATALOG_SERVICE_1="http://localhost:18091"
CATALOG_SERVICE_2="http://localhost:18092"
CATALOG_SERVICE_3="http://localhost:18093"

# 检查服务健康状态
check_service() {
    local service_name=$1
    local url=$2
    echo -n "检查 ${service_name}... "
    if curl -s "${url}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 正常${NC}"
        return 0
    else
        echo -e "${RED}✗ 不可用${NC}"
        return 1
    fi
}

# 步骤1：检查所有服务健康状态
echo -e "${YELLOW}步骤 1: 检查服务健康状态${NC}"
echo "--------------------------------------"
check_service "User Service 1" "${USER_SERVICE_1}"
check_service "User Service 2" "${USER_SERVICE_2}"
check_service "User Service 3" "${USER_SERVICE_3}"
check_service "Catalog Service 1" "${CATALOG_SERVICE_1}"
check_service "Catalog Service 2" "${CATALOG_SERVICE_2}"
check_service "Catalog Service 3" "${CATALOG_SERVICE_3}"
check_service "Enrollment Service" "${ENROLLMENT_SERVICE}"
echo ""

# 步骤2：创建测试数据
echo -e "${YELLOW}步骤 2: 创建测试数据${NC}"
echo "--------------------------------------"

# 创建学生
echo "创建测试学生..."
for i in {1..3}; do
    curl -s -X POST "${USER_SERVICE_1}/api/students" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"student_$i\",
            \"email\": \"student$i@zjgsu.edu.cn\",
            \"studentId\": \"20210100$i\",
            \"name\": \"测试学生$i\",
            \"major\": \"计算机科学\",
            \"grade\": 2021
        }" > /dev/null
    echo -e "  学生 20210100$i ${GREEN}创建成功${NC}"
done
echo ""

# 创建课程
echo "创建测试课程..."
curl -s -X POST "${CATALOG_SERVICE_1}/api/courses" \
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
    }' > /dev/null
echo -e "  课程 CS101 ${GREEN}创建成功${NC}"
echo ""

# 步骤3：测试负载均衡
echo -e "${YELLOW}步骤 3: 测试负载均衡（发送 10 次请求）${NC}"
echo "--------------------------------------"
echo "通过 Enrollment Service 发送选课请求，观察请求分配到不同实例..."
echo ""

for i in {1..10}; do
    echo -n "请求 #$i: "
    response=$(curl -s -X POST "${ENROLLMENT_SERVICE}/api/enrollments" \
        -H "Content-Type: application/json" \
        -d "{\"courseId\": \"CS101\", \"studentId\": \"test_student_$i\"}")

    if echo "$response" | grep -q "id"; then
        echo -e "${GREEN}✓ 成功${NC}"
    else
        echo -e "${RED}✗ 失败${NC}"
    fi
    sleep 0.5
done
echo ""

# 步骤4：测试服务调用
echo -e "${YELLOW}步骤 4: 测试 Enrollment Service 调用其他服务${NC}"
echo "--------------------------------------"
echo "查看 Enrollment Service 的测试端点..."
curl -s "${ENROLLMENT_SERVICE}/api/enrollments/test" | jq '.'
echo ""

# 步骤5：查看 Docker 容器日志
echo -e "${YELLOW}步骤 5: 查看服务实例日志（最近 5 条）${NC}"
echo "--------------------------------------"
echo ""
echo -e "${GREEN}User Service 实例日志:${NC}"
echo "--- user-service-1 ---"
docker logs --tail 5 user-service-1 2>&1 | grep "User Service \[port"
echo ""
echo "--- user-service-2 ---"
docker logs --tail 5 user-service-2 2>&1 | grep "User Service \[port"
echo ""
echo "--- user-service-3 ---"
docker logs --tail 5 user-service-3 2>&1 | grep "User Service \[port"
echo ""

echo -e "${GREEN}Catalog Service 实例日志:${NC}"
echo "--- catalog-service-1 ---"
docker logs --tail 5 catalog-service-1 2>&1 | grep "Catalog Service \[port"
echo ""
echo "--- catalog-service-2 ---"
docker logs --tail 5 catalog-service-2 2>&1 | grep "Catalog Service \[port"
echo ""
echo "--- catalog-service-3 ---"
docker logs --tail 5 catalog-service-3 2>&1 | grep "Catalog Service \[port"
echo ""

echo -e "${GREEN}Enrollment Service 日志:${NC}"
docker logs --tail 10 enrollment-service 2>&1 | grep -E "(开始选课|验证成功)"
echo ""

# 测试完成
echo "======================================"
echo -e "${GREEN}测试完成！${NC}"
echo "======================================"
echo ""
echo "日志分析提示："
echo "1. 检查 User Service 和 Catalog Service 的日志"
echo "2. 确认不同的 hostname 出现在日志中（user-service-1, user-service-2, user-service-3）"
echo "3. 这证明负载均衡正在工作，请求被分配到不同实例"
echo ""
echo "如需查看完整日志，请使用："
echo "  docker logs -f user-service-1"
echo "  docker logs -f catalog-service-1"
echo "  docker logs -f enrollment-service"
echo ""
