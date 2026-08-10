package com.lorafilm.movie.cinema.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGES = Pattern.compile("(^-+)|(-+$)");

    private SlugUtils() {
        // Prevent instantiation
    }

    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        // Replace white spaces with dashes
        String step1 = WHITESPACE.matcher(input.trim()).replaceAll("-");

        // Decompose unicode characters (separates base characters from diacritical marks)
        String step2 = Normalizer.normalize(step1, Normalizer.Form.NFD);

        // Remove diacritical marks
        String step3 = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(step2).replaceAll("");

        // Handle specific Vietnamese character cases ('đ', 'Đ')
        String step4 = step3.replace("đ", "d").replace("Đ", "d");

        // Keep only alphanumeric and dashes
        String step5 = NONLATIN.matcher(step4).replaceAll("");

        // Strip leading/trailing dashes and convert to lowercase
        String step6 = EDGES.matcher(step5).replaceAll("");

        return step6.toLowerCase(Locale.ENGLISH);
    }
}
