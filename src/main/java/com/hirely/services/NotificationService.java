package com.hirely.services;

import com.hirely.dao.NotificationDAO;
import com.hirely.models.Interview;
import com.hirely.models.Notification;

/**
 * Service for managing notifications
 */
public class NotificationService {

    private NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    /**
     * Notify when interview is scheduled
     */
    public void notifyInterviewScheduled(Interview interview, int candidateUserId, String candidateName, String jobTitle) {
        // Notify candidate
        Notification candidateNotif = new Notification(
            candidateUserId,
            "INTERVIEW_INVITATION",
            "New Interview Invitation",
            "You have been invited for a " + interview.getInterviewTypeName() +
            " interview for " + jobTitle + " on " + interview.getScheduledDate() +
            " at " + interview.getScheduledTime() + ". Please confirm your availability."
        );
        candidateNotif.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(candidateNotif);

        System.out.println("📬 Notification sent to candidate: " + candidateName);
    }

    /**
     * Notify when candidate confirms interview
     */
    public void notifyInterviewConfirmed(Interview interview, int recruiterUserId, String candidateName, String jobTitle) {
        // Notify recruiter
        Notification recruiterNotif = new Notification(
            recruiterUserId,
            "INTERVIEW_CONFIRMED",
            "Interview Confirmed",
            candidateName + " has confirmed the interview for " + jobTitle +
            " scheduled on " + interview.getScheduledDate() + " at " + interview.getScheduledTime() + "."
        );
        recruiterNotif.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(recruiterNotif);

        System.out.println("📬 Notification sent to recruiter");
    }

    /**
     * Notify when candidate declines interview
     */
    public void notifyInterviewDeclined(Interview interview, int recruiterUserId, String candidateName, String jobTitle) {
        // Notify recruiter
        Notification recruiterNotif = new Notification(
            recruiterUserId,
            "INTERVIEW_CANCELLED",
            "Interview Declined",
            candidateName + " has declined the interview for " + jobTitle +
            " that was scheduled on " + interview.getScheduledDate() + "."
        );
        recruiterNotif.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(recruiterNotif);

        System.out.println("📬 Notification sent to recruiter about decline");
    }

    /**
     * Notify when interview is cancelled by recruiter
     */
    public void notifyInterviewCancelled(Interview interview, int candidateUserId, String jobTitle, String reason) {
        // Notify candidate
        Notification candidateNotif = new Notification(
            candidateUserId,
            "INTERVIEW_CANCELLED",
            "Interview Cancelled",
            "Your interview for " + jobTitle + " scheduled on " + interview.getScheduledDate() +
            " has been cancelled." + (reason != null ? " Reason: " + reason : "")
        );
        candidateNotif.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(candidateNotif);

        System.out.println("📬 Notification sent to candidate about cancellation");
    }

    /**
     * Notify when evaluation is completed
     */
    public void notifyEvaluationCompleted(Interview interview, int candidateUserId, String jobTitle) {
        // Notify candidate
        Notification candidateNotif = new Notification(
            candidateUserId,
            "EVALUATION_COMPLETED",
            "Interview Evaluated",
            "Your interview for " + jobTitle + " has been evaluated. " +
            "The recruiter will contact you regarding the next steps."
        );
        candidateNotif.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(candidateNotif);

        System.out.println("📬 Notification sent to candidate about evaluation");
    }

    /**
     * Send interview reminder (could be scheduled task)
     */
    public void sendInterviewReminder(Interview interview, int userId, String jobTitle, int hoursBeforeInterview) {
        Notification reminder = new Notification(
            userId,
            "INTERVIEW_REMINDER",
            "Interview Reminder",
            "Reminder: You have an interview for " + jobTitle +
            " in " + hoursBeforeInterview + " hours on " + interview.getScheduledDate() +
            " at " + interview.getScheduledTime() + "."
        );
        reminder.setInterviewId(interview.getInterviewId());
        notificationDAO.createNotification(reminder);

        System.out.println("⏰ Reminder sent");
    }
}
