package com.zjgsu.lll.coursecloud.catalog.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_course_code", columnList = "code")
})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Course code is required")
    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @NotBlank(message = "Course title is required")
    @Column(nullable = false, length = 200)
    private String title;

    @Valid
    @Embedded
    private Instructor instructor;

    @Valid
    @Embedded
    private ScheduleSlot schedule;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer enrolled = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Course() {
        // JPA requires no-arg constructor
    }

    public Course(String code, String title, Instructor instructor, ScheduleSlot schedule, Integer capacity) {
        if (capacity != null && capacity <= 0) {
            throw new IllegalArgumentException("Course capacity must be positive");
        }
        this.code = code;
        this.title = title;
        this.instructor = instructor;
        this.schedule = schedule;
        this.capacity = capacity;
        this.enrolled = 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public ScheduleSlot getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleSlot schedule) {
        this.schedule = schedule;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getEnrolled() {
        return enrolled;
    }

    public void setEnrolled(Integer enrolled) {
        this.enrolled = enrolled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public boolean isFull() {
        return enrolled >= capacity;
    }

    public void incrementEnrolled() {
        if (isFull()) {
            throw new IllegalStateException("Course is already full");
        }
        this.enrolled++;
    }

    public void decrementEnrolled() {
        if (enrolled > 0) {
            this.enrolled--;
        }
    }

    public Duration duration() {
        return schedule != null ? schedule.duration() : Duration.ZERO;
    }
}
