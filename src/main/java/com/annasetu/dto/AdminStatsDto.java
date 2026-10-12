package com.annasetu.dto;

/**
 * Responsibility:
 * Data Transfer Object summarizing system-wide metrics and institutional health indicators
 * for the Administrator Console.
 *
 * Domain & Architecture Context:
 * - Aggregates counts of donors, verified partners, pending verification queues, available slots,
 *   and platform booking transactions.
 * - Decouples presentation analytics from direct multiple database queries.
 *
 * Interview Talking Points:
 * 1. Read-Model Encapsulation: Groups discrete metrics into a single cohesive payload.
 * 2. Operational Insight: Enables administrative oversight of the platform's supply (organizations/slots)
 *    and demand (donors/bookings).
 */
public class AdminStatsDto {

    private long totalDonors;
    private long totalOrganizations;
    private long approvedOrgsCount;
    private long pendingOrgsCount;
    private long rejectedOrgsCount;
    private long totalSlots;
    private long totalBookings;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalDonors, long totalOrganizations, long approvedOrgsCount,
                         long pendingOrgsCount, long rejectedOrgsCount, long totalSlots, long totalBookings) {
        this.totalDonors = totalDonors;
        this.totalOrganizations = totalOrganizations;
        this.approvedOrgsCount = approvedOrgsCount;
        this.pendingOrgsCount = pendingOrgsCount;
        this.rejectedOrgsCount = rejectedOrgsCount;
        this.totalSlots = totalSlots;
        this.totalBookings = totalBookings;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(long totalDonors) {
        this.totalDonors = totalDonors;
    }

    public long getTotalOrganizations() {
        return totalOrganizations;
    }

    public void setTotalOrganizations(long totalOrganizations) {
        this.totalOrganizations = totalOrganizations;
    }

    public long getApprovedOrgsCount() {
        return approvedOrgsCount;
    }

    public void setApprovedOrgsCount(long approvedOrgsCount) {
        this.approvedOrgsCount = approvedOrgsCount;
    }

    public long getPendingOrgsCount() {
        return pendingOrgsCount;
    }

    public void setPendingOrgsCount(long pendingOrgsCount) {
        this.pendingOrgsCount = pendingOrgsCount;
    }

    public long getRejectedOrgsCount() {
        return rejectedOrgsCount;
    }

    public void setRejectedOrgsCount(long rejectedOrgsCount) {
        this.rejectedOrgsCount = rejectedOrgsCount;
    }

    public long getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(long totalSlots) {
        this.totalSlots = totalSlots;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }
}
