package com.annasetu.repository;

import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility:
 * Data access slice test (@DataJpaTest) validating repository query methods, derived queries,
 * and entity mappings in isolation without starting the full web server or servlet container.
 *
 * Domain & Architecture Context:
 * - Tests Spring Data JPA interfaces against the persistent database with {@code Replace.NONE}.
 * - Verifies custom JPQL queries, JOIN FETCH operations, and status aggregations.
 *
 * Interview Talking Points:
 * 1. Targeted Slice Testing: {@code @DataJpaTest} configures only JPA components (Entities,
 *    Repositories, EntityManager), dramatically speeding up test feedback cycles compared to full {@code @SpringBootTest}.
 * 2. Automated Transaction Rollback: Every test method runs inside a managed transaction that rolls back upon completion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DataJpaRepositorySliceTest {

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    @DisplayName("@DataJpaTest: UserRepository findByEmail returns active user")
    void testUserRepository_FindByEmail() {
        Optional<User> admin = userRepository.findByEmail("admin@annasetu.org");
        assertTrue(admin.isPresent(), "Seeded admin account should be found");
        assertEquals(Role.ADMIN, admin.get().getRole());
        assertTrue(admin.get().isEnabled());
    }

    @Test
    @DisplayName("@DataJpaTest: OrganizationProfileRepository countByApprovalStatus")
    void testOrganizationProfileRepository_CountByStatus() {
        long approvedCount = organizationProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        assertTrue(approvedCount >= 8, "Expected at least 8 seeded approved organizations");

        List<String> cities = organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED);
        assertNotNull(cities);
        assertTrue(cities.size() >= 3, "Expected at least 3 distinct cities in seed data");
    }

    @Test
    @DisplayName("@DataJpaTest: SlotAvailabilityRepository findByOrganizationAndDateRange")
    void testSlotAvailabilityRepository_DateRangeQuery() {
        List<OrganizationProfile> orgs = organizationProfileRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        assertFalse(orgs.isEmpty());
        OrganizationProfile org = orgs.get(0);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);

        List<SlotAvailability> slots = slotAvailabilityRepository.findByOrganizationIdAndSlotDateBetweenOrderBySlotDateAscMealSlotAsc(
                org.getId(), startDate, endDate);
        assertNotNull(slots);
        assertFalse(slots.isEmpty(), "Organization should have scheduled slots within 30-day window");
    }

    @Test
    @DisplayName("@DataJpaTest: BookingRepository findRecentBookingsWithDetails executes with deep JOIN FETCH")
    void testBookingRepository_RecentBookingsJoinFetch() {
        List<Booking> recent = bookingRepository.findRecentBookingsWithDetails(PageRequest.of(0, 10));
        assertNotNull(recent);
        for (Booking booking : recent) {
            assertNotNull(booking.getSlot(), "Slot should be eagerly fetched");
            assertNotNull(booking.getSlot().getOrganization(), "Organization should be eagerly fetched");
            assertNotNull(booking.getDonor(), "Donor should be eagerly fetched");
        }
    }

    @Test
    @DisplayName("@DataJpaTest: SlotAvailabilityRepository pessimistic write lock query exists")
    void testSlotAvailabilityRepository_FindByIdForUpdate() {
        List<SlotAvailability> allSlots = slotAvailabilityRepository.findAll();
        assertFalse(allSlots.isEmpty());
        SlotAvailability slot = allSlots.get(0);

        Optional<SlotAvailability> lockedSlot = slotAvailabilityRepository.findByIdWithPessimisticLock(slot.getId());
        assertTrue(lockedSlot.isPresent());
        assertEquals(slot.getId(), lockedSlot.get().getId());
    }
}
