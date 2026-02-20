package me.girish.spellingbeeapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DictionaryServiceTest {

    @InjectMocks
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSmartMask_ExactMatch() throws Exception {
        Method method = DictionaryService.class.getDeclaredMethod("smartMask", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(dictionaryService, "A quick brown fox", "fox");
        assertEquals("A quick brown f*x", result, "Should mask 'fox' as 'f*x'");
    }

    @Test
    void testSmartMask_CaseInsensitive() throws Exception {
        Method method = DictionaryService.class.getDeclaredMethod("smartMask", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(dictionaryService, "The Fox jumped over", "fox");
        assertEquals("The F*x jumped over", result, "Should mask 'Fox' as 'F*x' regardless of case");
    }

    @Test
    void testSmartMask_LongWord() throws Exception {
        Method method = DictionaryService.class.getDeclaredMethod("smartMask", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(dictionaryService, "This is a definition", "definition");
        assertEquals("This is a d********n", result, "Should mask 'definition' as 'd********n'");
    }

    @Test
    void testMaskWord_RootWord() throws Exception {
        // Also test the higher-level maskWord method to ensure it catches roots
        Method method = DictionaryService.class.getDeclaredMethod("maskWord", String.class, String.class);
        method.setAccessible(true);

        // "jumping" -> "jump" (simple suffix removal works here)
        String result = (String) method.invoke(dictionaryService, "To jump high", "jumping");
        assertEquals("To j**p high", result, "Should mask root word 'jump' from 'jumping'");
    }
}
