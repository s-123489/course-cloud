package com.zjgsu.coursecloud.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("STUDENT")
public class Student extends User {
    @NotBlank(message = "Student ID is required")
    @Column(name = "student_id", unique = true, length = 20)
    private String studentId;

    @NotBlank(message = "Name is required")
    @Column(length = 100)
    private String name;

    @NotBlank(message = "Major is required")
    @Column(length = 100)
    private String major;

    @NotNull(message = "Grade is required")
    @Column
    private Integer grade;

    public Student() {
        super();
    }

    public Student(String username, String email, String studentId, String name, String major, Integer grade) {
        super(username, email, UserType.STUDENT);
        this.studentId = studentId;
        this.name = name;
        this.major = major;
        this.grade = grade;
    }

    // Getters and setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }
}
