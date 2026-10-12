package com.annasetu.service;

import com.annasetu.dto.PlatformStatsDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit tests for {@link PlatformStatsServiceImpl} validating platform metrics aggregation.
 *
 * Domain & Architecture Context:
 * - Tests data assembly from multiple repositories for the public landing view.
 * - Asserts handling of distinct active city counts and approved organization listings.
 *
 * Interview Talking Points:
 * 1. Read-Only Aggregation Testing: Verifies the service correctly interacts with all 4 domain repositories.
 * 2. Deterministic Mock Scaffolding: Isolates business logic from database schema changes.
 */
@ExtendWith(MockitoExtension.class)
class PlatformStatsServiceTest {

    @Mock
    private OrganizationProfileRepository organizationProfileRepository;

    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @InjectMocks
    private PlatformStatsServiceImpl platformStatsService;

    @Test
    @DisplayName("getPlatformStats aggregates counts across all repositories")
    void testGetPlatformStats() {
        when(organizationProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(8L);
        when(organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(List.of("Pune", "Mumbai", "Bengaluru"));
        when(slotAvailabilityRepository.count()).thenReturn(120L);
        when(bookingRepository.count()).thenReturn(45L);
        when(donorProfileRepository.count()).thenReturn(25L);

        PlatformStatsDto stats = platformStatsService.getPlatformStats();

        assertNotNull(stats);
        assertEquals(8L, stats.getTotalOrganizations());
        assertEquals(3L, stats.getTotalCities());
        assertEquals(120L, stats.getTotalSlots());
        assertEquals(45L, stats.getTotalBookings());
        assertEquals(25L, stats.getTotalDonors());
    }

    @Test
    @DisplayName("getFeaturedOrganizations returns only approved organizations")
    void testGetFeaturedOrganizations() {
        OrganizationProfile org1 = new OrganizationProfile();
        org1.setName("Org 1");
        OrganizationProfile org2 = new OrganizationProfile();
        org2.setName("Org 2");

        when(organizationProfileRepository.findByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(List.of(org1, org2));

        List<OrganizationProfile> featured = platformStatsService.getFeaturedOrganizations();

        assertEquals(2, featured.size());
        assertEquals("Org 1", featured.get(0).getName());
    }

    @Test
    @DisplayName("getAvailableCities returns distinct approved cities")
    void testGetAvailableCities() {
        when(organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(List.of("Pune", "Bengaluru"));

        List<String> cities = platformStatsService.getAvailableCities();

        assertEquals(2, cities.size());
        assertTrue(cities.contains("Pune"));
        assertTrue(cities.contains("Bengaluru"));
    }
}
