package com.zjgsu.lll.coursecloud.enrollment.service;

import com.zjgsu.lll.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.lll.coursecloud.enrollment.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final RestTemplate restTemplate;
    private final EnrollmentRepository repository;

    // ⭐⭐⭐ 关键修改：不再使用 @Value 读取 URL，直接使用服务名
    private static final String USER_SERVICE_URL = "http://user-service";  // Nacos 服务名
    private static final String CATALOG_SERVICE_URL = "http://catalog-service";  // Nacos 服务名

    public EnrollmentService(RestTemplate restTemplate, EnrollmentRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    public EnrollmentRecord enroll(String courseId, String studentId) {
        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);

        // Check if already enrolled
        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        // 1. ⭐ 通过服务名调用 user-service
        try {
            String userUrl = USER_SERVICE_URL + "/api/students/studentId/" + studentId;
            log.info("调用 user-service 验证学生: {}", userUrl);

            Map<String, Object> studentResponse = restTemplate.getForObject(userUrl, Map.class);
            log.info("学生验证成功，响应来自端口: {}", studentResponse.get("port"));  // ✅ 日志显示负载均衡

        } catch (HttpClientErrorException.NotFound e) {
            log.error("学生不存在: {}", studentId);
            throw new IllegalArgumentException("Student not found: " + studentId);
        } catch (Exception e) {
            log.error("验证学生时出错: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying student with user-service: " + e.getMessage());
        }

        // 2. ⭐ 通过服务名调用 catalog-service
        try {
            String courseUrl = CATALOG_SERVICE_URL + "/api/courses/" + courseId;
            log.info("调用 catalog-service 验证课程: {}", courseUrl);

            Map<String, Object> courseResponse = restTemplate.getForObject(courseUrl, Map.class);

            if (courseResponse == null) {
                log.error("课程不存在: {}", courseId);
                throw new IllegalArgumentException("Course not found: " + courseId);
            }

            log.info("课程验证成功，响应来自端口: {}", courseResponse.get("port"));  // ✅ 日志显示负载均衡

            // 处理嵌套的 data 结构
            Object dataObj = courseResponse.get("data");
            Map<String, Object> courseData = dataObj instanceof Map ? (Map<String, Object>) dataObj : courseResponse;

            Integer capacity = (Integer) courseData.get("capacity");
            Integer enrolled = (Integer) courseData.get("enrolled");

            log.debug("课程容量检查: capacity={}, enrolled={}", capacity, enrolled);

            if (enrolled != null && capacity != null && enrolled >= capacity) {
                log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
                throw new IllegalStateException("Course capacity reached");
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.error("课程不存在: {}", courseId);
            throw new IllegalArgumentException("Course not found: " + courseId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证课程时出错: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying course with catalog-service: " + e.getMessage());
        }

        // 3. Create enrollment record
        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
        EnrollmentRecord saved = repository.save(record);

        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByCourse(String courseId) {
        log.debug("查询课程的选课记录: courseId={}", courseId);
        return repository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByStudent(String studentId) {
        log.debug("查询学生的选课记录: studentId={}", studentId);
        return repository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listAll() {
        log.debug("查询所有选课记录");
        return repository.findAll();
    }
}