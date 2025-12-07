package com.zjgsu.coursecloud.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

@Entity
@DiscriminatorValue("TEACHER")
public class Teacher extends User {
    @NotBlank(message = "Teacher ID is required")
    @Column(name = "teacher_id", unique = true, length = 20)
    private String teacherId;

    @NotBlank(message = "Name is required")
    @Column(length = 100)
    private String name;

    @NotBlank(message = "Department is required")
    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String title;

    public Teacher() {
        super();
    }

    public Teacher(String username, String email, String teacherId, String name, String department, String title) {
        super(username, email, UserType.TEACHER);
        this.teacherId = teacherId;
        this.name = name;
        this.department = department;
        this.title = title;
    }

    // Getters and setters
    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
