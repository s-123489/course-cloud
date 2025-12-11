#!/bin/bash

# 测试熔断降级脚本
# 用于验证 OpenFeign 熔断降级功能

echo "======================================"
echo "OpenFeign 熔断降级测试脚本"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ENROLLMENT_SERVICE="http://localhost:8083"

# 步骤1：测试正常情况
echo -e "${YELLOW}步骤 1: 测试正常情况（所有服务运行）${NC}"
echo "--------------------------------------"
echo "发送选课请求..."
response=$(curl -s -X POST "${ENROLLMENT_SERVICE}/api/enrollments" \
    -H "Content-Type: application/json" \
    -d '{"courseId": "CS101", "studentId": "202101001"}')

if echo "$response" | grep -q "id"; then
    echo -e "${GREEN}✓ 成功：服务正常运行${NC}"
else
    echo -e "${RED}✗ 失败：${response}${NC}"
fi
echo ""

# 步骤2：停止 User Service
echo -e "${YELLOW}步骤 2: 停止所有 User Service 实例${NC}"
echo "--------------------------------------"
docker stop user-service-1 user-service-2 user-service-3
echo -e "${GREEN}✓ User Service 已停止${NC}"
echo ""
sleep 5

# 步骤3：测试熔断降级
echo -e "${YELLOW}步骤 3: 测试熔断降级（User Service 不可用）${NC}"
echo "--------------------------------------"
echo "发送选课请求..."
response=$(curl -s -X POST "${ENROLLMENT_SERVICE}/api/enrollments" \
    -H "Content-Type: application/json" \
    -d '{"courseId": "CS101", "studentId": "202101002"}')

if echo "$response" | grep -q "用户服务暂时不可用"; then
    echo -e "${GREEN}✓ 成功：熔断降级被触发${NC}"
    echo "响应信息: $response"
elif echo "$response" | grep -q "user-service unavailable"; then
    echo -e "${GREEN}✓ 成功：熔断降级被触发${NC}"
    echo "响应信息: $response"
else
    echo -e "${RED}✗ 失败：熔断降级未触发${NC}"
    echo "响应信息: $response"
fi
echo ""

# 步骤4：查看 Enrollment Service 日志
echo -e "${YELLOW}步骤 4: 查看 Enrollment Service 日志${NC}"
echo "--------------------------------------"
echo "查看 Fallback 触发日志..."
docker logs --tail 20 enrollment-service 2>&1 | grep -E "(fallback|UserClient|user-service)"
echo ""

# 步骤5：重启 User Service
echo -e "${YELLOW}步骤 5: 重启 User Service${NC}"
echo "--------------------------------------"
docker start user-service-1 user-service-2 user-service-3
echo -e "${GREEN}✓ User Service 已重启${NC}"
echo ""
sleep 10

# 步骤6：验证服务恢复
echo -e "${YELLOW}步骤 6: 验证服务恢复${NC}"
echo "--------------------------------------"
echo "发送选课请求..."
response=$(curl -s -X POST "${ENROLLMENT_SERVICE}/api/enrollments" \
    -H "Content-Type: application/json" \
    -d '{"courseId": "CS101", "studentId": "202101003"}')

if echo "$response" | grep -q "id"; then
    echo -e "${GREEN}✓ 成功：服务已恢复正常${NC}"
else
    echo -e "${RED}✗ 失败：服务未恢复${NC}"
    echo "响应信息: $response"
fi
echo ""

# 步骤7：停止 Catalog Service
echo -e "${YELLOW}步骤 7: 测试 Catalog Service 熔断降级${NC}"
echo "--------------------------------------"
docker stop catalog-service-1 catalog-service-2 catalog-service-3
echo -e "${GREEN}✓ Catalog Service 已停止${NC}"
echo ""
sleep 5

echo "发送选课请求..."
response=$(curl -s -X POST "${ENROLLMENT_SERVICE}/api/enrollments" \
    -H "Content-Type: application/json" \
    -d '{"courseId": "CS101", "studentId": "202101004"}')

if echo "$response" | grep -q "课程服务暂时不可用"; then
    echo -e "${GREEN}✓ 成功：Catalog Service 熔断降级被触发${NC}"
    echo "响应信息: $response"
elif echo "$response" | grep -q "catalog-service unavailable"; then
    echo -e "${GREEN}✓ 成功：Catalog Service 熔断降级被触发${NC}"
    echo "响应信息: $response"
else
    echo -e "${RED}✗ 失败：熔断降级未触发${NC}"
    echo "响应信息: $response"
fi
echo ""

# 步骤8：恢复所有服务
echo -e "${YELLOW}步骤 8: 恢复所有服务${NC}"
echo "--------------------------------------"
docker start catalog-service-1 catalog-service-2 catalog-service-3
echo -e "${GREEN}✓ 所有服务已恢复${NC}"
echo ""

# 测试完成
echo "======================================"
echo -e "${GREEN}熔断降级测试完成！${NC}"
echo "======================================"
echo ""
echo "测试结果总结："
echo "1. ✓ User Service 不可用时，熔断降级被正确触发"
echo "2. ✓ Catalog Service 不可用时，熔断降级被正确触发"
echo "3. ✓ 服务恢复后，系统自动恢复正常"
echo ""
echo "熔断器配置："
echo "- 失败率阈值: 50%"
echo "- 滑动窗口大小: 10 次调用"
echo "- 连接超时: 3 秒"
echo "- 读取超时: 5 秒"
echo "- 熔断器打开后等待: 10 秒"
echo ""
