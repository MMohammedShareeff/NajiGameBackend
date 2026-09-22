package com.naji.openai;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsonResponseMapper {

    public static final String SURVIVED = "Survived";
    public static final String NOT_SURVIVED = "Not Survived";
    public static final String UNKNOWN = "Unknown";
    private static final String NUMBER = "(?<![\\d.])(\\d{1,2}(?:\\.\\d+)?)";
    private static final Pattern MARKED_RATING =
            Pattern.compile("(?i)rating\\s*[:=]?\\s*" + NUMBER + "\\s*/\\s*10(?!\\d)");
    private static final Pattern ANY_RATING = Pattern.compile(
            "(?i)" + NUMBER + "\\s*(?:/\\s*10(?!\\d)|out\\s+of\\s+10(?!\\d))");
    private static final Pattern MARKED_RESULT =
            Pattern.compile("(?i)result\\s*[:=]\\s*\\W*(not\\s+survived|survived)");
    private static final Pattern ANY_VERDICT = Pattern.compile(
            "(?i)\\b(not\\s+survived|did\\s+not\\s+survive|didn'?t\\s+survive|survived)\\b");

    public String extractStatus(String answer) {
        if (answer == null || answer.isBlank()) {
            return UNKNOWN;
        }
        String verdict = lastGroup(MARKED_RESULT, answer);
        if (verdict == null) {
            verdict = lastGroup(ANY_VERDICT, answer);
        }
        if (verdict == null) {
            return UNKNOWN;
        }
        String v = verdict.toLowerCase();
        return v.startsWith("not") || v.startsWith("did") ? NOT_SURVIVED : SURVIVED;
    }

    public int extractRating(String answer) {
        if (answer == null || answer.isBlank()) {
            return -1;
        }
        String number = lastGroup(MARKED_RATING, answer);
        if (number == null) {
            number = lastGroup(ANY_RATING, answer);
        }
        if (number == null) {
            return -1;
        }
        long rating = Math.round(Double.parseDouble(number));
        return (int) Math.max(0, Math.min(10, rating));
    }

    private static String lastGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }
}
