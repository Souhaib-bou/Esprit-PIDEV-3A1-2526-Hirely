package service;

import model.ModerationReport;
import util.PerspectiveClient;
import util.PythonScoreClient;
import util.Secrets;

import java.net.ConnectException;
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
    private final PythonScoreClient pythonScoreClient;
    private final ExecutorService executor;

    public ModerationEngine() {
        this(new PerspectiveClient(), new PythonScoreClient());
    }

    public ModerationEngine(PerspectiveClient perspectiveClient, PythonScoreClient pythonScoreClient) {
        this.perspectiveClient = perspectiveClient;
        this.pythonScoreClient = pythonScoreClient;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<ModerationReport> analyzeAsync(ContentType type, String text) {
        String safeText = text == null ? "" : text.trim();
        long startNs = System.nanoTime();
        String perspectiveKey = readPerspectiveApiKey();
        String wireType = toWireType(type);
        String contentKey = cacheKey(safeText, type);

        CompletableFuture<PerspectiveOutcome> perspectiveFuture =
                CompletableFuture.supplyAsync(() -> callPerspective(safeText, perspectiveKey), executor)
                        .handle(PerspectiveOutcome::from);
        CompletableFuture<ScoreOutcome> scoreFuture =
                CompletableFuture.supplyAsync(() -> callScore(safeText, wireType, contentKey), executor)
                        .handle(ScoreOutcome::from);

        return perspectiveFuture.thenCombine(scoreFuture, (perspective, score) ->
                buildReport(type, startNs, perspective, score));
    }

    public void shutdown() {
        executor.shutdown();
    }

    private ModerationReport buildReport(ContentType type, long startNs,
            PerspectiveOutcome perspectiveOutcome, ScoreOutcome scoreOutcome) {
        ModerationReport report = new ModerationReport();
        List<String> reasons = new ArrayList<>();
        report.setReasons(reasons);
        report.setCategory("General");

        boolean fallback = false;

        if (perspectiveOutcome.failed()) {
            fallback = true;
            report.setToxicity(0.50);
            report.setPerspectiveRaw("Perspective unavailable");
            if ("Missing PERSPECTIVE_API_KEY".equals(perspectiveOutcome.errorDetail())) {
                reasons.add("Missing PERSPECTIVE_API_KEY");
            } else if ("Replace YOUR_KEY_HERE".equals(perspectiveOutcome.errorDetail())) {
                reasons.add("Replace YOUR_KEY_HERE");
            } else {
                reasons.add("Perspective API unavailable: " + perspectiveOutcome.errorDetail());
            }
        } else {
            PerspectiveClient.PerspectiveResult perspective = perspectiveOutcome.result();
            report.setToxicity(perspective.getToxicity());
            report.setPerspectiveRaw(perspective.getRaw());
            report.setPerspectiveLatencyMs(perspective.getLatencyMs());
        }

        if (scoreOutcome.failed()) {
            fallback = true;
            report.setRelevance(0.50);
            report.setCategory("General");
            report.setQualityScore(0.50);
            report.setDuplicateSimilarity(0.00);
            report.setPythonRaw("Python services unavailable");
            reasons.add("Python scoring service unavailable: " + scoreOutcome.errorDetail());
        } else {
            PythonScoreClient.ScoreResult score = scoreOutcome.result();
            report.setRelevance(score.getRelevance());
            report.setCategory(score.getCategory());
            report.setQualityScore(score.getQuality());
            report.setDuplicateSimilarity(score.getDuplicateSimilarity());
            report.setPythonRaw("score=" + score.getRaw());
            report.setPythonLatencyMs(score.getLatencyMs());
            reasons.addAll(score.getRelevanceReasons());
            reasons.addAll(score.getQualityReasons());
            reasons.addAll(score.getDuplicateReasons());
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
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isBlank()) {
            throw new CompletionException(new IllegalStateException("Missing PERSPECTIVE_API_KEY"));
        }
        try {
            if ("YOUR_KEY_HERE".equalsIgnoreCase(key)) {
                throw new IllegalStateException("Replace YOUR_KEY_HERE");
            }
            return perspectiveClient.analyze(text, key);
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private PythonScoreClient.ScoreResult callScore(String text, String type, String contentKey) {
        try {
            return pythonScoreClient.score(text, type, contentKey);
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
        // Dev constant (replace YOUR_KEY_HERE locally).
        if (Secrets.PERSPECTIVE_API_KEY == null) {
            return "";
        }
        String key = Secrets.PERSPECTIVE_API_KEY.trim();
        if (key.length() >= 2) {
            if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
                key = key.substring(1, key.length() - 1).trim();
            }
        }
        if ("YOUR_KEY_HERE".equalsIgnoreCase(key)) {
            return "YOUR_KEY_HERE";
        }
        return key;
    }

    private String toWireType(ContentType type) {
        return type == null ? "post" : type.name().toLowerCase(Locale.ROOT);
    }

    private String cacheKey(String text, ContentType type) {
        String normalizedType = type == null ? "post" : type.name().toLowerCase(Locale.ROOT);
        return normalizedType + ":" + Integer.toHexString(text.hashCode()) + ":" + System.currentTimeMillis();
    }

    private static String safeErrorDetail(Throwable error) {
        Throwable root = unwrap(error);
        if (root instanceof ConnectException) {
            return "ConnectException (cannot reach Python AI service; verify PY_AI_URL/service status)";
        }
        String detail = root == null ? "" : root.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = root == null ? "Unknown error" : root.getClass().getSimpleName();
        }
        detail = detail.replace('\n', ' ').replace('\r', ' ').trim();
        if (detail.contains("Missing PERSPECTIVE_API_KEY")) {
            return "Missing PERSPECTIVE_API_KEY";
        }
        if (detail.contains("Replace YOUR_KEY_HERE")) {
            return "Replace YOUR_KEY_HERE";
        }
        if (detail.length() > 120) {
            return detail.substring(0, 120) + "...";
        }
        return detail;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static final class PerspectiveOutcome {
        private final PerspectiveClient.PerspectiveResult result;
        private final String errorDetail;

        private PerspectiveOutcome(PerspectiveClient.PerspectiveResult result, String errorDetail) {
            this.result = result;
            this.errorDetail = errorDetail;
        }

        private static PerspectiveOutcome from(PerspectiveClient.PerspectiveResult result, Throwable error) {
            if (error == null) {
                return new PerspectiveOutcome(result, "");
            }
            return new PerspectiveOutcome(null, safeErrorDetail(error));
        }

        private boolean failed() {
            return result == null;
        }

        private PerspectiveClient.PerspectiveResult result() {
            return result;
        }

        private String errorDetail() {
            return errorDetail;
        }
    }

    private static final class ScoreOutcome {
        private final PythonScoreClient.ScoreResult result;
        private final String errorDetail;

        private ScoreOutcome(PythonScoreClient.ScoreResult result, String errorDetail) {
            this.result = result;
            this.errorDetail = errorDetail;
        }

        private static ScoreOutcome from(PythonScoreClient.ScoreResult result, Throwable error) {
            if (error == null) {
                return new ScoreOutcome(result, "");
            }
            return new ScoreOutcome(null, safeErrorDetail(error));
        }

        private boolean failed() {
            return result == null;
        }

        private PythonScoreClient.ScoreResult result() {
            return result;
        }

        private String errorDetail() {
            return errorDetail;
        }
    }
}
