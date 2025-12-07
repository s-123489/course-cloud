package com.zjgsu.lll.coursecloud.user.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StudentRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Student ID is required")
    String studentId,

    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "Major is required")
    String major,

    @Positive(message = "Grade must be positive")
    int grade
) {
}
