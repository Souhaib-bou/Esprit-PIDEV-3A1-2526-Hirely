package model;

import java.util.ArrayList;
import java.util.List;

public class ModerationReport {
    private double toxicity;
    private double relevance;
    private String category;
    private double duplicateSimilarity;
    private double qualityScore;
    private String decision;
    private List<String> reasons = new ArrayList<>();
    private String perspectiveRaw;
    private String pythonRaw;
    private long perspectiveLatencyMs;
    private long pythonLatencyMs;
    private long totalLatencyMs;
    private boolean fallbackUsed;

    public double getToxicity() {
        return toxicity;
    }

    public void setToxicity(double toxicity) {
        this.toxicity = toxicity;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getDuplicateSimilarity() {
        return duplicateSimilarity;
    }

    public void setDuplicateSimilarity(double duplicateSimilarity) {
        this.duplicateSimilarity = duplicateSimilarity;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
    }

    public String getPerspectiveRaw() {
        return perspectiveRaw;
    }

    public void setPerspectiveRaw(String perspectiveRaw) {
        this.perspectiveRaw = perspectiveRaw;
    }

    public String getPythonRaw() {
        return pythonRaw;
    }

    public void setPythonRaw(String pythonRaw) {
        this.pythonRaw = pythonRaw;
    }

    public long getPerspectiveLatencyMs() {
        return perspectiveLatencyMs;
    }

    public void setPerspectiveLatencyMs(long perspectiveLatencyMs) {
        this.perspectiveLatencyMs = perspectiveLatencyMs;
    }

    public long getPythonLatencyMs() {
        return pythonLatencyMs;
    }

    public void setPythonLatencyMs(long pythonLatencyMs) {
        this.pythonLatencyMs = pythonLatencyMs;
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
}
