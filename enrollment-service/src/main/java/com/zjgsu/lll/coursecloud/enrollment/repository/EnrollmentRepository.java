package com.zjgsu.coursecloud.enrollment.repository;

import com.zjgsu.coursecloud.enrollment.model.EnrollmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentRecord, String> {

    // ✅ 使用显式查询避免方法名解析问题
    @Query("SELECT e FROM EnrollmentRecord e WHERE e.courseId = :courseId")
    List<EnrollmentRecord> findByCourseId(@Param("courseId") String courseId);

    @Query("SELECT e FROM EnrollmentRecord e WHERE e.studentId = :studentId")
    List<EnrollmentRecord> findByStudentId(@Param("studentId") String studentId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EnrollmentRecord e WHERE e.courseId = :courseId AND e.studentId = :studentId")
    boolean existsByCourseIdAndStudentId(@Param("courseId") String courseId, @Param("studentId") String studentId);
}