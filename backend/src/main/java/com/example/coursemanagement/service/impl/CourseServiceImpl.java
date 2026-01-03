package com.example.coursemanagement.service.impl;

import com.example.coursemanagement.entity.Course;
import com.example.coursemanagement.repository.CourseRepository;
import com.example.coursemanagement.repository.CourseSemesterRepository;
import com.example.coursemanagement.repository.SemesterRepository;
import com.example.coursemanagement.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程Service实现类
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CourseSemesterRepository courseSemesterRepository;
    
    @Autowired
    private SemesterRepository semesterRepository;

    @Override
    @Cacheable(value = "course", key = "'all'")
    public List<Course> list() {
        return courseRepository.findAll();
    }

    @Override
    @Cacheable(value = "course", key = "#id")
    public Course getById(Integer id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    @CacheEvict(value = "course", allEntries = true)
    public boolean save(Course course) {
        return courseRepository.save(course) > 0;
    }

    @Override
    @CacheEvict(value = "course", allEntries = true)
    public boolean updateById(Course course) {
        return courseRepository.update(course) > 0;
    }

    @Override
    @CacheEvict(value = "course", allEntries = true)
    public boolean removeById(Integer id) {
        return courseRepository.deleteById(id) > 0;
    }

    @Override
    @CacheEvict(value = "course", allEntries = true)
    public boolean removeByIds(List<Integer> ids) {
        return courseRepository.deleteBatch(ids) > 0;
    }

    @Override
    @Cacheable(value = "course", key = "'page:' + #page + '-limit:' + #limit")
    public List<Course> listPage(Integer page, Integer limit) {
        return courseRepository.findByPage(page, limit);
    }

    @Override
    @Cacheable(value = "course", key = "'count'")
    public int count() {
        return courseRepository.count();
    }
    
    @Override
    @CacheEvict(value = "course", allEntries = true)
    public boolean saveCourseSemester(Integer courseId, List<Integer> semesterIds) {
        try {
            courseSemesterRepository.batchSave(courseId, semesterIds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<Integer> getSemesterIdsByCourseId(Integer courseId) {
        return courseSemesterRepository.findSemesterIdsByCourseId(courseId);
    }
    
    @Override
    public List<Course> getCoursesBySemesterId(Integer semesterId) {
        List<Integer> courseIds = courseSemesterRepository.findCourseIdsBySemesterId(semesterId);
        return courseIds.stream()
                .map(courseId -> courseRepository.findById(courseId).orElse(null))
                .filter(course -> course != null)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> getCoursesByProgramId(Integer programId) {
        return courseRepository.findByProgramId(programId);
    }
    
    @Override
    public List<Course> getCoursesByMajorId(Integer majorId) {
        return courseRepository.findByMajorId(majorId);
    }
    
    @Override
    public List<Course> getCoursesByMajorAndSemester(Integer majorId, Integer semesterId) {
        return courseRepository.findByMajorIdAndSemesterId(majorId, semesterId);
    }
    
    @Override
    public Map<Integer, List<Course>> getFullCourseScheduleByProgramId(Integer programId) {
        Map<Integer, List<Course>> schedule = new HashMap<>();
        // 假设学期ID为1-8，对应四年八个学期
        for (int i = 1; i <= 8; i++) {
            List<Course> courses = courseRepository.findByProgramIdAndSemesterId(programId, i);
            schedule.put(i, courses);
        }
        return schedule;
    }
    
    @Override
    public Map<String, Object> getCourseStatistics(Integer programId) {
        List<Course> courses = courseRepository.findByProgramId(programId);
        return buildCourseStatistics(courses);
    }

    @Override
    public Map<String, Object> getCourseStatisticsByProgramAndSemester(Integer programId, Integer semesterId) {
        List<Course> courses = courseRepository.findByProgramIdAndSemesterId(programId, semesterId);
        return buildCourseStatistics(courses);
    }

    private Map<String, Object> buildCourseStatistics(List<Course> courses) {
        Map<String, Object> statistics = new HashMap<>();

        Double totalCredit = courses.stream()
                .mapToDouble(course -> course.getCredit() != null ? course.getCredit() : 0.0)
                .sum();
        Integer totalHours = courses.stream()
                .mapToInt(course -> course.getTotalHours() != null ? course.getTotalHours() : 0)
                .sum();
        Integer totalTheoreticalHours = courses.stream()
                .mapToInt(course -> course.getTheoreticalHours() != null ? course.getTheoreticalHours() : 0)
                .sum();
        Integer totalPracticalHours = courses.stream()
                .mapToInt(course -> course.getPracticalHours() != null ? course.getPracticalHours() : 0)
                .sum();

        Map<String, Long> courseTypeCount = courses.stream()
                .collect(Collectors.groupingBy(course -> course.getCourseType() != null ? course.getCourseType() : "未分类", Collectors.counting()));
        Map<String, Long> courseNatureCount = courses.stream()
                .collect(Collectors.groupingBy(course -> course.getCourseNature() != null ? course.getCourseNature() : "未分类", Collectors.counting()));
        Map<String, Long> courseCategoryCount = courses.stream()
                .collect(Collectors.groupingBy(course -> course.getCourseCategory() != null ? course.getCourseCategory() : "未分类", Collectors.counting()));

        statistics.put("totalCourses", courses.size());
        statistics.put("totalCredit", totalCredit);
        statistics.put("totalHours", totalHours);
        statistics.put("totalTheoreticalHours", totalTheoreticalHours);
        statistics.put("totalPracticalHours", totalPracticalHours);
        statistics.put("courseTypeCount", courseTypeCount);
        statistics.put("courseNatureCount", courseNatureCount);
        statistics.put("courseCategoryCount", courseCategoryCount);

        return statistics;
    }
}
