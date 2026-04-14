package com.makerworld.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssertionUtils {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[\\d,.]*)(\\s*[%kKmM]?)");

    private AssertionUtils() {
    }

    public static String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    public static String normalizeForComparison(String value) {
        String normalized = Normalizer.normalize(normalizeWhitespace(value), Normalizer.Form.NFKD)
            .replaceAll("[^\\p{Alnum} ]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
        return normalized;
    }

    public static boolean normalizedContains(String haystack, String needle) {
        String normalizedHaystack = normalizeForComparison(haystack);
        String normalizedNeedle = normalizeForComparison(needle);
        return !normalizedNeedle.isBlank() && normalizedHaystack.contains(normalizedNeedle);
    }

    public static boolean looksLikeModelDetailUrl(String url) {
        return url != null && url.matches(".*/models/\\d+.*");
    }

    public static boolean looksLikeContestDetailUrl(String url) {
        return url != null && url.matches(".*/contests/[^/?#]+.*") && !url.endsWith("/contests");
    }

    public static boolean looksLikeCrowdfundingDetailUrl(String url) {
        return url != null && url.matches(".*/crowdfunding/[^/?#]+.*") && !url.endsWith("/crowdfunding");
    }

    public static int countNumericTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static String slugToken(String text) {
        String normalized = normalizeForComparison(text);
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split(" ");
        return parts[0];
    }

    public static boolean makerWorldOwnedUrl(String url) {
        return url != null && url.contains("makerworld.com");
    }
}
