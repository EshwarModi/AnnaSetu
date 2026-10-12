package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Represents the verification and onboarding lifecycle state of an organization.
 *
 * Domain Context:
 * - PENDING: Newly registered organization waiting for admin verification of credentials/trust papers.
 * - APPROVED: Verified by platform admin; visible on public search and eligible to accept slot bookings.
 * - REJECTED: Failed platform background check or disqualified by admin.
 *
 * Interview Talking Point:
 * Essential for platform trust & safety. Search and booking queries always enforce
 * {@code approvalStatus = 'APPROVED'} to prevent fraudulent or unverified listings.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
