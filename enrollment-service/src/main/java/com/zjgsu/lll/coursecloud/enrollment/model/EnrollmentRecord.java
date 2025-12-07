package com.zjgsu.lll.coursecloud.enrollment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", indexes = {
    @Index(name = "idx_course_id", columnList = "course_id"),
    @Index(name = "idx_student_id", columnList = "student_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_course_student", columnNames = {"course_id", "student_id"})
})
public class EnrollmentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Course ID is required")
    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @NotBlank(message = "Student ID is required")
    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    public EnrollmentRecord() {
        // JPA requires no-arg constructor
    }

    public EnrollmentRecord(String courseId, String studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }

    @PrePersist
    protected void onCreate() {
        this.enrolledAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
