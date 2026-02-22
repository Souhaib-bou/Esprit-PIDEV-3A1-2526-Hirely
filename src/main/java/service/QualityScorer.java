package service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QualityScorer {
    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{3,}");
    private static final Pattern REPEATED_PUNCT_PATTERN = Pattern.compile("[!?.,]{4,}");

    public QualityResult score(String text, String type) {
        String value = text == null ? "" : text.trim();
        String contentType = type == null ? "post" : type.toLowerCase(Locale.ROOT);

        List<String> reasons = new ArrayList<>();
        double score = 1.0;

        int minLength = "comment".equals(contentType) ? 12 : 30;
        int idealLength = "comment".equals(contentType) ? 80 : 180;

        int len = value.length();
        if (len < minLength) {
            score -= 0.40;
            reasons.add("Too short");
        } else if (len < idealLength) {
            score -= 0.15;
        }

        int linkCount = countLinks(value);
        if (linkCount >= 3) {
            score -= 0.25;
            reasons.add("Too many links");
        } else if (linkCount == 2) {
            score -= 0.15;
            reasons.add("High link density");
        }

        double capsRatio = capsRatio(value);
        if (capsRatio > 0.70) {
            score -= 0.25;
            reasons.add("Excessive all-caps");
        } else if (capsRatio > 0.45) {
            score -= 0.10;
        }

        if (REPEATED_CHAR_PATTERN.matcher(value).find()) {
            score -= 0.12;
            reasons.add("Repeated characters detected");
        }
        if (REPEATED_PUNCT_PATTERN.matcher(value).find()) {
            score -= 0.12;
            reasons.add("Repeated punctuation detected");
        }

        double diversity = tokenDiversity(value);
        if (diversity < 0.35) {
            score -= 0.20;
            reasons.add("Low word diversity");
        } else if (diversity < 0.50) {
            score -= 0.08;
        }

        double finalScore = clamp01(score);
        if (finalScore >= 0.80) {
            reasons.add("Strong content quality");
        }
        return new QualityResult(finalScore, reasons);
    }

    private int countLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private double capsRatio(String text) {
        int letters = 0;
        int uppercase = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    uppercase++;
                }
            }
        }
        if (letters == 0) {
            return 0.0;
        }
        return (double) uppercase / letters;
    }

    private double tokenDiversity(String text) {
        String[] raw = text.toLowerCase(Locale.ROOT).split("\\s+");
        int tokenCount = 0;
        Set<String> unique = new HashSet<>();
        for (String token : raw) {
            String t = token.replaceAll("[^a-z0-9]", "");
            if (t.isBlank()) {
                continue;
            }
            tokenCount++;
            unique.add(t);
        }
        if (tokenCount == 0) {
            return 0.0;
        }
        return (double) unique.size() / tokenCount;
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public static final class QualityResult {
        private final double qualityScore;
        private final List<String> reasons;

        public QualityResult(double qualityScore, List<String> reasons) {
            this.qualityScore = qualityScore;
            this.reasons = reasons;
        }

        public double getQualityScore() {
            return qualityScore;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
