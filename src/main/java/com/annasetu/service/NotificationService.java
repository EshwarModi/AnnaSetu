package com.annasetu.service;

import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;

/**
 * Responsibility:
 * Business notification contract orchestrating asynchronous and transactional email dispatches
 * for core platform events (slot reservations, confirmations, cancellations, and partner approvals).
 *
 * Domain & Architecture Context:
 * - Decouples presentation and business workflows from external messaging/SMTP providers.
 * - Allows graceful operation in environments where email is disabled or mocked.
 *
 * Interview Talking Points:
 * 1. Asynchronous Decoupling: Keeps notification dispatches isolated from core database transactions.
 * 2. Feature Toggle Pattern: Managed via {@code annasetu.mail.enabled} to prevent unwanted SMTP network calls in dev/test.
 */
public interface NotificationService {

    /**
     * Sends booking reservation notice to donor and host organization.
     */
    void sendBookingCreatedNotice(Booking booking);

    /**
     * Sends confirmation notification to the donor when the host organization confirms.
     */
    void sendBookingConfirmationNotice(Booking booking);

    /**
     * Sends cancellation notification when a booking is cancelled.
     */
    void sendBookingCancellationNotice(Booking booking);

    /**
     * Sends approval notice to the organization coordinator.
     */
    void sendOrganizationApprovalNotice(OrganizationProfile org);

    /**
     * Sends rejection notice with specific feedback to the organization coordinator.
     */
    void sendOrganizationRejectionNotice(OrganizationProfile org, String reason);
}
