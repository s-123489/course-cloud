package com.zjgsu.syt.coursecloud.enrollment.service;

import com.zjgsu.syt.coursecloud.enrollment.client.UserClient;
import com.zjgsu.syt.coursecloud.enrollment.client.CatalogClient;
import com.zjgsu.syt.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.syt.coursecloud.enrollment.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final UserClient userClient;
    private final CatalogClient catalogClient;
    private final EnrollmentRepository repository;

    public EnrollmentService(UserClient userClient, CatalogClient catalogClient, EnrollmentRepository repository) {
        this.userClient = userClient;
        this.catalogClient = catalogClient;
        this.repository = repository;
    }

    public EnrollmentRecord enroll(String courseId, String studentId) {
        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);

        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        try {
            Map<String, Object> studentResponse = userClient.getStudent(studentId);
            log.info("✅ 学生验证成功: {}", studentResponse);
        } catch (Exception e) {
            log.error("调用 user-service 出错: {}", e.getMessage(), e);
            throw new RuntimeException("user-service unavailable: " + e.getMessage());
        }

        try {
            Map<String, Object> courseResponse = catalogClient.getCourse(courseId);
            log.info("✅ 课程验证成功: {}", courseResponse);
        } catch (Exception e) {
            log.error("调用 catalog-service 出错: {}", e.getMessage(), e);
            throw new RuntimeException("catalog-service unavailable: " + e.getMessage());
        }

        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
        EnrollmentRecord saved = repository.save(record);
        log.info("选课成功: {}", saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByCourse(String courseId) {
        return repository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByStudent(String studentId) {
        return repository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listAll() {
        return repository.findAll();
    }
}
