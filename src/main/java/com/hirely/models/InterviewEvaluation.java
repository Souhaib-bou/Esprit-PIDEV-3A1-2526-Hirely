package com.hirely.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Interview Evaluation model
 */
public class InterviewEvaluation {
    private int evaluationId;
    private int interviewId;
    private int recruiterId;
    private double overallRating;
    private String recommendation;
    private String strengths;
    private String weaknesses;
    private String generalComments;
    private String hireDecision;
    private String nextSteps;
    private boolean isDraft;
    private Timestamp evaluatedAt;
    private Timestamp updatedAt;

    // List of individual scores per criteria
    private List<EvaluationScore> scores;

    // Interview details for display
    private Interview interview;

    // Constructors
    public InterviewEvaluation() {
        this.scores = new ArrayList<>();
    }

    // Getters and Setters
    public int getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(int evaluationId) {
        this.evaluationId = evaluationId;
    }

    public int getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(int interviewId) {
        this.interviewId = interviewId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public double getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(double overallRating) {
        this.overallRating = overallRating;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getGeneralComments() {
        return generalComments;
    }

    public void setGeneralComments(String generalComments) {
        this.generalComments = generalComments;
    }

    public String getHireDecision() {
        return hireDecision;
    }

    public void setHireDecision(String hireDecision) {
        this.hireDecision = hireDecision;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setDraft(boolean draft) {
        isDraft = draft;
    }

    public Timestamp getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Timestamp evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<EvaluationScore> getScores() {
        return scores;
    }

    public void setScores(List<EvaluationScore> scores) {
        this.scores = scores;
    }

    public void addScore(EvaluationScore score) {
        this.scores.add(score);
    }

    public Interview getInterview() {
        return interview;
    }

    public void setInterview(Interview interview) {
        this.interview = interview;
    }
}
