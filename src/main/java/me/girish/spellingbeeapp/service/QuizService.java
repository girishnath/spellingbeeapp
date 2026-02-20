package me.girish.spellingbeeapp.service;

import me.girish.spellingbeeapp.dto.SubmissionDTO;
import me.girish.spellingbeeapp.model.*;
import me.girish.spellingbeeapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuizService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private TestResultRepository testResultRepository;
    @Autowired
    private MistakeRepository mistakeRepository;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private GeminiService geminiService;

    public Student registerStudent(String name) {
        String id = name.trim().toLowerCase().replace(" ", "");
        return studentRepository.save(new Student(id, name));
    }

    public List<Question> generateQuiz(String studentId, String mode) {
        List<String> allWords = loadAllWords();
        Collections.shuffle(allWords);

        List<Question> quizQuestions = new ArrayList<>();

        boolean isSpellingOnly = mode.contains("SPELLING_ONLY");
        boolean isQuick = mode.contains("QUICK") || "QUICK".equalsIgnoreCase(mode);
        int totalQuestions = isQuick ? 10 : 45;

        int spellingCount;
        int vocabCount;

        if (isSpellingOnly) {
            spellingCount = totalQuestions;
            vocabCount = 0;
        } else {
            spellingCount = isQuick ? 6 : 25;
            vocabCount = isQuick ? 4 : 20;
        }

        // Spelling Questions
        int wIdx = 0;
        for (int i = 0; i < spellingCount && wIdx < allWords.size(); wIdx++) {
            String word = allWords.get(wIdx);
            quizQuestions.add(createSpellingQuestion(word));
            i++;
        }

        // Vocabulary Questions
        for (int i = 0; i < vocabCount && wIdx < allWords.size(); wIdx++) {
            String word = allWords.get(wIdx);
            Question vocabQ = null;

            // Try Gemini First
            if (geminiService.isEnabled()) {
                Optional<GeminiService.VocabData> aiData = geminiService.generateVocabQuestion(word);
                if (aiData.isPresent()) {
                    vocabQ = createVocabQuestion(word, aiData.get().definition, aiData.get().distractors);
                }
            }

            // Fallback to Dictionary if Gemini failed or disabled
            if (vocabQ == null) {
                // For correct answer, we DO want masking if the word appears
                String def = dictionaryService.getDefinition(word, true).orElse(null);
                if (def != null && !def.toLowerCase().contains(word.toLowerCase())) { // Basic filter
                    // Get distractors
                    List<String> distractors = new ArrayList<>();
                    int attempts = 0;
                    while (distractors.size() < 3 && attempts < 10) {
                        int randIdx = new Random().nextInt(allWords.size());
                        String distractorWord = allWords.get(randIdx);
                        if (!distractorWord.equalsIgnoreCase(word)) {
                            // For distractors, we DO NOT want to mask the distractor word in its own
                            // definition
                            // This avoids giving away that it's NOT the target word by seeing asterisks for
                            // a different word
                            dictionaryService.getDefinition(distractorWord, false).ifPresent(d -> {
                                if (!d.toLowerCase().contains(word.toLowerCase()))
                                    distractors.add(d);
                            });
                        }
                        attempts++;
                    }

                    if (distractors.size() >= 3) {
                        vocabQ = createVocabQuestion(word, def, distractors);
                    }
                }
            }

            // If we have a valid vocab question, add it. Otherwise, generate a spelling
            // question for this word.
            if (vocabQ != null) {
                quizQuestions.add(vocabQ);
                i++;
            } else {
                quizQuestions.add(createSpellingQuestion(word));
                // We incremented wIdx but treat this as a "vocab slot filled with spelling" or
                // just skip to next requirement?
                // The requirement is specific counts. Converting to spelling means we have more
                // spelling than expected.
                // Let's just treat it as a valid question added to the list, effectively
                // increasing spelling count.
                // BUT, to stick to the loop logic for `vocabCount`, we should increment `i`
                // ONLY if we added a question.
                // Here we added a spelling question instead. So we technically filled a 'slot'.
                i++;
            }
        }

        try {
            List<Question> saved = questionRepository.saveAll(quizQuestions);
            questionRepository.flush();
            return saved;
        } catch (Throwable e) {
            System.err.println("ERROR SAVING QUIZ: " + e.getMessage());
            e.printStackTrace(); // Print stack trace to stderr (app.log)
            throw e;
        }
    }

    public TestResult submitQuiz(SubmissionDTO submission) {
        Student student = studentRepository.findById(submission.getStudentId()).orElseThrow();
        int score = 0;
        int total = submission.getAnswers().size();

        for (SubmissionDTO.AnswerDTO ans : submission.getAnswers()) {
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
            if (q != null) {
                if (q.getCorrectAnswer().equals(ans.getSelectedOption())) {
                    score++;
                } else {
                    Mistake m = new Mistake();
                    m.setStudent(student);
                    m.setWord(q.getWord());
                    m.setUserAnswer(ans.getSelectedOption());
                    m.setCorrectAnswer(q.getCorrectAnswer());
                    m.setType(q.getType());
                    m.setTimestamp(LocalDateTime.now());
                    mistakeRepository.save(m);
                }
            }
        }

        TestResult result = new TestResult();
        result.setStudent(student);
        result.setScore(score);
        // Use totalQuestions sent from frontend, or default to answers size if 0
        // (backward compatibility)
        result.setTotalQuestions(submission.getTotalQuestions() > 0 ? submission.getTotalQuestions() : total);
        result.setTimestamp(LocalDateTime.now());
        return testResultRepository.save(result);
    }

    private Map<String, List<String>> wordCategories = new HashMap<>();

    public List<Question> getStudyWords(String category, String letter) {
        if (wordCategories.isEmpty()) {
            loadAllWords();
        }

        List<String> words = wordCategories.getOrDefault(category, new ArrayList<>());

        List<Question> questions = new ArrayList<>();
        for (String word : words) {
            String w = word.trim();
            if (w.isEmpty())
                continue;

            if ("ALL".equalsIgnoreCase(letter) || w.toUpperCase().startsWith(letter.toUpperCase())) {
                Question q = new Question();
                q.setWord(w);
                q.setType(Question.QuestionType.SPELLING); // Reuse spelling type for study
                q.setQuestionText("Spell the word");
                q.setCorrectAnswer(w);
                // Options not needed for study mode as it's input based, but we can set correct
                // one
                q.setOptions(Collections.singletonList(w));
                questions.add(q);
            }
        }
        if ("ALL".equalsIgnoreCase(letter)) {
            Collections.shuffle(questions);
        }

        return questions;
    }

    private synchronized List<String> loadAllWords() {
        if (!wordCategories.isEmpty()) {
            List<String> all = new ArrayList<>();
            wordCategories.values().forEach(all::addAll);
            return all;
        }

        Map<String, String> fileMap = new HashMap<>();
        fileMap.put("ONE_BEE", "words/onebee.txt");
        fileMap.put("TWO_BEE", "words/twobee.txt");
        fileMap.put("THREE_BEE", "words/threebee.txt");
        fileMap.put("NEW_WORDS", "words/external.txt");

        List<String> allWords = new ArrayList<>();

        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            List<String> categoryWords = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new ClassPathResource(entry.getValue()).getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        String w = line.trim();
                        categoryWords.add(w);
                        allWords.add(w);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            wordCategories.put(entry.getKey(), categoryWords);
        }
        return allWords;
    }

    private Question createSpellingQuestion(String word) {
        Question q = new Question();
        q.setWord(word);
        q.setType(Question.QuestionType.SPELLING);
        q.setQuestionText("Select the correct spelling:");

        Set<String> opts = new LinkedHashSet<>();
        String lowerWord = word.toLowerCase();
        opts.add(lowerWord); // Correct

        // 1. Try Smart Misspellings
        Set<String> smart = generateSmartMisspellings(lowerWord);
        List<String> smartList = new ArrayList<>(smart);
        Collections.shuffle(smartList);
        for (String s : smartList) {
            if (opts.size() >= 4)
                break;
            opts.add(s);
        }

        // 2. Fallback if needed
        int attempts = 0;
        while (opts.size() < 4 && attempts < 20) {
            String mis = generateRandomMisspelling(lowerWord);
            if (!mis.equalsIgnoreCase(lowerWord)) {
                opts.add(mis);
            }
            attempts++;
        }

        // If still not enough, force simple variations
        while (opts.size() < 4) {
            opts.add(lowerWord + (opts.size()));
        }

        List<String> finalOpts = new ArrayList<>(opts);
        Collections.shuffle(finalOpts);

        q.setOptions(finalOpts);
        q.setCorrectAnswer(lowerWord); // Ensure exact match with option
        return q;
    }

    private Question createVocabQuestion(String word, String def, List<String> distractors) {
        // Validate inputs
        if (def == null || def.trim().isEmpty() || distractors == null || distractors.size() < 3) {
            return null; // Invalid question
        }

        Question q = new Question();
        q.setWord(word);
        q.setType(Question.QuestionType.VOCABULARY);
        q.setQuestionText("What is the definition of '" + word + "'?");

        List<String> opts = new ArrayList<>();
        opts.add(def.length() > 999 ? def.substring(0, 999) : def);
        for (String d : distractors.subList(0, 3)) {
            opts.add(d.length() > 999 ? d.substring(0, 999) : d);
        }
        Collections.shuffle(opts);

        q.setOptions(opts);
        q.setCorrectAnswer(def);
        return q;
    }

    private Set<String> generateSmartMisspellings(String word) {
        Set<String> misspellings = new HashSet<>();
        String lower = word.toLowerCase();

        // 1. Common Suffixes
        if (lower.endsWith("tion"))
            misspellings.add(replaceSuffix(lower, "tion", "sion"));
        if (lower.endsWith("sion"))
            misspellings.add(replaceSuffix(lower, "sion", "tion"));
        if (lower.endsWith("ence"))
            misspellings.add(replaceSuffix(lower, "ence", "ance"));
        if (lower.endsWith("ance"))
            misspellings.add(replaceSuffix(lower, "ance", "ence"));
        if (lower.endsWith("ent"))
            misspellings.add(replaceSuffix(lower, "ent", "ant"));
        if (lower.endsWith("ant"))
            misspellings.add(replaceSuffix(lower, "ant", "ent"));
        if (lower.endsWith("ible"))
            misspellings.add(replaceSuffix(lower, "ible", "able"));
        if (lower.endsWith("able"))
            misspellings.add(replaceSuffix(lower, "able", "ible"));
        if (lower.endsWith("ary"))
            misspellings.add(replaceSuffix(lower, "ary", "ery"));
        if (lower.endsWith("ery"))
            misspellings.add(replaceSuffix(lower, "ery", "ary"));
        if (lower.endsWith("ly"))
            misspellings.add(replaceSuffix(lower, "ly", "ley"));

        // 2. Phonetic Replacements (Regex for global replacement)
        // ph -> f
        if (lower.contains("ph"))
            misspellings.add(lower.replace("ph", "f"));
        else if (lower.contains("f"))
            misspellings.add(lower.replace("f", "ph"));

        // ch -> k or sh (only if hard sound, but hard to know) -> let's try 'k' if it
        // looks Greek
        if (lower.contains("ch")) {
            misspellings.add(lower.replace("ch", "k"));
            misspellings.add(lower.replace("ch", "sh")); // chef
        }
        if (lower.contains("k"))
            misspellings.add(lower.replace("k", "c")); // cat
        if (lower.contains("c") && !lower.contains("ch")) {
            misspellings.add(lower.replace("c", "k"));
            misspellings.add(lower.replace("c", "s"));
        }

        // z -> s
        if (lower.contains("z"))
            misspellings.add(lower.replace("z", "s"));
        else if (lower.contains("s") && !lower.contains("ss") && !lower.contains("sh"))
            misspellings.add(lower.replace("s", "z"));

        // j -> g / g -> j
        if (lower.contains("j"))
            misspellings.add(lower.replace("j", "g"));
        else if (lower.contains("g") && !lower.endsWith("ing"))
            misspellings.add(lower.replace("g", "j")); // giant

        // 3. Vowels
        if (lower.contains("ie"))
            misspellings.add(lower.replace("ie", "ei"));
        else if (lower.contains("ei"))
            misspellings.add(lower.replace("ei", "ie"));

        if (lower.contains("ai"))
            misspellings.add(lower.replace("ai", "ay"));
        else if (lower.contains("ay"))
            misspellings.add(lower.replace("ay", "ai"));

        if (lower.contains("ea"))
            misspellings.add(lower.replace("ea", "ee"));
        else if (lower.contains("ee"))
            misspellings.add(lower.replace("ee", "ea"));

        if (lower.contains("ou"))
            misspellings.add(lower.replace("ou", "u"));

        // 4. Double Letters
        for (int i = 0; i < lower.length() - 1; i++) {
            if (lower.charAt(i) == lower.charAt(i + 1)) {
                // Remove one
                misspellings.add(lower.substring(0, i) + lower.substring(i + 1));
            } else {
                // Double it (naive, but works for generating options)
                // limit to consonants to avoid 'aa', 'ii' etc which are rare
                char c = lower.charAt(i);
                if ("bdfgklmnprstz".indexOf(c) >= 0) {
                    misspellings.add(lower.substring(0, i + 1) + c + lower.substring(i + 1));
                }
            }
        }

        // Remove originals and empty strings
        misspellings.remove(lower);
        misspellings.remove("");
        return misspellings;
    }

    private String replaceSuffix(String word, String oldSuffix, String newSuffix) {
        return word.substring(0, word.length() - oldSuffix.length()) + newSuffix;
    }

    // Fallback for when smart rules don't generate enough options
    private String generateRandomMisspelling(String word) {
        StringBuilder sb = new StringBuilder(word.toLowerCase());
        Random r = new Random();
        int idx = r.nextInt(sb.length());
        char c = sb.charAt(idx);

        // Vowel swap
        if ("aeiou".indexOf(c) >= 0) {
            String vowels = "aeiou";
            char newC = vowels.charAt(r.nextInt(vowels.length()));
            while (newC == c && vowels.length() > 1) { // ensure change
                newC = vowels.charAt(r.nextInt(vowels.length()));
            }
            sb.setCharAt(idx, newC);
        } else {
            // Consonant swap or double
            if (r.nextBoolean() && idx < sb.length() - 1) {
                // Swap neighbors
                char next = sb.charAt(idx + 1);
                sb.setCharAt(idx, next);
                sb.setCharAt(idx + 1, c);
            } else {
                // Replace with common similar looking/sounding
                if (c == 'm')
                    sb.setCharAt(idx, 'n');
                else if (c == 'n')
                    sb.setCharAt(idx, 'm');
                else if (c == 't')
                    sb.setCharAt(idx, 'd');
                else if (c == 'd')
                    sb.setCharAt(idx, 't');
                else
                    sb.setCharAt(idx, (char) ('a' + r.nextInt(26))); // absolute fallback
            }
        }
        return sb.toString();
    }

    public List<Mistake> getMistakes(String studentId) {
        return mistakeRepository.findByStudentId(studentId);
    }
}
