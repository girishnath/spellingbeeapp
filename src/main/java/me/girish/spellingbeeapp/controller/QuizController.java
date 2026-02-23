package me.girish.spellingbeeapp.controller;

import me.girish.spellingbeeapp.dto.SubmissionDTO;
import me.girish.spellingbeeapp.model.Question;
import me.girish.spellingbeeapp.model.Student;
import me.girish.spellingbeeapp.model.TestResult;
import me.girish.spellingbeeapp.model.Mistake;
import me.girish.spellingbeeapp.service.DictionaryService;
import me.girish.spellingbeeapp.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QuizController {

    @Autowired
    private QuizService quizService;
    @Autowired
    private DictionaryService dictionaryService;

    @PostMapping("/login")
    public ResponseEntity<Student> login(@RequestParam("name") String name) {
        return ResponseEntity.ok(quizService.registerStudent(name));
    }

    @GetMapping("/quiz/{studentId}")
    public ResponseEntity<List<Question>> startQuiz(@PathVariable("studentId") String studentId,
            @RequestParam(name = "mode", defaultValue = "NORMAL") String mode) {
        return ResponseEntity.ok(quizService.generateQuiz(studentId, mode));
    }

    @GetMapping("/study/words")
    public ResponseEntity<List<Question>> getStudyWords(@RequestParam("category") String category,
            @RequestParam("letter") String letter) {
        return ResponseEntity.ok(quizService.getStudyWords(category, letter));
    }

    @PostMapping("/submit")
    public ResponseEntity<TestResult> submit(@RequestBody SubmissionDTO submission) {
        return ResponseEntity.ok(quizService.submitQuiz(submission));
    }

    @GetMapping("/definition/{word}")
    public ResponseEntity<String> getDefinition(@PathVariable("word") String word) {
        return ResponseEntity.of(dictionaryService.getDefinition(word));
    }

    @GetMapping("/mistakes/{studentId}")
    public ResponseEntity<List<Mistake>> getMistakes(@PathVariable("studentId") String studentId) {
        return ResponseEntity.ok(quizService.getMistakes(studentId));
    }
}
