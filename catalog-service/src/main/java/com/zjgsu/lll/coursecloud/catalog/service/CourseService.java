package com.zjgsu.coursecloud.catalog.service;

import com.zjgsu.coursecloud.catalog.model.Course;
import com.zjgsu.coursecloud.catalog.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * 获取所有课程
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * 根据ID获取课程
     */
    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id);
    }

    /**
     * 根据课程代码获取课程
     */
    public Optional<Course> getCourseByCode(String code) {
        return courseRepository.findByCode(code);
    }

    /**
     * 创建课程
     */
    public Course createCourse(Course course) {
        // 检查课程代码是否已存在
        if (courseRepository.findByCode(course.getCode()).isPresent()) {
            throw new IllegalArgumentException("Course with code " + course.getCode() + " already exists");
        }
        return courseRepository.save(course);
    }

    /**
     * 更新课程
     */
    public Course updateCourse(String id, Course updatedCourse) {
        return courseRepository.findById(id)
                .map(existingCourse -> {
                    existingCourse.setCode(updatedCourse.getCode());
                    existingCourse.setTitle(updatedCourse.getTitle());
                    existingCourse.setInstructor(updatedCourse.getInstructor());
                    existingCourse.setSchedule(updatedCourse.getSchedule());
                    existingCourse.setCapacity(updatedCourse.getCapacity());
                    existingCourse.setEnrolled(updatedCourse.getEnrolled());
                    return courseRepository.save(existingCourse);
                })
                .orElseThrow(() -> new IllegalArgumentException("Course with id " + id + " not found"));
    }

    /**
     * 删除课程
     */
    public void deleteCourse(String id) {
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException("Course with id " + id + " not found");
        }
        courseRepository.deleteById(id);
    }

    /**
     * 增加课程的选课人数
     */
    public void incrementEnrolled(String courseId) {
        courseRepository.findById(courseId).ifPresent(course -> {
            course.setEnrolled(course.getEnrolled() + 1);
            courseRepository.save(course);
        });
    }

    /**
     * 减少课程的选课人数
     */
    public void decrementEnrolled(String courseId) {
        courseRepository.findById(courseId).ifPresent(course -> {
            if (course.getEnrolled() > 0) {
                course.setEnrolled(course.getEnrolled() - 1);
                courseRepository.save(course);
            }
        });
    }

    /**
     * 检查课程是否还有容量
     */
    public boolean hasCapacity(String courseId) {
        return courseRepository.findById(courseId)
                .map(course -> course.getEnrolled() < course.getCapacity())
                .orElse(false);
    }
}
