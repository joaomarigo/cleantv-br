package com.gathin.tvhome;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Local app search for the All apps screen: case- and accent-insensitive, so "globo" finds "Globoplay" and
 * "musica" finds "Música". Each word of the query must appear in the name, or the package name for people who
 * type that.
 */
final class AppSearch {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private AppSearch() { }

    /** Lower case, accents removed, runs of spaces folded: the form both sides are compared in. */
    static String normalize(String text) {
        if (text == null) return "";
        String plain = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return SPACES.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** Whether the app matches the query; an empty query matches everything. */
    static boolean matches(String query, String label, String pkg) {
        String q = normalize(query);
        if (q.isEmpty()) return true;
        String name = normalize(label);
        String id = pkg == null ? "" : pkg.toLowerCase(Locale.ROOT);
        for (String word : q.split(" ")) {
            if (!name.contains(word) && !id.contains(word)) return false;
        }
        return true;
    }

    /** Names that start with the query rank before names that only contain it. */
    static int compareRelevance(String query, String labelA, String labelB) {
        String q = normalize(query);
        if (q.isEmpty()) return 0;
        boolean a = normalize(labelA).startsWith(q), b = normalize(labelB).startsWith(q);
        return a == b ? 0 : a ? -1 : 1;
    }
}
