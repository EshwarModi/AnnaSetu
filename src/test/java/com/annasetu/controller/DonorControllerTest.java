package com.annasetu.controller;

import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Occasion;
import com.annasetu.entity.enums.Role;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Integration tests verifying Donor Dashboard, Reservation Form, Booking Submission,
 * and Cancellation workflows.
 *
 * Domain & Architecture Context:
 * - Proves that donors can inspect upcoming and past reservations.
 * - Validates presentation flow of booking forms, Bean Validation rejection, and flash message handling.
 * - Asserts role-based security isolation for donor workspace endpoints.
 *
 * Interview Talking Points:
 * 1. MockMvc Role Isolation: Tests both authorized access (ROLE_DONOR) and security boundary enforcement.
 * 2. PRG Validation: Confirms flash messages on redirect vs. model errors on invalid form redisplay.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DonorControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private DonorProfile donor;
    private SlotAvailability futureSlot;

    @BeforeEach
    void setUp() {
        donor = donorProfileRepository.findByUserEmail("rahul.sharma@example.com")
                .orElseGet(() -> {
                    User u = new User("rahul.sharma@example.com", "hash", Role.DONOR, true);
                    u = userRepository.save(u);
                    DonorProfile d = new DonorProfile(u, "Rahul Sharma", "+91 98111 22233", "Mumbai");
                    return donorProfileRepository.save(d);
                });

        OrganizationProfile org = organizationProfileRepository.findAll().stream()
                .filter(o -> o.getApprovalStatus() == ApprovalStatus.APPROVED)
                .findFirst()
                .orElseThrow();

        // Lookup existing seeded slot or create one outside 30-day seeded window
        futureSlot = slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(
                org.getId(), LocalDate.now().plusDays(10), MealSlot.LUNCH)
                .orElseGet(() -> slotAvailabilityRepository.save(
                        new SlotAvailability(org, LocalDate.now().plusDays(40), MealSlot.LUNCH, 2)
                ));
    }

    @Test
    @DisplayName("GET /donor/dashboard - Authorized donor sees workspace with upcoming and past bookings")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testDonorDashboard_Authorized() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/dashboard"))
                .andExpect(model().attributeExists("donor", "upcomingBookings", "pastBookings", "cancellableMap"));
    }

    @Test
    @DisplayName("GET /donor/dashboard - Unauthenticated user redirected to login")
    void testDonorDashboard_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /donor/dashboard - Organization user forbidden (403)")
    @WithMockUser(username = "org@example.com", roles = "ORGANIZATION")
    void testDonorDashboard_OrganizationUser_Forbidden() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /donor/book - Renders reservation form for valid slot")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testShowBookingForm_Success() throws Exception {
        mockMvc.perform(get("/donor/book").param("slotId", futureSlot.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/book"))
                .andExpect(model().attributeExists("slot", "org", "bookingDto", "occasions"));
    }

    @Test
    @DisplayName("POST /donor/book - Valid reservation redirects to dashboard with success message")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testProcessBooking_Success() throws Exception {
        mockMvc.perform(post("/donor/book")
                        .with(csrf())
                        .param("slotId", futureSlot.getId().toString())
                        .param("occasion", "BIRTHDAY")
                        .param("peopleCount", "50")
                        .param("foodDescription", "Special birthday feast with sweets and paneer curry"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /donor/book - Blank food description triggers Bean Validation error")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testProcessBooking_ValidationError() throws Exception {
        mockMvc.perform(post("/donor/book")
                        .with(csrf())
                        .param("slotId", futureSlot.getId().toString())
                        .param("occasion", "BIRTHDAY")
                        .param("peopleCount", "50")
                        .param("foodDescription", "")) // Blank triggers validation error
                .andExpect(status().isOk())
                .andExpect(view().name("donor/book"))
                .andExpect(model().attributeHasFieldErrors("bookingDto", "foodDescription"));
    }

    @Test
    @DisplayName("POST /donor/bookings/{id}/cancel - Cancels active booking and redirects with flash message")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testCancelBooking_Success() throws Exception {
        // Prepare an active booking far in the future
        futureSlot.setBookedCount(1);
        slotAvailabilityRepository.save(futureSlot);

        Booking booking = new Booking(
                futureSlot,
                donor,
                Occasion.ANNIVERSARY,
                40,
                "Anniversary dinner",
                BookingStatus.PENDING
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(post("/donor/bookings/" + booking.getId() + "/cancel")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("GET /donor/bookings/{id}/receipt - Renders printable receipt for authorized donor")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testViewReceipt_Success() throws Exception {
        Booking booking = new Booking(
                futureSlot,
                donor,
                Occasion.BIRTHDAY,
                50,
                "Birthday feast receipt test",
                BookingStatus.CONFIRMED
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(get("/donor/bookings/" + booking.getId() + "/receipt"))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/receipt"))
                .andExpect(model().attributeExists("donor", "booking", "receiptNumber", "issueDate"));
    }
}
