package me.girish.spellingbeeapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@Service
public class DictionaryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DICT_API = "https://api.dictionaryapi.dev/api/v2/entries/en/";
    private static final String DATAMUSE_API = "https://api.datamuse.com/words?sp=%s&md=d&max=1";

    public Optional<String> getDefinition(String word) {
        return getDefinition(word, true);
    }

    public Optional<String> getDefinition(String word, boolean maskSelf) {
        String dirtyDefinition = null;

        // 1. Try Dictionary API
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(DICT_API + word, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray() && root.size() > 0) {
                    JsonNode definitions = root.get(0).path("meanings").get(0).path("definitions");
                    if (definitions.isArray()) {
                        for (JsonNode defNode : definitions) {
                            String def = defNode.path("definition").asText();
                            if (def != null && !def.isEmpty()) {
                                if (!containsWord(def, word)) {
                                    return Optional.of(def); // Found clean definition
                                } else if (dirtyDefinition == null) {
                                    dirtyDefinition = def; // Keep as fallback
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but continue to fallback
            System.err.println("Error fetching definition for " + word + ": " + e.getMessage());
        }

        // 2. Fallback to Datamuse API
        Optional<String> datamuseDef = getDatamuseDefinition(word);
        if (datamuseDef.isPresent()) {
            String def = datamuseDef.get();
            if (!containsWord(def, word)) {
                return Optional.of(def); // Clean fallback found
            }
            if (dirtyDefinition == null) {
                dirtyDefinition = def;
            }
        }

        // 3. If we only have a dirty definition
        if (dirtyDefinition != null) {
            if (maskSelf) {
                return Optional.of(maskWord(dirtyDefinition, word));
            } else {
                return Optional.of(dirtyDefinition);
            }
        }

        return Optional.empty();
    }

    private Optional<String> getDatamuseDefinition(String word) {
        try {
            String url = String.format(DATAMUSE_API, word);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        // Datamuse format: [{"word":"foo","score":100,"defs":["def1","def2"]}]
                        if (node.has("defs")) {
                            JsonNode defs = node.get("defs");
                            for (JsonNode d : defs) {
                                String defText = d.asText();
                                // Datamuse definitions often start with "n\t", "v\t" etc. strip them
                                if (defText.contains("\t")) {
                                    defText = defText.substring(defText.indexOf("\t") + 1);
                                }
                                return Optional.of(defText);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching definition from Datamuse for " + word + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private boolean containsWord(String definition, String word) {
        String defLower = definition.toLowerCase();
        String wordLower = word.toLowerCase();

        // Check exact word match
        if (defLower.contains(wordLower))
            return true;

        // Check root word (e.g., "harrow" from "harrowing")
        String root = getRootWord(word);
        if (root != null && defLower.contains(root.toLowerCase())) {
            return true;
        }

        // Check for significant substrings (e.g., "hermit" in "hermitage")
        // Only check if word is long enough (6+ chars) to avoid false positives
        if (word.length() >= 6) {
            // Check if any substring of 5+ chars from the target word appears
            for (int len = Math.min(word.length() - 1, word.length()); len >= 5; len--) {
                for (int i = 0; i <= word.length() - len; i++) {
                    String substring = wordLower.substring(i, i + len);
                    // Use word boundary regex to avoid matching inside other words
                    if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(substring) + "\\b",
                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(definition).find()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String maskWord(String definition, String word) {
        String masked = definition;

        // Mask the exact word
        masked = smartMask(masked, word);

        // Mask root word
        String root = getRootWord(word);
        if (root != null && !root.equalsIgnoreCase(word)) {
            masked = smartMask(masked, root);
        }

        // Mask significant substrings (e.g., "hermit" in "hermitage")
        if (word.length() >= 6) {
            for (int len = Math.min(word.length() - 1, word.length()); len >= 5; len--) {
                for (int i = 0; i <= word.length() - len; i++) {
                    String substring = word.substring(i, i + len);
                    // Only mask if it appears as a whole word in the definition
                    if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(substring) + "\\b",
                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(masked).find()) {
                        masked = smartMask(masked, substring);
                    }
                }
            }
        }

        return masked;
    }

    private String getRootWord(String word) {
        if (word == null || word.length() <= 4)
            return null;
        String w = word.toLowerCase();
        if (w.endsWith("ing"))
            return word.substring(0, word.length() - 3);
        if (w.endsWith("ed"))
            return word.substring(0, word.length() - 2);
        if (w.endsWith("s") && !w.endsWith("ss"))
            return word.substring(0, word.length() - 1);
        if (w.endsWith("es"))
            return word.substring(0, word.length() - 2);
        if (w.endsWith("ly"))
            return word.substring(0, word.length() - 2);
        if (w.endsWith("er"))
            return word.substring(0, word.length() - 2);
        if (w.endsWith("est"))
            return word.substring(0, word.length() - 3);
        if (w.endsWith("ful"))
            return word.substring(0, word.length() - 3);
        if (w.endsWith("ment"))
            return word.substring(0, word.length() - 4);
        if (w.endsWith("ness"))
            return word.substring(0, word.length() - 4);
        return null; // No common suffix found
    }

    private String smartMask(String text, String target) {
        // Regex to find the target word (case insensitive)
        // We use word boundaries \b to avoid masking inside other words (e.g. 'cat' in
        // 'catch')
        // unless the match is very long? No, safer to match whole words or checks.
        // Actually, for "harrow" in "harrowing", we are running this ON the definition.
        // Definition: "To drag a harrow over" -> target "harrow" matches partial? No
        // matches whole word "harrow".

        String regex = "(?i)\\b" + java.util.regex.Pattern.quote(target) + "\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String replacement;
            if (match.length() <= 2) {
                replacement = "*".repeat(match.length());
            } else {
                replacement = match.charAt(0) + "*".repeat(match.length() - 2) + match.charAt(match.length() - 1);
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
