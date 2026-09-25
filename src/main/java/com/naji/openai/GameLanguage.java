package com.naji.openai;

import java.util.List;

public final class GameLanguage {

    public static final String ENGLISH = "en";
    public static final String ARABIC = "ar";
    public static final List<String> SUPPORTED = List.of(ENGLISH, ARABIC);

    private static final List<String> ARABIC_THEME_LABELS =
            List.of("العالم الحقيقي", "فكاهة", "خيال", "غرائب", "الختام");

    private GameLanguage() {
    }

    public static String normalize(String lang) {
        if (lang == null) {
            return ENGLISH;
        }
        String trimmed = lang.trim().toLowerCase();
        return SUPPORTED.contains(trimmed) ? trimmed : ENGLISH;
    }

    public static boolean isArabic(String lang) {
        return ARABIC.equals(normalize(lang));
    }

    public static String themeLabel(String lang, int slot, ScenarioTheme englishTheme) {
        if (!isArabic(lang)) {
            return englishTheme.label();
        }
        int index = Math.min(Math.max(slot, 1), ARABIC_THEME_LABELS.size()) - 1;
        return ARABIC_THEME_LABELS.get(index);
    }
}
