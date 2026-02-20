package me.girish.spellingbeeapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Mistake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    private String word;
    private String userAnswer;
    private String correctAnswer;

    @Enumerated(EnumType.STRING)
    private Question.QuestionType type;

    private LocalDateTime timestamp;

    public Mistake() {
    }

    public Mistake(Long id, Student student, String word, String userAnswer, String correctAnswer,
            Question.QuestionType type, LocalDateTime timestamp) {
        this.id = id;
        this.student = student;
        this.word = word;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.type = type;
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

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Question.QuestionType getType() {
        return type;
    }

    public void setType(Question.QuestionType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
