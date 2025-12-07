#!/bin/bash

echo "=== 测试微服务通过 Nacos 的服务发现 ==="

# 1. 创建学生
echo -e "\n=== 1. 创建学生 ==="
STUDENT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lisi",
    "email": "lisi@example.edu.cn",
    "studentId": "2024002",
    "name": "李四",
    "major": "软件工程",
    "grade": 2024
  }')
echo $STUDENT_RESPONSE | jq '.'

# 2. 获取所有学生
echo -e "\n=== 2. 获取所有学生 ==="
curl -s http://localhost:8081/api/students | jq '.students'

# 3. 创建课程
echo -e "\n=== 3. 创建课程 ==="
COURSE_RESPONSE=$(curl -s -X POST http://localhost:8082/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CS102",
    "title": "数据结构",
    "instructorId": "T002",
    "instructorName": "李教授",
    "instructorEmail": "li@example.edu.cn",
    "dayOfWeek": "TUESDAY",
    "start": "10:00",
    "end": "12:00",
    "capacity": 50,
    "expectedAttendance": 45
  }')
echo $COURSE_RESPONSE | jq '.'

# 提取课程ID
COURSE_ID=$(echo $COURSE_RESPONSE | jq -r '.id')
echo "课程ID: $COURSE_ID"

# 4. 获取所有课程
echo -e "\n=== 4. 获取所有课程 ==="
curl -s http://localhost:8082/api/courses | jq '.'

# 5. 测试选课（验证服务间通信）
echo -e "\n=== 5. 测试学生选课（验证服务间调用）==="
ENROLLMENT_RESPONSE=$(curl -s -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"studentId\": \"2024002\"
  }")
echo $ENROLLMENT_RESPONSE | jq '.'

# 6. 测试用已存在的学生选课
echo -e "\n=== 6. 使用已存在的学生(2024001)选课 ==="
ENROLLMENT_RESPONSE2=$(curl -s -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"studentId\": \"2024001\"
  }")
echo $ENROLLMENT_RESPONSE2 | jq '.'

# 7. 查询选课记录
echo -e "\n=== 7. 查询所有选课记录 ==="
curl -s http://localhost:8083/api/enrollments | jq '.'

# 8. 测试选课失败（学生不存在）
echo -e "\n=== 8. 测试选课失败（学生不存在）==="
curl -s -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"studentId\": \"9999999\"
  }" | jq '.'

# 9. 测试选课失败（课程不存在）
echo -e "\n=== 9. 测试选课失败（课程不存在）==="
curl -s -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "non-existent-course-id",
    "studentId": "2024001"
  }' | jq '.'

# 10. 测试重复选课
echo -e "\n=== 10. 测试重复选课（应该失败）==="
curl -s -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"studentId\": \"2024001\"
  }" | jq '.'

# 11. 查询特定学生的选课
echo -e "\n=== 11. 查询学生 2024001 的选课记录 ==="
curl -s http://localhost:8083/api/enrollments/student/2024001 | jq '.'

# 12. 查询特定课程的选课
echo -e "\n=== 12. 查询课程 $COURSE_ID 的选课记录 ==="
curl -s "http://localhost:8083/api/enrollments/course/$COURSE_ID" | jq '.'

echo -e "\n=== 测试完成 ==="