package me.girish.spellingbeeapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSmartMisspellings_Phonetics() throws Exception {
        Method method = QuizService.class.getDeclaredMethod("generateSmartMisspellings", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) method.invoke(quizService, "phonics");

        assertTrue(result.contains("fonics"), "Should contain 'fonics' (ph -> f)");
    }

    @Test
    void testSmartMisspellings_Suffixes() throws Exception {
        Method method = QuizService.class.getDeclaredMethod("generateSmartMisspellings", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) method.invoke(quizService, "action");

        assertTrue(result.contains("acsion"), "Should contain 'acsion' (tion -> sion)");
    }

    @Test
    void testSmartMisspellings_DoubleLetters() throws Exception {
        Method method = QuizService.class.getDeclaredMethod("generateSmartMisspellings", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) method.invoke(quizService, "success");

        // success -> sucess (remove double)
        assertTrue(result.contains("sucess"), "Should contain 'sucess' (double s removal)");
    }

    @Test
    void testSmartMisspellings_Vowels() throws Exception {
        Method method = QuizService.class.getDeclaredMethod("generateSmartMisspellings", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) method.invoke(quizService, "receive");

        // receive -> recieve (ei -> ie)
        assertTrue(result.contains("recieve"), "Should contain 'recieve' (ei -> ie)");
    }
}
