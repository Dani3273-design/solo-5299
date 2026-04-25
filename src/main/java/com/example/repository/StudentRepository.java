package com.example.repository;

import com.example.entity.Student;
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
public class StudentRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 查询所有学生
    public List<Student> findAll() {
        String sql = "SELECT * FROM student";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Student.class));
    }

    // 根据ID查询学生
    public Optional<Student> findById(Long id) {
        String sql = "SELECT * FROM student WHERE id = ?";
        try {
            Student student = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Student.class), id);
            return Optional.ofNullable(student);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // 新增学生
    public Student save(Student student) {
        String sql = "INSERT INTO student (name, age, gender, major) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, student.getName());
            ps.setObject(2, student.getAge());
            ps.setString(3, student.getGender());
            ps.setString(4, student.getMajor());
            return ps;
        }, keyHolder);

        // 获取生成的ID
        Long generatedId = keyHolder.getKey().longValue();
        student.setId(generatedId);
        return student;
    }

    // 更新学生
    public Student update(Student student) {
        String sql = "UPDATE student SET name = ?, age = ?, gender = ?, major = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                student.getName(),
                student.getAge(),
                student.getGender(),
                student.getMajor(),
                student.getId());
        return student;
    }

    // 删除学生
    public void deleteById(Long id) {
        String sql = "DELETE FROM student WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // 检查学生是否存在
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM student WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
