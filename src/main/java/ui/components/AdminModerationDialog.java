package ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import model.ModerationReport;

import java.util.List;

public final class AdminModerationDialog {
    private AdminModerationDialog() {
    }

    public static void show(ModerationReport report, String contentType, String identifier) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Admin AI Analysis");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        String normalizedType = safe(contentType).isBlank() ? "Content" : safe(contentType);
        Label analyzedType = new Label("Analyzed: " + normalizedType);
        root.getChildren().add(analyzedType);
        if (!safe(identifier).isBlank()) {
            root.getChildren().add(new Label("Reference: " + identifier));
        }

        VBox summary = new VBox(6);
        summary.getChildren().addAll(
                new Label("Decision: " + safe(report.getDecision())),
                new Label("Recommended Admin Action: " + recommendedAction(report.getDecision())),
                new Label(String.format("Toxicity: %.3f (%s)", report.getToxicity(), toxicityBand(report.getToxicity()))),
                new Label(String.format("Relevance: %.3f (%s)", report.getRelevance(), relevanceBand(report.getRelevance()))),
                new Label(String.format("Quality: %.3f (%s)", report.getQualityScore(), qualityBand(report.getQualityScore()))),
                new Label(String.format("Duplicate Similarity: %.3f (%s)",
                        report.getDuplicateSimilarity(), duplicateBand(report.getDuplicateSimilarity()))),
                new Label(String.format("Latency: total=%dms, perspective=%dms, python=%dms",
                        report.getTotalLatencyMs(),
                        report.getPerspectiveLatencyMs(),
                        report.getPythonLatencyMs())));

        Label plainEnglishTitle = new Label("Plain English Summary");
        TextArea plainEnglish = textPane(buildPlainEnglishSummary(report), 190);

        TitledPane reasonsPane = new TitledPane("Reasons", textPane(toBullets(report.getReasons()), 120));
        reasonsPane.setExpanded(true);

        TitledPane perspectivePane = new TitledPane(
                "Perspective Raw Output",
                textPane(safe(report.getPerspectiveRaw()), 160));
        perspectivePane.setExpanded(false);

        TitledPane pythonPane = new TitledPane(
                "Python Raw Output",
                textPane(safe(report.getPythonRaw()), 140));
        pythonPane.setExpanded(false);

        root.getChildren().addAll(summary, plainEnglishTitle, plainEnglish, reasonsPane, perspectivePane, pythonPane);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    private static String buildPlainEnglishSummary(ModerationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Toxicity ").append(formatScore(report.getToxicity())).append(": ")
                .append(toxicityBand(report.getToxicity())).append('.').append('\n');
        sb.append("Relevance ").append(formatScore(report.getRelevance())).append(": ")
                .append(relevanceBand(report.getRelevance())).append('.').append('\n');
        sb.append("Quality ").append(formatScore(report.getQualityScore())).append(": ")
                .append(qualityBand(report.getQualityScore())).append('.').append('\n');
        sb.append("Duplicate ").append(formatScore(report.getDuplicateSimilarity())).append(": ")
                .append(duplicateBand(report.getDuplicateSimilarity())).append('.').append('\n');
        sb.append("Recommended Admin Action: ").append(recommendedAction(report.getDecision())).append('.').append('\n');
        sb.append("Latency: total ").append(report.getTotalLatencyMs()).append("ms, Perspective ")
                .append(report.getPerspectiveLatencyMs()).append("ms, Python ")
                .append(report.getPythonLatencyMs()).append("ms.");
        return sb.toString();
    }

    private static TextArea textPane(String content, double prefHeight) {
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefHeight(prefHeight);
        return area;
    }

    private static String toxicityBand(double score) {
        if (score < 0.20) {
            return "safe";
        }
        if (score < 0.50) {
            return "borderline";
        }
        if (score < 0.80) {
            return "concerning";
        }
        return "toxic";
    }

    private static String relevanceBand(double score) {
        if (score >= 0.60) {
            return "on-topic";
        }
        if (score >= 0.35) {
            return "somewhat on-topic";
        }
        return "off-topic";
    }

    private static String qualityBand(double score) {
        if (score >= 0.80) {
            return "strong";
        }
        if (score >= 0.55) {
            return "ok";
        }
        return "weak";
    }

    private static String duplicateBand(double score) {
        if (score >= 0.93) {
            return "near-duplicate or spam";
        }
        if (score >= 0.88) {
            return "possible duplicate";
        }
        return "not a duplicate concern";
    }

    private static String recommendedAction(String decision) {
        if ("APPROVED".equalsIgnoreCase(safe(decision))) {
            return "Approve";
        }
        if ("REJECTED".equalsIgnoreCase(safe(decision))) {
            return "Reject";
        }
        return "Manual review";
    }

    private static String toBullets(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "- No additional reasons";
        }
        StringBuilder sb = new StringBuilder();
        for (String reason : reasons) {
            sb.append("- ").append(reason == null ? "" : reason).append('\n');
        }
        return sb.toString().trim();
    }

    private static String formatScore(double score) {
        return String.format("%.3f", score);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
