package com.example.entity;

public class Score {
    private Long id;
    private Long studentId;
    private String subject;
    private Double score;
    private String examDate;

    public Score() {
    }

    public Score(Long id, Long studentId, String subject, Double score, String examDate) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.score = score;
        this.examDate = examDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = examDate;
    }
}
