package me.girish.spellingbeeapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private int score;
    private int totalQuestions;
    private LocalDateTime timestamp;

    public TestResult() {
    }

    public TestResult(Long id, Student student, int score, int totalQuestions, LocalDateTime timestamp) {
        this.id = id;
        this.student = student;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
