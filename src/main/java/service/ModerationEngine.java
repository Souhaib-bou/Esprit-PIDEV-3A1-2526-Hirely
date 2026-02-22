package service;

import model.ModerationReport;
import util.PerspectiveClient;
import util.PythonEmbeddingClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModerationEngine {
    public enum ContentType {
        POST,
        COMMENT
    }

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";

    private final PerspectiveClient perspectiveClient;
    private final PythonEmbeddingClient pythonEmbeddingClient;
    private final RelevanceEngine relevanceEngine;
    private final DuplicateDetector duplicateDetector;
    private final QualityScorer qualityScorer;
    private final ExecutorService executor;

    public ModerationEngine() {
        this(new PerspectiveClient(), new PythonEmbeddingClient(), new DuplicateDetector(), new QualityScorer());
    }

    public ModerationEngine(PerspectiveClient perspectiveClient, PythonEmbeddingClient pythonEmbeddingClient,
            DuplicateDetector duplicateDetector, QualityScorer qualityScorer) {
        this.perspectiveClient = perspectiveClient;
        this.pythonEmbeddingClient = pythonEmbeddingClient;
        this.relevanceEngine = new RelevanceEngine(pythonEmbeddingClient);
        this.duplicateDetector = duplicateDetector;
        this.qualityScorer = qualityScorer;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<ModerationReport> analyzeAsync(ContentType type, String text) {
        String safeText = text == null ? "" : text.trim();
        return CompletableFuture.supplyAsync(() -> analyze(type, safeText), executor);
    }

    private ModerationReport analyze(ContentType type, String text) {
        long startNs = System.nanoTime();
        ModerationReport report = new ModerationReport();
        List<String> reasons = new ArrayList<>();
        report.setReasons(reasons);
        report.setCategory("General");

        String perspectiveKey = readPerspectiveApiKey();
        String contentType = toWireType(type);

        CompletableFuture<PerspectiveClient.PerspectiveResult> perspectiveFuture =
                CompletableFuture.supplyAsync(() -> callPerspective(text, perspectiveKey), executor);
        CompletableFuture<PythonEmbeddingClient.EmbeddingResult> embeddingFuture =
                CompletableFuture.supplyAsync(() -> callEmbedding(text, contentType), executor);

        PerspectiveClient.PerspectiveResult perspective = null;
        PythonEmbeddingClient.EmbeddingResult embedding = null;
        boolean fallback = false;

        try {
            perspective = perspectiveFuture.join();
            report.setToxicity(perspective.getToxicity());
            report.setPerspectiveRaw(perspective.getRaw());
            report.setPerspectiveLatencyMs(perspective.getLatencyMs());
        } catch (Exception ex) {
            fallback = true;
            report.setToxicity(0.50);
            report.setPerspectiveRaw("Perspective unavailable");
            reasons.add("Perspective API unavailable");
        }

        try {
            embedding = embeddingFuture.join();
            report.setPythonRaw("model=" + embedding.getModel() + ", dim=" + embedding.getDim() + "\n" + embedding.getRaw());
            report.setPythonLatencyMs(embedding.getLatencyMs());
        } catch (Exception ex) {
            fallback = true;
            report.setPythonRaw("Python embedding service unavailable");
            reasons.add("Python embedding service unavailable");
        }

        QualityScorer.QualityResult quality = qualityScorer.score(text, contentType);
        report.setQualityScore(quality.getQualityScore());
        reasons.addAll(quality.getReasons());

        if (embedding != null) {
            try {
                RelevanceEngine.RelevanceResult relevance = relevanceEngine.evaluate(embedding.getEmbedding(), contentType);
                report.setRelevance(relevance.getRelevance());
                report.setCategory(relevance.getCategory());
                reasons.addAll(relevance.getReasons());

                DuplicateDetector.DuplicateResult dup = duplicateDetector.evaluateAndRemember(
                        contentType,
                        cacheKey(text, type),
                        embedding.getEmbedding());
                report.setDuplicateSimilarity(dup.getDuplicateSimilarity());
                reasons.addAll(dup.getReasons());
            } catch (Exception ex) {
                fallback = true;
                report.setRelevance(0.50);
                report.setDuplicateSimilarity(0.00);
                reasons.add("Relevance/duplicate analysis unavailable");
            }
        } else {
            report.setRelevance(0.50);
            report.setDuplicateSimilarity(0.00);
        }

        if (fallback) {
            report.setDecision(STATUS_PENDING);
            reasons.add("AI unavailable - sent for manual review");
            report.setFallbackUsed(true);
        } else {
            report.setDecision(decideWithPolicy(type, report, reasons));
            report.setFallbackUsed(false);
        }

        report.setTotalLatencyMs((System.nanoTime() - startNs) / 1_000_000);
        return report;
    }

    private PerspectiveClient.PerspectiveResult callPerspective(String text, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new CompletionException(new IllegalStateException("Missing PERSPECTIVE_API_KEY"));
        }
        try {
            return perspectiveClient.analyze(text, apiKey);
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private PythonEmbeddingClient.EmbeddingResult callEmbedding(String text, String type) {
        try {
            return pythonEmbeddingClient.embed(text, type);
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private String decideWithPolicy(ContentType type, ModerationReport report, List<String> reasons) {
        double toxicity = report.getToxicity();
        double relevance = report.getRelevance();
        double quality = report.getQualityScore();
        double duplicate = report.getDuplicateSimilarity();

        if (type == ContentType.POST) {
            if (toxicity >= 0.80) {
                reasons.add("High toxicity score");
                return STATUS_REJECTED;
            }
            if (toxicity <= 0.20 && relevance >= 0.70 && quality >= 0.55 && duplicate <= 0.90) {
                reasons.add("Passed post auto-approval policy");
                return STATUS_APPROVED;
            }
            if (relevance < 0.35) {
                reasons.add("Low career relevance");
            }
            return STATUS_PENDING;
        }

        if (toxicity >= 0.80) {
            reasons.add("High toxicity score");
            return STATUS_REJECTED;
        }
        if (toxicity <= 0.25 && relevance >= 0.45 && quality >= 0.40 && duplicate <= 0.93) {
            reasons.add("Passed comment auto-approval policy");
            return STATUS_APPROVED;
        }
        return STATUS_PENDING;
    }

    private String readPerspectiveApiKey() {
        String key = System.getenv("PERSPECTIVE_API_KEY");
        return key == null ? "" : key.trim();
    }

    private String toWireType(ContentType type) {
        return type == null ? "post" : type.name().toLowerCase(Locale.ROOT);
    }

    private String cacheKey(String text, ContentType type) {
        return (type == null ? "post" : type.name().toLowerCase(Locale.ROOT)) + ":" + Integer.toHexString(text.hashCode());
    }
}
