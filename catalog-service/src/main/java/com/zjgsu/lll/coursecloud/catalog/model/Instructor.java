package com.zjgsu.lll.coursecloud.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class Instructor {

    @NotBlank(message = "Instructor ID is required")
    @Column(name = "instructor_id", nullable = false, length = 50)
    private String id;

    @NotBlank(message = "Instructor name is required")
    @Column(name = "instructor_name", nullable = false, length = 100)
    private String name;

    @Email(message = "Instructor email should be valid")
    @Column(name = "instructor_email", length = 100)
    private String email;

    public Instructor() {
    }

    public Instructor(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}