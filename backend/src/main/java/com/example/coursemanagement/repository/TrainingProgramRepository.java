package com.example.coursemanagement.repository;

import com.example.coursemanagement.entity.TrainingProgram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 培养方案数据访问层，使用Spring JDBC Template实现
 */
@Repository
public class TrainingProgramRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile Set<String> availableColumns;

    /**
     * 查询所有培养方案
     */
    public List<TrainingProgram> findAll() {
        String sql = "SELECT * FROM training_program";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TrainingProgram.class));
    }

    /**
     * 根据ID查询培养方案
     */
    public Optional<TrainingProgram> findById(Integer id) {
        String sql = "SELECT * FROM training_program WHERE program_id = ?";
        List<TrainingProgram> programs = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TrainingProgram.class), id);
        return programs.isEmpty() ? Optional.empty() : Optional.of(programs.get(0));
    }

    /**
     * 新增培养方案
     */
    public int save(TrainingProgram program) {
        try {
            return doSave(program);
        } catch (BadSqlGrammarException ex) {
            if (isUnknownColumnError(ex)) {
                resetAvailableColumns();
                return doSave(program);
            }
            throw ex;
        }
    }

    private int doSave(TrainingProgram program) {
        Set<String> columns = getAvailableColumns();
        List<String> insertColumns = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (columns.contains("major_name")) {
            insertColumns.add("major_name");
            params.add(program.getMajorName());
        }
        if (columns.contains("duration")) {
            insertColumns.add("duration");
            params.add(program.getDuration());
        }
        if (columns.contains("total_credit")) {
            insertColumns.add("total_credit");
            params.add(program.getTotalCredit());
        }
        if (columns.contains("effective_year")) {
            insertColumns.add("effective_year");
            params.add(program.getEffectiveYear());
        }
        if (columns.contains("description")) {
            insertColumns.add("description");
            params.add(program.getDescription());
        }

        if (insertColumns.isEmpty() && !columns.contains("create_time") && !columns.contains("update_time")) {
            return 0;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO training_program (");
        sql.append(String.join(", ", insertColumns));
        if (columns.contains("create_time")) {
            sql.append(insertColumns.isEmpty() ? "create_time" : ", create_time");
        }
        if (columns.contains("update_time")) {
            sql.append(insertColumns.isEmpty() && !columns.contains("create_time") ? "update_time" : ", update_time");
        }
        sql.append(") VALUES (");

        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < insertColumns.size(); i++) {
            placeholders.add("?");
        }
        if (columns.contains("create_time")) {
            placeholders.add("NOW()");
        }
        if (columns.contains("update_time")) {
            placeholders.add("NOW()");
        }
        sql.append(String.join(", ", placeholders));
        sql.append(")");

        return jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * 更新培养方案
     */
    public int update(TrainingProgram program) {
        try {
            return doUpdate(program);
        } catch (BadSqlGrammarException ex) {
            if (isUnknownColumnError(ex)) {
                resetAvailableColumns();
                return doUpdate(program);
            }
            throw ex;
        }
    }

    private int doUpdate(TrainingProgram program) {
        Set<String> columns = getAvailableColumns();
        List<String> updates = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (columns.contains("major_name")) {
            updates.add("major_name = ?");
            params.add(program.getMajorName());
        }
        if (columns.contains("duration")) {
            updates.add("duration = ?");
            params.add(program.getDuration());
        }
        if (columns.contains("total_credit")) {
            updates.add("total_credit = ?");
            params.add(program.getTotalCredit());
        }
        if (columns.contains("effective_year")) {
            updates.add("effective_year = ?");
            params.add(program.getEffectiveYear());
        }
        if (columns.contains("description")) {
            updates.add("description = ?");
            params.add(program.getDescription());
        }
        if (columns.contains("update_time")) {
            updates.add("update_time = NOW()");
        }

        if (updates.isEmpty()) {
            return 0;
        }

        StringBuilder sql = new StringBuilder("UPDATE training_program SET ");
        sql.append(String.join(", ", updates));
        sql.append(" WHERE program_id = ?");
        params.add(program.getProgramId());

        return jdbcTemplate.update(sql.toString(), params.toArray());
    }

    private Set<String> getAvailableColumns() {
        if (availableColumns != null) {
            return availableColumns;
        }
        synchronized (this) {
            if (availableColumns != null) {
                return availableColumns;
            }
            List<String> columns = jdbcTemplate.query(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'training_program'",
                    (rs, rowNum) -> rs.getString("column_name")
            );
            availableColumns = Collections.unmodifiableSet(new HashSet<>(columns));
            return availableColumns;
        }
    }

    private void resetAvailableColumns() {
        availableColumns = null;
    }

    private boolean isUnknownColumnError(BadSqlGrammarException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("Unknown column");
    }

    /**
     * 删除培养方案
     */
    public int deleteById(Integer id) {
        String sql = "DELETE FROM training_program WHERE program_id = ?";
        return jdbcTemplate.update(sql, id);
    }

    /**
     * 批量删除培养方案
     */
    public int deleteBatch(List<Integer> ids) {
        String sql = "DELETE FROM training_program WHERE program_id IN (" + ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("") + ")";
        return jdbcTemplate.update(sql, ids.toArray());
    }

    /**
     * 分页查询培养方案
     */
    public List<TrainingProgram> findByPage(int page, int limit) {
        int offset = (page - 1) * limit;
        String sql = "SELECT * FROM training_program LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TrainingProgram.class), limit, offset);
    }

    /**
     * 查询培养方案总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM training_program";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 根据老师ID查询培养方案列表
     */
    public List<TrainingProgram> findByTeacherId(Integer teacherId) {
        // 由于表中没有teacher_id字段，返回空列表或所有记录
        // 这里返回所有记录作为临时解决方案
        return findAll();
    }
}
