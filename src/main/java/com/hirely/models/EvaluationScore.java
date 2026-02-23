package com.hirely.models;

/**
 * Evaluation Score model (score for individual criteria)
 */
public class EvaluationScore {
    private int scoreId;
    private int evaluationId;
    private int criteriaId;
    private int score;
    private String comments;

    // Additional fields
    private String criteriaName;

    // Constructors
    public EvaluationScore() {
    }

    public EvaluationScore(int criteriaId, int score, String comments) {
        this.criteriaId = criteriaId;
        this.score = score;
        this.comments = comments;
    }

    // Getters and Setters
    public int getScoreId() {
        return scoreId;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
    }

    public int getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(int evaluationId) {
        this.evaluationId = evaluationId;
    }

    public int getCriteriaId() {
        return criteriaId;
    }

    public void setCriteriaId(int criteriaId) {
        this.criteriaId = criteriaId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCriteriaName() {
        return criteriaName;
    }

    public void setCriteriaName(String criteriaName) {
        this.criteriaName = criteriaName;
    }
}
