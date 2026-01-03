package com.example.coursemanagement.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培养方案实体类
 */
@Data
public class TrainingProgram {

    private Integer programId;
    private String programCode;
    private String programName;
    private Integer majorId;
    private String majorName;
    private String startGrade;
    private Integer duration;
    private Double totalCredit;
    private Integer effectiveYear;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer teacherId;
}
