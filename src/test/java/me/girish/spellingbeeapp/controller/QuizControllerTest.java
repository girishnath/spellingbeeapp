package me.girish.spellingbeeapp.controller;

import me.girish.spellingbeeapp.model.Student;
import me.girish.spellingbeeapp.service.DictionaryService;
import me.girish.spellingbeeapp.service.QuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @InjectMocks
    private QuizController quizController;

    @Mock
    private QuizService quizService;

    @Mock
    private DictionaryService dictionaryService;

    @Test
    void testLogin() {
        Student student = new Student("testuser", "Test User");
        when(quizService.registerStudent(anyString())).thenReturn(student);

        ResponseEntity<Student> response = quizController.login("Test User");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testuser", response.getBody().getId());
    }

    @Test
    void testStartQuiz() {
        // Fix: Use correct return type for mock
        when(quizService.generateQuiz(anyString(), anyString())).thenReturn(Collections.emptyList());

        ResponseEntity<List<me.girish.spellingbeeapp.model.Question>> response = quizController.startQuiz("testuser", "NORMAL");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testGetDefinition() {
        when(dictionaryService.getDefinition("hello")).thenReturn(Optional.of("A greeting"));

        ResponseEntity<String> response = quizController.getDefinition("hello");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("A greeting", response.getBody());
    }

    @Test
    void testGetDefinition_NotFound() {
        when(dictionaryService.getDefinition("unknown")).thenReturn(Optional.empty());

        ResponseEntity<String> response = quizController.getDefinition("unknown");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
