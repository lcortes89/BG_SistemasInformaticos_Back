package com.bgsistemas.api.globals;

import java.util.Locale;

// BGSI-302: the text of the search box of the admin lists, ready for "LOWER(column) LIKE :q ESCAPE '!'".
// "!" is the escape character because a backslash means something else in MySQL strings
public final class SearchText {

    private static final int MAX_LENGTH = 100;

    private SearchText() {
    }

    // "  Bar_1 " -> "%bar!_1%". Empty -> null, which means "no filter"
    public static String likePattern(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.trim().toLowerCase(Locale.ROOT);
        if (clean.length() > MAX_LENGTH) {
            clean = clean.substring(0, MAX_LENGTH);
        }
        String escaped = clean.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
