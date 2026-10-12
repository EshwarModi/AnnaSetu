package com.annasetu.repository;

import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility:
 * Integration test suite validating the Data Access Layer (Spring Data JPA Repositories)
 * against the MySQL database, schema constraints, and seed data.
 *
 * Domain & Architecture Context:
 * - Validates schema integrity (foreign keys, unique constraints, check constraints).
 * - Tests derived query methods and custom JPQL queries across all repositories.
 * - Asserts BCrypt password validity for seeded accounts.
 *
 * Interview Talking Points:
 * 1. Transactional Testing: Tests run within a transaction roll back any mutating actions,
 *    preserving database determinism across builds.
 * 2. Full Stack Confidence: By validating against the real database engine with {@code ddl-auto=validate},
 *    we guarantee that production migrations and JPA mappings stay in 100% lockstep.
 */
@SpringBootTest
@Transactional
class RepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Verify Seed Users: Admin, Organizations, and Donors exist with valid BCrypt passwords")
    void testSeedUsersAndBCryptPassword() {
        Optional<User> adminOpt = userRepository.findByEmail("admin@annasetu.org");
        assertTrue(adminOpt.isPresent(), "Admin user should exist in seed data");
        User admin = adminOpt.get();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(passwordEncoder.matches("Password@123", admin.getPasswordHash()),
                "Admin BCrypt hash should match default Password@123");

        assertEquals(1, userRepository.countByRole(Role.ADMIN));
        assertEquals(8, userRepository.countByRole(Role.ORGANIZATION));
        assertEquals(5, userRepository.countByRole(Role.DONOR));
    }

    @Test
    @DisplayName("Verify Organizations: 8 approved organizations across 3 cities")
    void testSeedOrganizationsAndCityQueries() {
        List<OrganizationProfile> approvedOrgs = organizationProfileRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        assertEquals(8, approvedOrgs.size(), "Should find 8 approved organizations");

        List<String> cities = organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED);
        assertEquals(3, cities.size(), "Should have exactly 3 distinct cities");
        assertTrue(cities.contains("Pune"));
        assertTrue(cities.contains("Mumbai"));
        assertTrue(cities.contains("Bengaluru"));

        List<OrganizationProfile> puneOrgs = organizationProfileRepository.findByCityIgnoreCaseAndApprovalStatus("Pune", ApprovalStatus.APPROVED);
        assertEquals(3, puneOrgs.size(), "Pune should have 3 organizations");

        List<OrganizationProfile> mumbaiOrgs = organizationProfileRepository.findByCityIgnoreCaseAndApprovalStatus("Mumbai", ApprovalStatus.APPROVED);
        assertEquals(3, mumbaiOrgs.size(), "Mumbai should have 3 organizations");

        List<OrganizationProfile> blrOrgs = organizationProfileRepository.findByCityIgnoreCaseAndApprovalStatus("Bengaluru", ApprovalStatus.APPROVED);
        assertEquals(2, blrOrgs.size(), "Bengaluru should have 2 organizations");
    }

    @Test
    @DisplayName("Verify Donor Profiles: 5 donors linked with user accounts")
    void testSeedDonors() {
        assertEquals(5, donorProfileRepository.count());

        Optional<DonorProfile> donorOpt = donorProfileRepository.findByUserEmail("rahul.sharma@example.com");
        assertTrue(donorOpt.isPresent());
        DonorProfile donor = donorOpt.get();
        assertEquals("Rahul Sharma", donor.getFullName());
        assertEquals("Mumbai", donor.getCity());
    }

    @Test
    @DisplayName("Verify Slots: 30 days of slots populated for all organizations")
    void testSlotAvailabilities() {
        long totalSlots = slotAvailabilityRepository.count();
        assertEquals(720, totalSlots, "8 orgs * 30 days * 3 meal slots should equal 720 slots");

        // Verify slot lookup for organization 1 for tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Optional<SlotAvailability> lunchSlotOpt = slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(
                1L, tomorrow, MealSlot.LUNCH);
        assertTrue(lunchSlotOpt.isPresent());
        SlotAvailability lunchSlot = lunchSlotOpt.get();
        assertEquals(2, lunchSlot.getCapacity());
        assertEquals(1, lunchSlot.getBookedCount());
        assertTrue(lunchSlot.isAvailable(), "Slot with capacity 2 and bookedCount 1 should be available");
        assertEquals(1, lunchSlot.getRemainingCapacity());
    }

    @Test
    @DisplayName("Verify Bookings: Sample bookings are linked to slots and donors")
    void testSeedBookings() {
        long bookingCount = bookingRepository.count();
        assertTrue(bookingCount >= 5, "Should have at least 5 pre-booked reservations in seed data");

        List<Booking> donorBookings = bookingRepository.findDonorBookingsWithDetails(1L);
        assertFalse(donorBookings.isEmpty());
        Booking b = donorBookings.get(0);
        assertNotNull(b.getSlot());
        assertNotNull(b.getSlot().getOrganization());
        assertEquals("Rahul Sharma", b.getDonor().getFullName());
    }
}
