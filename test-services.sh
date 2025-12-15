#!/bin/bash

# =================================================================
# CourseCloud 微服务测试脚本
# 集成了所有服务测试功能
# =================================================================

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务端点配置
GATEWAY_URL="http://localhost:8090"
ENROLLMENT_SERVICE="http://localhost:8083"
USER_SERVICE_1="http://localhost:18081"
USER_SERVICE_2="http://localhost:18082"
USER_SERVICE_3="http://localhost:18083"
CATALOG_SERVICE_1="http://localhost:18091"
CATALOG_SERVICE_2="http://localhost:18092"
CATALOG_SERVICE_3="http://localhost:18093"

# =================================================================
# 辅助函数
# =================================================================

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

# =================================================================
# 测试 1: Gateway 和 JWT 认证测试
# =================================================================
test_gateway_jwt() {
    echo ""
    echo "=========================================="
    echo "   hw09 API 网关与 JWT 认证测试"
    echo "=========================================="
    echo ""

    # 测试 1.1: 登录获取 Token
    echo -e "${YELLOW}[测试 1.1] 登录获取 JWT Token${NC}"
    echo "请求: POST $GATEWAY_URL/api/auth/login"
    echo ""

    LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "admin",
        "password": "admin123"
      }')

    echo "响应:"
    echo "$LOGIN_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGIN_RESPONSE"
    echo ""

    # 提取 Token
    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token' 2>/dev/null)

    if [ "$TOKEN" != "null" ] && [ ! -z "$TOKEN" ]; then
        echo -e "${GREEN}✓ 登录成功，获取到 Token${NC}"
        echo "Token (前 50 个字符): ${TOKEN:0:50}..."
    else
        echo -e "${RED}✗ 登录失败，未获取到 Token${NC}"
        echo "可能原因："
        echo "1. 用户数据未初始化"
        echo "2. Gateway 服务未启动"
        echo "3. User Service 未启动"
        return 1
    fi
    echo ""

    # 测试 1.2: 未认证访问
    echo -e "${YELLOW}[测试 1.2] 未认证访问 API（预期返回 401）${NC}"
    echo "请求: GET $GATEWAY_URL/api/users/students"
    echo ""

    UNAUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$GATEWAY_URL/api/users/students")
    HTTP_CODE=$(echo "$UNAUTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

    if [ "$HTTP_CODE" == "401" ]; then
        echo -e "${GREEN}✓ 正确返回 401 Unauthorized${NC}"
    else
        echo -e "${RED}✗ 返回状态码: $HTTP_CODE (预期 401)${NC}"
    fi
    echo ""

    # 测试 1.3: 携带 Token 访问
    echo -e "${YELLOW}[测试 1.3] 携带 Token 访问 API（预期返回 200）${NC}"
    echo "请求: GET $GATEWAY_URL/api/users/students"
    echo ""

    AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$GATEWAY_URL/api/users/students" \
      -H "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$AUTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ 认证成功，返回 200 OK${NC}"
        echo "响应数据:"
        echo "$AUTH_RESPONSE" | grep -v "HTTP_CODE" | jq '.' 2>/dev/null || echo "$AUTH_RESPONSE" | grep -v "HTTP_CODE"
    else
        echo -e "${RED}✗ 返回状态码: $HTTP_CODE (预期 200)${NC}"
    fi
    echo ""

    # 测试 1.4: Catalog Service 路由
    echo -e "${YELLOW}[测试 1.4] 测试 Catalog Service 路由${NC}"
    echo "请求: GET $GATEWAY_URL/api/courses"
    echo ""

    CATALOG_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$GATEWAY_URL/api/courses" \
      -H "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$CATALOG_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ Catalog Service 路由正常${NC}"
        COURSE_COUNT=$(echo "$CATALOG_RESPONSE" | grep -v "HTTP_CODE" | jq 'length' 2>/dev/null || echo "0")
        echo "课程数量: $COURSE_COUNT"
    else
        echo -e "${RED}✗ Catalog Service 路由失败，状态码: $HTTP_CODE${NC}"
    fi
    echo ""

    # 测试 1.5: Enrollment Service 路由
    echo -e "${YELLOW}[测试 1.5] 测试 Enrollment Service 路由${NC}"
    echo "请求: GET $GATEWAY_URL/api/enrollments"
    echo ""

    ENROLLMENT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$GATEWAY_URL/api/enrollments" \
      -H "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$ENROLLMENT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}✓ Enrollment Service 路由正常${NC}"
        ENROLLMENT_COUNT=$(echo "$ENROLLMENT_RESPONSE" | grep -v "HTTP_CODE" | jq 'length' 2>/dev/null || echo "0")
        echo "选课记录数量: $ENROLLMENT_COUNT"
    else
        echo -e "${RED}✗ Enrollment Service 路由失败，状态码: $HTTP_CODE${NC}"
    fi
    echo ""

    # 测试 1.6: 无效 Token
    echo -e "${YELLOW}[测试 1.6] 测试无效 Token（预期返回 401）${NC}"
    echo "请求: GET $GATEWAY_URL/api/users/students"
    echo ""

    INVALID_TOKEN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$GATEWAY_URL/api/users/students" \
      -H "Authorization: Bearer invalid-token-12345")
    HTTP_CODE=$(echo "$INVALID_TOKEN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

    if [ "$HTTP_CODE" == "401" ]; then
        echo -e "${GREEN}✓ 正确拒绝无效 Token，返回 401${NC}"
    else
        echo -e "${RED}✗ 返回状态码: $HTTP_CODE (预期 401)${NC}"
    fi
    echo ""

    echo -e "${GREEN}Gateway 和 JWT 认证测试完成！${NC}"
}

# =================================================================
# 测试 2: 负载均衡测试
# =================================================================
test_load_balance() {
    echo ""
    echo "======================================"
    echo "   OpenFeign 负载均衡测试"
    echo "======================================"
    echo ""

    # 检查服务健康状态
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

    # 创建测试数据
    echo -e "${YELLOW}步骤 2: 创建测试数据${NC}"
    echo "--------------------------------------"

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

    # 测试负载均衡
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

    # 查看服务实例日志
    echo -e "${YELLOW}步骤 4: 查看服务实例日志（最近 5 条）${NC}"
    echo "--------------------------------------"
    echo ""
    echo -e "${GREEN}User Service 实例日志:${NC}"
    echo "--- user-service-1 ---"
    docker logs --tail 5 user-service-1 2>&1 | grep "User Service \[port" || echo "无相关日志"
    echo ""
    echo "--- user-service-2 ---"
    docker logs --tail 5 user-service-2 2>&1 | grep "User Service \[port" || echo "无相关日志"
    echo ""
    echo "--- user-service-3 ---"
    docker logs --tail 5 user-service-3 2>&1 | grep "User Service \[port" || echo "无相关日志"
    echo ""

    echo -e "${GREEN}Catalog Service 实例日志:${NC}"
    echo "--- catalog-service-1 ---"
    docker logs --tail 5 catalog-service-1 2>&1 | grep "Catalog Service \[port" || echo "无相关日志"
    echo ""
    echo "--- catalog-service-2 ---"
    docker logs --tail 5 catalog-service-2 2>&1 | grep "Catalog Service \[port" || echo "无相关日志"
    echo ""
    echo "--- catalog-service-3 ---"
    docker logs --tail 5 catalog-service-3 2>&1 | grep "Catalog Service \[port" || echo "无相关日志"
    echo ""

    echo -e "${GREEN}负载均衡测试完成！${NC}"
}

# =================================================================
# 测试 3: 熔断降级测试
# =================================================================
test_circuit_breaker() {
    echo ""
    echo "======================================"
    echo "   OpenFeign 熔断降级测试"
    echo "======================================"
    echo ""

    # 测试正常情况
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

    # 停止 User Service
    echo -e "${YELLOW}步骤 2: 停止所有 User Service 实例${NC}"
    echo "--------------------------------------"
    docker stop user-service-1 user-service-2 user-service-3
    echo -e "${GREEN}✓ User Service 已停止${NC}"
    echo ""
    sleep 5

    # 测试熔断降级
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

    # 查看日志
    echo -e "${YELLOW}步骤 4: 查看 Enrollment Service 日志${NC}"
    echo "--------------------------------------"
    echo "查看 Fallback 触发日志..."
    docker logs --tail 20 enrollment-service 2>&1 | grep -E "(fallback|UserClient|user-service)" || echo "无相关日志"
    echo ""

    # 重启 User Service
    echo -e "${YELLOW}步骤 5: 重启 User Service${NC}"
    echo "--------------------------------------"
    docker start user-service-1 user-service-2 user-service-3
    echo -e "${GREEN}✓ User Service 已重启${NC}"
    echo ""
    sleep 10

    # 验证服务恢复
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

    # 测试 Catalog Service 熔断降级
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

    # 恢复所有服务
    echo -e "${YELLOW}步骤 8: 恢复所有服务${NC}"
    echo "--------------------------------------"
    docker start catalog-service-1 catalog-service-2 catalog-service-3
    echo -e "${GREEN}✓ 所有服务已恢复${NC}"
    echo ""

    echo -e "${GREEN}熔断降级测试完成！${NC}"
}

# =================================================================
# 测试 4: 所有路由测试
# =================================================================
test_all_routes() {
    echo ""
    echo "=========================================="
    echo "   测试所有 Gateway 路由"
    echo "=========================================="
    echo ""

    echo "[1] 测试 User Service - Students 路由（应该返回 401 未认证）:"
    curl -s "http://localhost:8090/api/users/students" | jq '.'

    echo ""
    echo "[2] 测试 Catalog Service 路由（应该返回 401 未认证）:"
    curl -s "http://localhost:8090/api/courses" | jq '.'

    echo ""
    echo "[3] 测试 Enrollment Service 路由（应该返回 401 未认证）:"
    curl -s "http://localhost:8090/api/enrollments" | jq '.'

    echo ""
    echo "[4] 查看 Gateway 健康检查:"
    curl -s "http://localhost:8090/actuator/health" | jq '.'

    echo ""
    echo "[5] 尝试访问 Gateway routes:"
    curl -s "http://localhost:8090/actuator/gateway/routes" | jq '.' 2>/dev/null || echo "Gateway routes endpoint 不可用"

    echo ""
}

# =================================================================
# 主菜单
# =================================================================
show_menu() {
    echo ""
    echo "=========================================="
    echo "   CourseCloud 微服务测试脚本"
    echo "=========================================="
    echo ""
    echo "请选择要运行的测试："
    echo ""
    echo "  1) Gateway 和 JWT 认证测试"
    echo "  2) 负载均衡测试"
    echo "  3) 熔断降级测试"
    echo "  4) 所有路由测试"
    echo "  5) 运行所有测试"
    echo "  0) 退出"
    echo ""
    echo -n "请输入选项 [0-5]: "
}

# =================================================================
# 主程序
# =================================================================
main() {
    if [ $# -eq 1 ]; then
        # 命令行参数模式
        case $1 in
            1|gateway|jwt)
                test_gateway_jwt
                ;;
            2|loadbalance|lb)
                test_load_balance
                ;;
            3|circuit|breaker)
                test_circuit_breaker
                ;;
            4|routes)
                test_all_routes
                ;;
            5|all)
                test_gateway_jwt
                test_load_balance
                test_circuit_breaker
                test_all_routes
                ;;
            *)
                echo "无效的选项: $1"
                echo "用法: $0 [1|gateway|jwt|2|loadbalance|lb|3|circuit|breaker|4|routes|5|all]"
                exit 1
                ;;
        esac
    else
        # 交互式菜单模式
        while true; do
            show_menu
            read -r choice
            case $choice in
                1)
                    test_gateway_jwt
                    ;;
                2)
                    test_load_balance
                    ;;
                3)
                    test_circuit_breaker
                    ;;
                4)
                    test_all_routes
                    ;;
                5)
                    test_gateway_jwt
                    test_load_balance
                    test_circuit_breaker
                    test_all_routes
                    ;;
                0)
                    echo "退出测试脚本"
                    exit 0
                    ;;
                *)
                    echo -e "${RED}无效的选项，请重新选择${NC}"
                    ;;
            esac
        done
    fi
}

# 运行主程序
main "$@"
