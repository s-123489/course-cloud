package com.zjgsu.lll.coursecloud.user.controller;

import com.zjgsu.lll.coursecloud.user.model.Teacher;

import java.time.LocalDateTime;

public record TeacherResponse(
    String id,
    String username,
    String email,
    String teacherId,
    String name,
    String department,
    String title,
    LocalDateTime createdAt
) {
    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUsername(),
            teacher.getEmail(),
            teacher.getTeacherId(),
            teacher.getName(),
            teacher.getDepartment(),
            teacher.getTitle(),
            teacher.getCreatedAt()
        );
    }
}
