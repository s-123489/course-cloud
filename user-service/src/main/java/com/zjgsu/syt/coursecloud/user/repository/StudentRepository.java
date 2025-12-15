package com.zjgsu.syt.coursecloud.user.repository;

import com.zjgsu.syt.coursecloud.user.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    boolean existsByEmail(String email);

}
