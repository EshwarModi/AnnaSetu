package com.annasetu.service;

import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Responsibility:
 * Concrete email notification provider using Spring Mail ({@link JavaMailSender}).
 *
 * Domain & Architecture Context:
 * - Operates under a feature flag ({@code annasetu.mail.enabled=false} by default).
 * - When disabled, outputs formatted log messages simulating live mail deliveries without SMTP overhead.
 * - When enabled, attempts real SMTP transmission and defensively catches {@link org.springframework.mail.MailException}
 *   so that notification delivery failures never roll back crucial database transactions.
 *
 * Interview Talking Points:
 * 1. Safe Optional Injection: Uses {@link ObjectProvider} to safely inject {@link JavaMailSender} without
 *    failing application boot if mail auto-configuration is absent or partial.
 * 2. Fault Tolerance: Encapsulates SMTP calls in try-catch blocks to prevent email server outages from impacting users.
 * 3. Audit Logging: Every email attempt is logged with recipient, subject, and event context.
 */
@Service
public class EmailServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${annasetu.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${annasetu.mail.from:noreply@annasetu.org}")
    private String fromAddress;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    private void dispatchEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[EMAIL SIMULATION - DISABLED VIA CONFIG] To: {} | Subject: '{}' | Content: {}",
                    to, subject, body.replaceAll("\n", " "));
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[EMAIL SKIPPED] Mail is enabled but JavaMailSender bean is unavailable. Target: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[EMAIL SENT] Successfully dispatched email to: {} with subject: '{}'", to, subject);
        } catch (Exception ex) {
            log.error("[EMAIL ERROR] Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    @Override
    public void sendBookingCreatedNotice(Booking booking) {
        if (booking == null) return;
        String donorEmail = booking.getDonor().getUser().getEmail();
        String subject = "AnnaSetu: Meal Slot Reserved - #BK-" + booking.getId();
        String body = String.format(
                "Dear %s,\n\nYour food donation slot has been reserved successfully!\n\n" +
                "Booking Reference: #BK-%d\n" +
                "Organization: %s (%s)\n" +
                "Date: %s (%s)\n" +
                "Occasion: %s\n" +
                "Status: PENDING HOST CONFIRMATION\n\n" +
                "You can view details and print your receipt at: /donor/bookings/%d/receipt\n\n" +
                "Warm regards,\nAnnaSetu Team",
                booking.getDonor().getFullName(),
                booking.getId(),
                booking.getSlot().getOrganization().getName(),
                booking.getSlot().getOrganization().getCity(),
                booking.getSlot().getSlotDate(),
                booking.getSlot().getMealSlot(),
                booking.getOccasion(),
                booking.getId()
        );
        dispatchEmail(donorEmail, subject, body);
    }

    @Override
    public void sendBookingConfirmationNotice(Booking booking) {
        if (booking == null) return;
        String donorEmail = booking.getDonor().getUser().getEmail();
        String subject = "AnnaSetu: Booking Confirmed! - #BK-" + booking.getId();
        String body = String.format(
                "Dear %s,\n\nWonderful news! %s has confirmed your meal sponsorship.\n\n" +
                "Date: %s (%s)\n" +
                "Beneficiaries: %d residents\n" +
                "Menu: %s\n\n" +
                "Thank you for sharing your celebration with those who need it most.\n\n" +
                "Warm regards,\nAnnaSetu Team",
                booking.getDonor().getFullName(),
                booking.getSlot().getOrganization().getName(),
                booking.getSlot().getSlotDate(),
                booking.getSlot().getMealSlot(),
                booking.getSlot().getOrganization().getPeopleServed(),
                booking.getFoodDescription()
        );
        dispatchEmail(donorEmail, subject, body);
    }

    @Override
    public void sendBookingCancellationNotice(Booking booking) {
        if (booking == null) return;
        String donorEmail = booking.getDonor().getUser().getEmail();
        String subject = "AnnaSetu: Booking Cancelled - #BK-" + booking.getId();
        String body = String.format(
                "Dear %s,\n\nYour booking #BK-%d for %s on %s has been cancelled and the slot has been reopened.\n\n" +
                "Warm regards,\nAnnaSetu Team",
                booking.getDonor().getFullName(),
                booking.getId(),
                booking.getSlot().getOrganization().getName(),
                booking.getSlot().getSlotDate()
        );
        dispatchEmail(donorEmail, subject, body);
    }

    @Override
    public void sendOrganizationApprovalNotice(OrganizationProfile org) {
        if (org == null) return;
        String orgEmail = org.getUser().getEmail();
        String subject = "AnnaSetu: Partner Registration Approved!";
        String body = String.format(
                "Dear %s Team,\n\nCongratulations! Your institution application has been verified and approved by the AnnaSetu administration.\n\n" +
                "You can now log in, generate meal availability slots, and start accepting meal sponsorships from donors.\n\n" +
                "Warm regards,\nAnnaSetu Administration",
                org.getName()
        );
        dispatchEmail(orgEmail, subject, body);
    }

    @Override
    public void sendOrganizationRejectionNotice(OrganizationProfile org, String reason) {
        if (org == null) return;
        String orgEmail = org.getUser().getEmail();
        String subject = "AnnaSetu: Partner Application Update";
        String body = String.format(
                "Dear %s Team,\n\nThank you for applying to AnnaSetu. Your application could not be approved at this time.\n\n" +
                "Reason provided by administrator:\n%s\n\n" +
                "You can log in to update your profile details and resubmit.\n\n" +
                "Warm regards,\nAnnaSetu Administration",
                org.getName(),
                reason != null && !reason.isBlank() ? reason : "Profile verification requirements were not met."
        );
        dispatchEmail(orgEmail, subject, body);
    }
}
