package probe;

import model.ModerationReport;
import service.ModerationEngine;

import java.util.List;

public final class PerspectiveProbeMain {
    private static final List<String> SAMPLES = List.of(
            "I like this post",
            "Thanks for sharing this interview tip",
            "I hate you",
            "Go die",
            "This is unrelated gaming chat");

    private PerspectiveProbeMain() {
    }

    public static void main(String[] args) {
        ModerationEngine engine = new ModerationEngine();
        System.out.println("=== Moderation Engine Probe ===");
        for (int i = 0; i < SAMPLES.size(); i++) {
            String text = SAMPLES.get(i);
            ModerationReport report = engine.analyzeAsync(ModerationEngine.ContentType.COMMENT, text).join();
            System.out.printf("%02d. %s%n", i + 1, text);
            System.out.printf("    decision=%s toxicity=%.3f relevance=%.3f quality=%.3f duplicate=%.3f%n",
                    report.getDecision(),
                    report.getToxicity(),
                    report.getRelevance(),
                    report.getQualityScore(),
                    report.getDuplicateSimilarity());
            System.out.println("    reasons=" + String.join(" | ", report.getReasons()));
        }
    }
}
