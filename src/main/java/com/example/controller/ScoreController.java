package com.example.controller;

import com.example.entity.Score;
import com.example.repository.ScoreRepository;
import com.example.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;

    public ScoreController(ScoreRepository scoreRepository, StudentRepository studentRepository) {
        this.scoreRepository = scoreRepository;
        this.studentRepository = studentRepository;
    }

    // 获取所有成绩
    @GetMapping
    public List<Score> getAllScores() {
        return scoreRepository.findAll();
    }

    // 根据ID获取成绩
    @GetMapping("/{id}")
    public ResponseEntity<Score> getScoreById(@PathVariable Long id) {
        return scoreRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 根据学生ID获取成绩
    @GetMapping("/student/{studentId}")
    public List<Score> getScoresByStudentId(@PathVariable Long studentId) {
        return scoreRepository.findByStudentId(studentId);
    }

    // 创建成绩
    @PostMapping
    public ResponseEntity<Score> createScore(@RequestBody Score score) {
        try {
            // 检查学生是否存在
            if (!studentRepository.existsById(score.getStudentId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            Score savedScore = scoreRepository.save(score);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedScore);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 更新成绩
    @PutMapping("/{id}")
    public ResponseEntity<Score> updateScore(@PathVariable Long id, @RequestBody Score score) {
        if (!scoreRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // 检查学生是否存在
        if (!studentRepository.existsById(score.getStudentId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        score.setId(id);
        Score updatedScore = scoreRepository.update(score);
        return ResponseEntity.ok(updatedScore);
    }

    // 删除成绩
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScore(@PathVariable Long id) {
        if (!scoreRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scoreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
