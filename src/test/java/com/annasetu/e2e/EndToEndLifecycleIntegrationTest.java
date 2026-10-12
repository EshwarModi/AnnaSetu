package com.annasetu.e2e;

import com.annasetu.dto.BookingRequestDto;
import com.annasetu.dto.DonorRegistrationDto;
import com.annasetu.dto.OrganizationRegistrationDto;
import com.annasetu.dto.OrganizationSearchCriteria;
import com.annasetu.dto.OrganizationSearchResultDto;
import com.annasetu.dto.SlotCreationDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Occasion;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.entity.enums.Role;
import com.annasetu.exception.SlotFullException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import com.annasetu.service.AdminService;
import com.annasetu.service.BookingService;
import com.annasetu.service.OrganizationService;
import com.annasetu.service.SlotService;
import com.annasetu.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility:
 * Comprehensive End-to-End lifecycle integration test verifying the entire multi-persona user journey:
 * 1. Donor 1 registers.
 * 2. Organization registers (starts PENDING).
 * 3. Platform Admin approves Organization.
 * 4. Organization publishes availability slots (capacity = 2 for shared meal sponsorships).
 * 5. Donor searches and discovers available slots.
 * 6. Donor 1 books the slot.
 * 7. Host organization confirms the reservation.
 * 8. Donor 2 books the same shared slot (bookedCount reaches capacity 2/2).
 * 9. Donor 3 attempts to book the full slot and is rejected with SlotFullException.
 * 10. Donor 1 cancels their booking; bookedCount decrements back to 1 and slot reopens for donors.
 *
 * Domain & Architecture Context:
 * - Executes across all application layers (Services, Repositories, Pessimistic Locking, Notifications).
 * - Tests full end-to-end data flow with real entity persistence and business rule validation.
 *
 * Interview Talking Points:
 * 1. Full-Lifecycle System Verification: Tests the interplay of all 3 system roles (DONOR, ORGANIZATION, ADMIN).
 * 2. Concurrency & Business Invariants: Verifies shared slot capacity enforcement, refusal of oversubscription,
 *    and automated capacity recycling on cancellation.
 */
@SpringBootTest
@Transactional
class EndToEndLifecycleIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private BookingService bookingService;

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
    @DisplayName("Complete E2E Scenario: Registration -> Approval -> Slots -> Booking -> Shared Capacity -> Slot Full -> Cancellation")
    void testFullEndToEndPlatformLifecycle() {
        // =====================================================================
        // STEP 1: Donor 1 Registers
        // =====================================================================
        String donor1Email = "e2e.donor1." + System.currentTimeMillis() + "@annasetu.org";
        DonorRegistrationDto donor1Dto = new DonorRegistrationDto(
                "Aarav Patel",
                donor1Email,
                "Password@123",
                "Password@123",
                "+91 98765 43210",
                "Pune"
        );
        User donor1User = userService.registerDonor(donor1Dto);
        assertNotNull(donor1User.getId(), "Donor 1 user record should be generated");
        assertEquals(Role.DONOR, donor1User.getRole());
        assertTrue(donor1User.isEnabled());

        DonorProfile donor1Profile = donorProfileRepository.findByUserId(donor1User.getId())
                .orElseThrow(() -> new AssertionError("Donor 1 profile should exist"));
        assertEquals("Aarav Patel", donor1Profile.getFullName());

        // =====================================================================
        // STEP 2: Organization Registers (Starts PENDING)
        // =====================================================================
        String orgEmail = "e2e.shelter." + System.currentTimeMillis() + "@annasetu.org";
        OrganizationRegistrationDto orgDto = new OrganizationRegistrationDto();
        orgDto.setName("E2E Hope Shelter");
        orgDto.setEmail(orgEmail);
        orgDto.setPassword("Password@123");
        orgDto.setConfirmPassword("Password@123");
        orgDto.setType(OrganizationType.SHELTER);
        orgDto.setAddress("Plot 42, Anand Nagar Road");
        orgDto.setCity("Pune");
        orgDto.setArea("Kothrud");
        orgDto.setPincode("411038");
        orgDto.setContactPerson("Sneha Kulkarni");
        orgDto.setPhone("+91 98220 54321");
        orgDto.setPeopleServed(55);
        orgDto.setDescription("Dedicated shelter providing food and care for underprivileged families.");
        orgDto.setFoodPreference(FoodPreference.VEG);

        User orgUser = userService.registerOrganization(orgDto);
        assertNotNull(orgUser.getId(), "Organization user record should be generated");
        assertEquals(Role.ORGANIZATION, orgUser.getRole());

        OrganizationProfile orgProfile = organizationProfileRepository.findByUserId(orgUser.getId())
                .orElseThrow(() -> new AssertionError("Organization profile should exist"));
        assertEquals(ApprovalStatus.PENDING, orgProfile.getApprovalStatus(), "Newly registered organization must start in PENDING status");

        // =====================================================================
        // STEP 3: Platform Admin Approves Organization
        // =====================================================================
        adminService.approveOrganization(orgProfile.getId());

        OrganizationProfile approvedOrg = organizationProfileRepository.findById(orgProfile.getId())
                .orElseThrow(() -> new AssertionError("Approved organization should exist"));
        assertEquals(ApprovalStatus.APPROVED, approvedOrg.getApprovalStatus(), "Organization status must transition to APPROVED");
        assertTrue(userRepository.findById(orgUser.getId()).orElseThrow().isEnabled(), "Organization user account should be active");

        // =====================================================================
        // STEP 4: Organization Adds Availability Slots (Capacity = 2 for Shared)
        // =====================================================================
        LocalDate slotDate = LocalDate.now().plusDays(10); // 10 days in the future (safely beyond 12hr cutoff)
        SlotCreationDto slotDto = new SlotCreationDto();
        slotDto.setStartDate(slotDate);
        slotDto.setEndDate(slotDate);
        slotDto.setMealSlots(List.of(MealSlot.LUNCH));
        slotDto.setCapacity(2); // Shared slot: supports 2 donors sponsoring meals

        int slotsCreated = slotService.createSlotsForDateRange(approvedOrg.getId(), slotDto);
        assertTrue(slotsCreated >= 1, "At least 1 slot should be created for LUNCH");

        List<SlotAvailability> availableSlots = slotAvailabilityRepository.findByOrganizationIdAndSlotDateBetweenOrderBySlotDateAscMealSlotAsc(
                approvedOrg.getId(), slotDate, slotDate);
        assertFalse(availableSlots.isEmpty());
        SlotAvailability sharedSlot = availableSlots.stream()
                .filter(s -> s.getMealSlot() == MealSlot.LUNCH)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Lunch slot should be present"));
        assertEquals(2, sharedSlot.getCapacity());
        assertEquals(0, sharedSlot.getBookedCount());

        // =====================================================================
        // STEP 5: Donor Searches & Discovers Available Slots
        // =====================================================================
        OrganizationSearchCriteria criteria = new OrganizationSearchCriteria(
                "Pune", "Kothrud", OrganizationType.SHELTER, slotDate, MealSlot.LUNCH, FoodPreference.VEG
        );
        Page<OrganizationSearchResultDto> searchResults = organizationService.searchOrganizations(criteria, PageRequest.of(0, 10));
        assertNotNull(searchResults);
        assertTrue(searchResults.getContent().stream().anyMatch(o -> o.getId().equals(approvedOrg.getId())),
                "Search query should return the approved organization matching all filters");

        // =====================================================================
        // STEP 6: Donor 1 Books the Slot
        // =====================================================================
        BookingRequestDto bookingReq1 = new BookingRequestDto();
        bookingReq1.setOccasion(Occasion.BIRTHDAY);
        bookingReq1.setPeopleCount(50);
        bookingReq1.setFoodDescription("Puri Bhaji, Paneer Butter Masala, and Gulab Jamun");

        Booking booking1 = bookingService.book(sharedSlot.getId(), donor1Profile.getId(), bookingReq1);
        assertNotNull(booking1.getId(), "Booking 1 should be persisted with generated ID");
        assertEquals(BookingStatus.PENDING, booking1.getStatus());

        // Verify slot bookedCount incremented to 1
        SlotAvailability slotAfterBooking1 = slotAvailabilityRepository.findById(sharedSlot.getId()).orElseThrow();
        assertEquals(1, slotAfterBooking1.getBookedCount(), "Slot bookedCount should increment to 1");

        // =====================================================================
        // STEP 7: Organization Confirms Booking 1
        // =====================================================================
        Booking confirmedBooking1 = bookingService.confirmBooking(booking1.getId(), approvedOrg.getId());
        assertEquals(BookingStatus.CONFIRMED, confirmedBooking1.getStatus(), "Host organization should successfully confirm booking");

        // =====================================================================
        // STEP 8: Donor 2 Books the Same Shared Slot (Capacity reaches 2/2)
        // =====================================================================
        String donor2Email = "e2e.donor2." + System.currentTimeMillis() + "@annasetu.org";
        DonorRegistrationDto donor2Dto = new DonorRegistrationDto(
                "Priya Sharma",
                donor2Email,
                "Password@123",
                "Password@123",
                "+91 98111 22334",
                "Pune"
        );
        User donor2User = userService.registerDonor(donor2Dto);
        DonorProfile donor2Profile = donorProfileRepository.findByUserId(donor2User.getId()).orElseThrow();

        BookingRequestDto bookingReq2 = new BookingRequestDto();
        bookingReq2.setOccasion(Occasion.ANNIVERSARY);
        bookingReq2.setPeopleCount(40);
        bookingReq2.setFoodDescription("Dal Makhani, Jeera Rice, and Kheer");

        Booking booking2 = bookingService.book(sharedSlot.getId(), donor2Profile.getId(), bookingReq2);
        assertNotNull(booking2.getId(), "Booking 2 should be successfully created in shared slot");

        SlotAvailability slotAfterBooking2 = slotAvailabilityRepository.findById(sharedSlot.getId()).orElseThrow();
        assertEquals(2, slotAfterBooking2.getBookedCount(), "Slot bookedCount should reach maximum capacity of 2");

        // =====================================================================
        // STEP 9: Donor 3 Attempts to Book the Full Slot -> Rejection Asserted!
        // =====================================================================
        String donor3Email = "e2e.donor3." + System.currentTimeMillis() + "@annasetu.org";
        DonorRegistrationDto donor3Dto = new DonorRegistrationDto(
                "Vikram Malhotra",
                donor3Email,
                "Password@123",
                "Password@123",
                "+91 98333 44556",
                "Pune"
        );
        User donor3User = userService.registerDonor(donor3Dto);
        DonorProfile donor3Profile = donorProfileRepository.findByUserId(donor3User.getId()).orElseThrow();

        BookingRequestDto bookingReq3 = new BookingRequestDto();
        bookingReq3.setOccasion(Occasion.FESTIVAL);
        bookingReq3.setPeopleCount(35);
        bookingReq3.setFoodDescription("Festival Special Thali");

        assertThrows(SlotFullException.class, () -> {
            bookingService.book(sharedSlot.getId(), donor3Profile.getId(), bookingReq3);
        }, "Booking attempt by 3rd donor must throw SlotFullException when capacity is exhausted");

        // Assert bookedCount is still 2 and no phantom booking was recorded
        SlotAvailability slotAfterBlockedBooking = slotAvailabilityRepository.findById(sharedSlot.getId()).orElseThrow();
        assertEquals(2, slotAfterBlockedBooking.getBookedCount(), "BookedCount must not exceed capacity under any circumstances");

        // =====================================================================
        // STEP 10: Donor 1 Cancels Booking -> Capacity Recycled
        // =====================================================================
        Booking cancelledBooking1 = bookingService.cancel(booking1.getId(), donor1Profile.getId());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking1.getStatus(), "Booking 1 status should transition to CANCELLED");

        SlotAvailability slotAfterCancellation = slotAvailabilityRepository.findById(sharedSlot.getId()).orElseThrow();
        assertEquals(1, slotAfterCancellation.getBookedCount(), "Slot bookedCount must decrement back to 1, reopening slot for donors");

        // Verify that Donor 3 can now book the reopened slot!
        Booking booking3AfterCancel = bookingService.book(sharedSlot.getId(), donor3Profile.getId(), bookingReq3);
        assertNotNull(booking3AfterCancel.getId(), "Donor 3 should now be able to book the freed capacity");
        SlotAvailability slotAfterRebooking = slotAvailabilityRepository.findById(sharedSlot.getId()).orElseThrow();
        assertEquals(2, slotAfterRebooking.getBookedCount(), "Slot bookedCount reaches 2 again");
    }
}
