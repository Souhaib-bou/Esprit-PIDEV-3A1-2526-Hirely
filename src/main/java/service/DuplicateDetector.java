package service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DuplicateDetector {
    private static final int MAX_POST_CACHE = 600;
    private static final int MAX_COMMENT_CACHE = 900;

    private final LinkedHashMap<String, double[]> recentPostEmbeddings = createLruMap(MAX_POST_CACHE);
    private final LinkedHashMap<String, double[]> recentCommentEmbeddings = createLruMap(MAX_COMMENT_CACHE);

    public synchronized DuplicateResult evaluateAndRemember(String type, String textKey, double[] embedding) {
        LinkedHashMap<String, double[]> cache = "comment".equalsIgnoreCase(type)
                ? recentCommentEmbeddings
                : recentPostEmbeddings;

        double maxSimilarity = 0.0;
        for (double[] existing : cache.values()) {
            maxSimilarity = Math.max(maxSimilarity, cosine(embedding, existing));
        }

        List<String> reasons = new ArrayList<>();
        if (maxSimilarity >= 0.93) {
            reasons.add(String.format("Near-duplicate content detected (%.2f similarity)", maxSimilarity));
        } else if (maxSimilarity >= 0.88) {
            reasons.add(String.format("Possible duplicate detected (%.2f similarity)", maxSimilarity));
        }

        String safeKey = textKey == null || textKey.isBlank()
                ? "k-" + System.nanoTime()
                : textKey + "-" + System.nanoTime();
        cache.put(safeKey, embedding);
        return new DuplicateResult(maxSimilarity, reasons);
    }

    private LinkedHashMap<String, double[]> createLruMap(final int maxSize) {
        return new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, double[]> eldest) {
                return size() > maxSize;
            }
        };
    }

    private double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static final class DuplicateResult {
        private final double duplicateSimilarity;
        private final List<String> reasons;

        public DuplicateResult(double duplicateSimilarity, List<String> reasons) {
            this.duplicateSimilarity = duplicateSimilarity;
            this.reasons = reasons;
        }

        public double getDuplicateSimilarity() {
            return duplicateSimilarity;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
