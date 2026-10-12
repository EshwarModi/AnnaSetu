package com.annasetu.controller;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.entity.enums.Role;
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
 * Integration tests verifying Organization Dashboard, Profile Edit, and Slot Management workflows.
 *
 * Domain & Architecture Context:
 * - Proves that approved organizations can manage their profile, view metrics, and create/update/block slots.
 * - Proves that pending organizations are restricted: slot generation and updates are locked, and status banner is shown.
 * - Verifies CSRF protection and Post/Redirect/Get flash messaging patterns across all form submissions.
 *
 * Interview Talking Points:
 * 1. Role and Lifecycle Isolation: Tests behavior under both APPROVED and PENDING statuses to ensure security gates work.
 * 2. Flash Attribute Assertions: Asserts PRG redirection patterns using {@code flash().attributeExists(...)}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private SlotAvailabilityRepository slotAvailabilityRepository;

    private User pendingUser;
    private OrganizationProfile pendingOrg;

    @BeforeEach
    void setUp() {
        // Create an isolated pending organization for negative restriction tests
        if (userRepository.findByEmail("pending.test@annasetu.org").isEmpty()) {
            pendingUser = new User("pending.test@annasetu.org", "$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i", Role.ORGANIZATION);
            pendingUser = userRepository.save(pendingUser);

            pendingOrg = new OrganizationProfile(
                    pendingUser, "Shanti Seva Home", OrganizationType.OLD_AGE_HOME,
                    "77 MG Road", "Pune", "Camp", "411001", "Anand Kulkarni",
                    "+91 98220 77777", 40, "Elder care home awaiting verification",
                    FoodPreference.VEG, ApprovalStatus.PENDING
            );
            pendingOrg = organizationProfileRepository.save(pendingOrg);
        }
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("Approved org accessing /org/dashboard renders dashboard with APPROVED status banner")
    void testApprovedOrgDashboardRenders() throws Exception {
        mockMvc.perform(get("/org/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("org/dashboard"))
                .andExpect(model().attributeExists("org", "bookings", "upcomingSlotsCount", "bookedSlotsCount"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")))
                .andExpect(content().string(containsString("APPROVED")));
    }

    @Test
    @WithMockUser(username = "pending.test@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("Pending org accessing /org/dashboard renders dashboard with PENDING status banner")
    void testPendingOrgDashboardRendersNotice() throws Exception {
        mockMvc.perform(get("/org/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("org/dashboard"))
                .andExpect(content().string(containsString("Account Verification Pending")))
                .andExpect(content().string(containsString("PENDING ADMIN APPROVAL")));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("GET /org/profile displays editable profile form pre-populated with organization details")
    void testViewProfilePage() throws Exception {
        mockMvc.perform(get("/org/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("org/profile"))
                .andExpect(model().attributeExists("profileDto", "org", "orgTypes", "foodPreferences"))
                .andExpect(content().string(containsString("Organization Profile")));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/profile with valid inputs updates profile and redirects with success message")
    void testUpdateProfileSuccess() throws Exception {
        mockMvc.perform(post("/org/profile")
                        .with(csrf())
                        .param("name", "Asha Kiran Children Home (Updated)")
                        .param("type", "ORPHANAGE")
                        .param("contactPerson", "Sunita Deshmukh")
                        .param("phone", "+91 98220 11223")
                        .param("address", "Plot 45, FC Road")
                        .param("city", "Pune")
                        .param("area", "Deccan")
                        .param("pincode", "411004")
                        .param("peopleServed", "70")
                        .param("foodPreference", "VEG")
                        .param("description", "Updated description for children care"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/profile with blank required fields re-renders profile page with errors")
    void testUpdateProfileValidationFailure() throws Exception {
        mockMvc.perform(post("/org/profile")
                        .with(csrf())
                        .param("name", "") // Blank name
                        .param("type", "ORPHANAGE")
                        .param("contactPerson", "")
                        .param("phone", "invalid-phone")
                        .param("address", "")
                        .param("city", "")
                        .param("area", "")
                        .param("pincode", "123") // Invalid pincode
                        .param("peopleServed", "0") // Below minimum
                        .param("foodPreference", "VEG"))
                .andExpect(status().isOk())
                .andExpect(view().name("org/profile"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("GET /org/slots displays upcoming slot calendar and batch creation form")
    void testManageSlotsPage() throws Exception {
        mockMvc.perform(get("/org/slots"))
                .andExpect(status().isOk())
                .andExpect(view().name("org/slots"))
                .andExpect(model().attributeExists("org", "slots", "slotDto", "availableMealSlots"))
                .andExpect(content().string(containsString("Meal Slot Management")));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/slots/create generates batch slots for valid date range and redirects with success message")
    void testCreateSlotsBatchSuccess() throws Exception {
        LocalDate start = LocalDate.now().plusDays(35);
        LocalDate end = LocalDate.now().plusDays(37);

        mockMvc.perform(post("/org/slots/create")
                        .with(csrf())
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("mealSlots", "BREAKFAST", "LUNCH")
                        .param("capacity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/slots"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/slots/create with past date redirects with error message")
    void testCreateSlotsRejectsPastDate() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(2);

        mockMvc.perform(post("/org/slots/create")
                        .with(csrf())
                        .param("startDate", pastDate.toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString())
                        .param("mealSlots", "LUNCH")
                        .param("capacity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/slots"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "pending.test@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("Pending org cannot create slots: redirected to /org/dashboard with restriction message")
    void testPendingOrgCannotCreateSlots() throws Exception {
        LocalDate start = LocalDate.now().plusDays(35);
        LocalDate end = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/org/slots/create")
                        .with(csrf())
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("mealSlots", "LUNCH")
                        .param("capacity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/slots/{id}/capacity updates capacity for an unbooked slot")
    void testUpdateSlotCapacitySuccess() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate futureDate = LocalDate.now().plusDays(40);
        SlotAvailability testSlot = new SlotAvailability(org, futureDate, MealSlot.DINNER, 1, 0);
        testSlot = slotAvailabilityRepository.save(testSlot);

        mockMvc.perform(post("/org/slots/" + testSlot.getId() + "/capacity")
                        .with(csrf())
                        .param("capacity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/slots"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/slots/{id}/delete removes an unbooked slot")
    void testDeleteUnbookedSlotSuccess() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate futureDate = LocalDate.now().plusDays(42);
        SlotAvailability testSlot = new SlotAvailability(org, futureDate, MealSlot.BREAKFAST, 1, 0);
        testSlot = slotAvailabilityRepository.save(testSlot);

        mockMvc.perform(post("/org/slots/" + testSlot.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/slots"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("POST /org/slots/block-date removes all unbooked slots on date")
    void testBlockDateSuccess() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate blockDate = LocalDate.now().plusDays(45);
        SlotAvailability testSlot = new SlotAvailability(org, blockDate, MealSlot.DINNER, 1, 0);
        slotAvailabilityRepository.save(testSlot);

        mockMvc.perform(post("/org/slots/block-date")
                        .with(csrf())
                        .param("date", blockDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/slots"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
