package com.zjgsu.coursecloud.user.service;

import com.zjgsu.coursecloud.user.model.Student;
import com.zjgsu.coursecloud.user.model.Teacher;
import com.zjgsu.coursecloud.user.repository.StudentRepository;
import com.zjgsu.coursecloud.user.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public UserService(StudentRepository studentRepository, TeacherRepository teacherRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }

    public Teacher createTeacher(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(String id) {
        return studentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Teacher> getTeacherById(String id) {
        return teacherRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }

    public void deleteTeacher(String id) {
        teacherRepository.deleteById(id);
    }

    public Optional<Student> getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public Optional<Teacher> getTeacherByTeacherId(String teacherId) {
        return teacherRepository.findByTeacherId(teacherId);
    }
    public Student updateStudent(Student student) {
        return studentRepository.save(student);
    }
}
