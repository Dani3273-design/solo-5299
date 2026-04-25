package com.example.repository;

import com.example.entity.Score;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ScoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public ScoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 查询所有成绩
    public List<Score> findAll() {
        String sql = "SELECT * FROM score";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Score.class));
    }

    // 根据ID查询成绩
    public Optional<Score> findById(Long id) {
        String sql = "SELECT * FROM score WHERE id = ?";
        try {
            Score score = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Score.class), id);
            return Optional.ofNullable(score);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // 根据学生ID查询成绩
    public List<Score> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM score WHERE student_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Score.class), studentId);
    }

    // 新增成绩
    public Score save(Score score) {
        String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, score.getStudentId());
            ps.setString(2, score.getSubject());
            ps.setObject(3, score.getScore());
            ps.setString(4, score.getExamDate());
            return ps;
        }, keyHolder);

        // 获取生成的ID
        Long generatedId = keyHolder.getKey().longValue();
        score.setId(generatedId);
        return score;
    }

    // 更新成绩
    public Score update(Score score) {
        String sql = "UPDATE score SET student_id = ?, subject = ?, score = ?, exam_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                score.getStudentId(),
                score.getSubject(),
                score.getScore(),
                score.getExamDate(),
                score.getId());
        return score;
    }

    // 删除成绩
    public void deleteById(Long id) {
        String sql = "DELETE FROM score WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // 根据学生ID删除成绩
    public void deleteByStudentId(Long studentId) {
        String sql = "DELETE FROM score WHERE student_id = ?";
        jdbcTemplate.update(sql, studentId);
    }

    // 检查成绩是否存在
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM score WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
