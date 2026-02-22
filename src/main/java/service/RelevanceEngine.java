package service;

import util.PythonEmbeddingClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RelevanceEngine {
    private final PythonEmbeddingClient embeddingClient;
    private final Map<String, String> careerAnchors = new LinkedHashMap<>();
    private final Map<String, String> offTopicAnchors = new LinkedHashMap<>();
    private volatile boolean initialized = false;
    private final Map<String, double[]> careerAnchorEmbeddings = new LinkedHashMap<>();
    private final Map<String, double[]> offTopicAnchorEmbeddings = new LinkedHashMap<>();

    public RelevanceEngine(PythonEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
        careerAnchors.put("Internship", "internship opportunity");
        careerAnchors.put("Job Offer", "job opening hiring");
        careerAnchors.put("Interview", "interview preparation");
        careerAnchors.put("Resume", "resume review cv feedback");
        careerAnchors.put("Salary", "salary negotiation compensation");
        careerAnchors.put("Career Advice", "career advice and growth");
        careerAnchors.put("Applications", "application tips for jobs");

        offTopicAnchors.put("Gaming", "gaming and video games");
        offTopicAnchors.put("Memes", "internet memes and jokes");
        offTopicAnchors.put("Politics", "political debate and elections");
        offTopicAnchors.put("Dating", "dating and relationships");
        offTopicAnchors.put("Random Chat", "random casual chat");
    }

    public RelevanceResult evaluate(double[] embedding, String contentType) throws Exception {
        ensureAnchorsInitialized();
        String type = contentType == null ? "post" : contentType.toLowerCase();

        double bestCareer = -1.0;
        String bestCategory = "General";
        for (Map.Entry<String, double[]> e : careerAnchorEmbeddings.entrySet()) {
            double sim = cosine(embedding, e.getValue());
            if (sim > bestCareer) {
                bestCareer = sim;
                bestCategory = e.getKey();
            }
        }

        double bestOffTopic = -1.0;
        String offTopicCategory = "";
        for (Map.Entry<String, double[]> e : offTopicAnchorEmbeddings.entrySet()) {
            double sim = cosine(embedding, e.getValue());
            if (sim > bestOffTopic) {
                bestOffTopic = sim;
                offTopicCategory = e.getKey();
            }
        }

        double relevance = clamp01(bestCareer);
        List<String> reasons = new ArrayList<>();

        if (bestOffTopic > bestCareer) {
            double penalty = Math.min(0.35, (bestOffTopic - bestCareer) * 0.60);
            relevance = clamp01(relevance - penalty);
            reasons.add(String.format("Off-topic signal (%s) reduced relevance (%.2f)", offTopicCategory, penalty));
        }

        if ("comment".equals(type) && relevance < 0.35 && bestCareer > 0.35) {
            relevance = clamp01(relevance + 0.08);
            reasons.add("Comment context adjustment applied");
        }

        return new RelevanceResult(relevance, bestCategory, bestCareer, bestOffTopic, reasons);
    }

    private void ensureAnchorsInitialized() throws Exception {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            for (Map.Entry<String, String> e : careerAnchors.entrySet()) {
                double[] vector = embeddingClient.embed(e.getValue(), "post").getEmbedding();
                careerAnchorEmbeddings.put(e.getKey(), vector);
            }
            for (Map.Entry<String, String> e : offTopicAnchors.entrySet()) {
                double[] vector = embeddingClient.embed(e.getValue(), "post").getEmbedding();
                offTopicAnchorEmbeddings.put(e.getKey(), vector);
            }
            initialized = true;
        }
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

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public static final class RelevanceResult {
        private final double relevance;
        private final String category;
        private final double maxCareerSimilarity;
        private final double offTopicSimilarity;
        private final List<String> reasons;

        public RelevanceResult(double relevance, String category, double maxCareerSimilarity, double offTopicSimilarity,
                List<String> reasons) {
            this.relevance = relevance;
            this.category = category;
            this.maxCareerSimilarity = maxCareerSimilarity;
            this.offTopicSimilarity = offTopicSimilarity;
            this.reasons = reasons;
        }

        public double getRelevance() {
            return relevance;
        }

        public String getCategory() {
            return category;
        }

        public double getMaxCareerSimilarity() {
            return maxCareerSimilarity;
        }

        public double getOffTopicSimilarity() {
            return offTopicSimilarity;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
