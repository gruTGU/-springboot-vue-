package com.example.coursemanagement.repository;

import com.example.coursemanagement.entity.TrainingProgram;

import java.util.List;
import java.util.Optional;

/**
 * 培养方案仓库接口，暴露服务层需要的操作。
 */
public interface TrainingProgramRepositoryApi {

    List<TrainingProgram> findAll();

    Optional<TrainingProgram> findById(Integer id);

    int save(TrainingProgram program);

    int update(TrainingProgram program);

    int deleteById(Integer id);

    int deleteBatch(List<Integer> ids);

    List<TrainingProgram> findByPage(int page, int limit);

    int count();

    List<TrainingProgram> findByTeacherId(Integer teacherId);
}