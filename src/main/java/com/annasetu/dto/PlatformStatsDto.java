package com.annasetu.dto;

/**
 * Responsibility:
 * Data Transfer Object (DTO) aggregating platform-wide metrics to display on the home page.
 *
 * Domain & Architecture Context:
 * - Decouples view rendering from underlying persistence entity structure.
 * - Conveys high-level impact numbers: total verified organizations, cities served, available slots, and meals booked.
 *
 * Interview Talking Points:
 * 1. Implements DTO design pattern to prevent domain entity leakage into presentation templates.
 * 2. Immutable record or class structure keeping view-model data clean and testable.
 */
public class PlatformStatsDto {

    private final long totalOrganizations;
    private final long totalCities;
    private final long totalSlots;
    private final long totalBookings;
    private final long totalDonors;

    public PlatformStatsDto(long totalOrganizations, long totalCities, long totalSlots,
                            long totalBookings, long totalDonors) {
        this.totalOrganizations = totalOrganizations;
        this.totalCities = totalCities;
        this.totalSlots = totalSlots;
        this.totalBookings = totalBookings;
        this.totalDonors = totalDonors;
    }

    public long getTotalOrganizations() {
        return totalOrganizations;
    }

    public long getTotalCities() {
        return totalCities;
    }

    public long getTotalSlots() {
        return totalSlots;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    @Override
    public String toString() {
        return "PlatformStatsDto{" +
                "totalOrganizations=" + totalOrganizations +
                ", totalCities=" + totalCities +
                ", totalSlots=" + totalSlots +
                ", totalBookings=" + totalBookings +
                ", totalDonors=" + totalDonors +
                '}';
    }
}
