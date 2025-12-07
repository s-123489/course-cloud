package com.zjgsu.lll.coursecloud.catalog.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

@Embeddable
public class ScheduleSlot {
    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Expected attendance is required")
    private Integer expectedAttendance;

    public ScheduleSlot() {
        // JPA requires no-arg constructor
    }

    public ScheduleSlot(DayOfWeek dayOfWeek, LocalTime start, LocalTime end, int expectedAttendance) {
        if (end.isBefore(start) || end.equals(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (expectedAttendance <= 0) {
            throw new IllegalArgumentException("Expected attendance must be positive");
        }
        this.dayOfWeek = dayOfWeek;
        this.startTime = start;
        this.endTime = end;
        this.expectedAttendance = expectedAttendance;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStart() {
        return startTime;
    }

    public void setStart(LocalTime start) {
        this.startTime = start;
    }

    public LocalTime getEnd() {
        return endTime;
    }

    public void setEnd(LocalTime end) {
        this.endTime = end;
    }

    public Integer expectedAttendance() {
        return expectedAttendance;
    }

    public void setExpectedAttendance(Integer expectedAttendance) {
        this.expectedAttendance = expectedAttendance;
    }

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }
}
