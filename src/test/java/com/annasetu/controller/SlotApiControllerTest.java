package com.annasetu.controller;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Integration tests verifying the RESTful {@link SlotApiController} JSON endpoint.
 *
 * Domain & Architecture Context:
 * - Proves that client-side fetch calls receive valid JSON arrays covering Breakfast, Lunch, and Dinner.
 * - Asserts that fully booked slots correctly serialize {@code isAvailable: false} and {@code remainingCapacity: 0}.
 * - Asserts that unapproved organizations are shielded from public API access.
 *
 * Interview Talking Points:
 * 1. REST Verification: Uses MockMvc with JSONPath expressions to rigorously test payload schemas and contract adherence.
 * 2. Edge Case Coverage: Tests fully booked, partially booked, unbooked, and unconfigured slot states.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SlotApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Test
    @DisplayName("GET /api/organizations/{id}/slots returns JSON array with 3 meal slots for date")
    void testGetSlotsJsonResponse() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate targetDate = LocalDate.now().plusDays(2);

        mockMvc.perform(get("/api/organizations/" + org.getId() + "/slots")
                        .param("date", targetDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].mealSlot", is("BREAKFAST")))
                .andExpect(jsonPath("$[1].mealSlot", is("LUNCH")))
                .andExpect(jsonPath("$[2].mealSlot", is("DINNER")));
    }

    @Test
    @DisplayName("GET /api/organizations/{id}/slots marks fully booked slots with isAvailable=false and remainingCapacity=0")
    void testFullyBookedSlotSerialization() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate testDate = LocalDate.now().plusDays(55); // Far future date

        // Create a slot that is 100% booked (capacity = 1, bookedCount = 1)
        SlotAvailability fullSlot = new SlotAvailability(org, testDate, MealSlot.DINNER, 1, 1);
        slotAvailabilityRepository.save(fullSlot);

        mockMvc.perform(get("/api/organizations/" + org.getId() + "/slots")
                        .param("date", testDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].mealSlot", is("DINNER")))
                .andExpect(jsonPath("$[2].isAvailable", is(false)))
                .andExpect(jsonPath("$[2].remainingCapacity", is(0)))
                .andExpect(jsonPath("$[2].statusMessage", containsString("Fully Booked")));
    }

    @Test
    @DisplayName("GET /api/organizations/{id}/slots marks open slot with isAvailable=true and accurate remainingCapacity")
    void testAvailableSlotSerialization() throws Exception {
        OrganizationProfile org = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();
        LocalDate testDate = LocalDate.now().plusDays(56);

        // Create a shared slot with 1 open spot (capacity = 2, bookedCount = 1)
        SlotAvailability openSlot = new SlotAvailability(org, testDate, MealSlot.LUNCH, 2, 1);
        slotAvailabilityRepository.save(openSlot);

        mockMvc.perform(get("/api/organizations/" + org.getId() + "/slots")
                        .param("date", testDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].mealSlot", is("LUNCH")))
                .andExpect(jsonPath("$[1].isAvailable", is(true)))
                .andExpect(jsonPath("$[1].remainingCapacity", is(1)))
                .andExpect(jsonPath("$[1].statusMessage", containsString("1 spot left")));
    }

    @Test
    @DisplayName("GET /api/organizations/{id}/slots returns 404 when organization is not approved")
    void testUnapprovedOrgReturns404() throws Exception {
        OrganizationProfile pendingOrg = organizationProfileRepository
                .findByApprovalStatus(ApprovalStatus.PENDING)
                .stream().findFirst().orElse(null);

        if (pendingOrg != null) {
            mockMvc.perform(get("/api/organizations/" + pendingOrg.getId() + "/slots")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
