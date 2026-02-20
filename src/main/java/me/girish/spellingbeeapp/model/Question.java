package me.girish.spellingbeeapp.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String word;

    @Column(length = 1000)
    private String questionText;

    @ElementCollection
    @Column(length = 1000)
    private List<String> options;

    private String correctAnswer;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    public enum QuestionType {
        SPELLING, VOCABULARY
    }

    public Question() {
    }

    public Question(Long id, String word, String questionText, List<String> options, String correctAnswer,
            QuestionType type) {
        this.id = id;
        this.word = word;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }
}
