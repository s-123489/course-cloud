package com.zjgsu.syt.coursecloud.catalog.repository;

import com.zjgsu.syt.coursecloud.catalog.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    Optional<Course> findByCode(String code);
    boolean existsByCode(String code);
}
