package com.zjgsu.lll.coursecloud.catalog.controller;

import com.zjgsu.lll.coursecloud.catalog.model.Course;

public record CourseResponse(
        String id,
        String code,
        String title,
        String instructorName,
        String instructorEmail,
        String dayOfWeek,
        String start,
        String end,
        int capacity,
        int expectedAttendance
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getInstructor().getName(),
                course.getInstructor().getEmail(),
                course.getSchedule().getDayOfWeek().name(),
                course.getSchedule().getStart().toString(),
                course.getSchedule().getEnd().toString(),
                course.getCapacity(),
                course.getSchedule().expectedAttendance()
        );
    }
}
