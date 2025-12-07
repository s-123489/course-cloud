package com.zjgsu.lll.coursecloud.catalog.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CourseRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}\\d{3}") String code,
        @NotBlank String title,
        @NotBlank String instructorId,
        @NotBlank String instructorName,
        @Email String instructorEmail,
        @NotNull DayOfWeekValue dayOfWeek,
        @NotBlank String start,
        @NotBlank String end,
        @Min(10) @Max(500) int capacity,
        @Min(10) @Max(500) int expectedAttendance
) {
}
