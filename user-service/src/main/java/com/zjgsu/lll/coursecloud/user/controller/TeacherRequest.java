package com.zjgsu.lll.coursecloud.user.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TeacherRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Teacher ID is required")
    String teacherId,

    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "Department is required")
    String department,

    @NotBlank(message = "Title is required")
    String title
) {
}
