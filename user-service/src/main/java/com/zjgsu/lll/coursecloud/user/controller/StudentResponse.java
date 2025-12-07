package com.zjgsu.lll.coursecloud.user.controller;

import com.zjgsu.lll.coursecloud.user.model.Student;

import java.time.LocalDateTime;

public record StudentResponse(
    String id,
    String username,
    String email,
    String studentId,
    String name,
    String major,
    int grade,
    LocalDateTime createdAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getUsername(),
            student.getEmail(),
            student.getStudentId(),
            student.getName(),
            student.getMajor(),
            student.getGrade(),
            student.getCreatedAt()
        );
    }
}
