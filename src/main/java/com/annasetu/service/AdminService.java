package com.annasetu.service;

import com.annasetu.dto.AdminStatsDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;

import java.util.List;

/**
 * Responsibility:
 * Business service contract governing administrative operations:
 * platform analytics, organization partner vetting (approval & rejection with feedback),
 * and security user access management (enabling & disabling accounts).
 *
 * Domain & Architecture Context:
 * - Centralizes administrative decision workflows under {@code ROLE_ADMIN}.
 * - Coordinates state transitions on {@link OrganizationProfile} and {@link User}.
 * - Triggers administrative email notifications via {@link NotificationService}.
 *
 * Interview Talking Points:
 * 1. Separation of Concerns: Keeps administrative logic distinct from public discovery or partner self-service.
 * 2. Self-Protection Invariant: Enforces safeguards preventing administrators from disabling their own account.
 * 3. Transparent Feedback: Requires and records audit rationale when rejecting partner applications.
 */
public interface AdminService {

    /**
     * Retrieves aggregated system health metrics for the dashboard.
     */
    AdminStatsDto getAdminStats();

    /**
     * Retrieves the most recent booking transactions across the platform.
     */
    List<Booking> getRecentBookings(int limit);

    /**
     * Retrieves all organization applications currently awaiting administrative verification.
     */
    List<OrganizationProfile> getPendingOrganizations();

    /**
     * Retrieves all organizations regardless of approval status.
     */
    List<OrganizationProfile> getAllOrganizations();

    /**
     * Retrieves all registered platform user accounts for access administration.
     */
    List<User> getAllUsers();

    /**
     * Approves an organization application and unlocks slot publishing.
     */
    OrganizationProfile approveOrganization(Long orgId);

    /**
     * Rejects an organization application with a documented reason and notifies the coordinator.
     */
    OrganizationProfile rejectOrganization(Long orgId, String reason);

    /**
     * Toggles a user's active/disabled status. Prevents self-disabling by the active administrator.
     */
    User toggleUserStatus(Long userId, String authenticatedAdminEmail);
}
