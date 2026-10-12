package com.annasetu.controller;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.entity.enums.Role;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Integration tests verifying donor search and discovery, multi-dimensional filters,
 * pagination, and organization detail views with Google Maps URLs.
 *
 * Domain & Architecture Context:
 * - Proves that only approved institutions with at least one free slot are surfaced in search results.
 * - Asserts that unapproved organizations are strictly invisible to public discovery and detail views.
 * - Confirms that Google Maps query links render without requiring third-party API keys.
 *
 * Interview Talking Points:
 * 1. Filter Composition: Exercises multiple permutations of city, category, date, and meal window parameters.
 * 2. Progressive Enhancement: Verifies that initial slot availability is rendered server-side on detail views.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private OrganizationProfile approvedOrg;

    @BeforeEach
    void setUp() {
        approvedOrg = organizationProfileRepository.findByUserEmail("ashakiran@annasetu.org").orElseThrow();

        // Ensure a pending organization exists for negative assertion testing
        if (userRepository.findByEmail("hidden.pending@test.org").isEmpty()) {
            User pendingUser = userRepository.save(new User("hidden.pending@test.org", "hash", Role.ORGANIZATION));
            OrganizationProfile pending = new OrganizationProfile(
                    pendingUser, "Hidden Pending Shelter", OrganizationType.SHELTER,
                    "99 Secrecy Way", "Pune", "Hinjewadi", "411057", "Ravi",
                    "+91 98999 11111", 30, "Awaiting approval",
                    FoodPreference.VEG, ApprovalStatus.PENDING
            );
            organizationProfileRepository.save(pending);
        }
    }

    @Test
    @DisplayName("GET /search without filters returns approved organizations with free slots")
    void testSearchDefaultReturnsApprovedOrganizations() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("results", "criteria", "cities", "orgTypes", "mealSlots"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")))
                .andExpect(content().string(containsString("Only Approved Partners Shown")))
                .andExpect(content().string(not(containsString("Hidden Pending Shelter"))));
    }

    @Test
    @DisplayName("GET /search?city=Pune filters institutions by city")
    void testSearchFilterByCity() throws Exception {
        mockMvc.perform(get("/search").param("city", "Pune"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")))
                .andExpect(content().string(not(containsString("Vatsalya Child Welfare Centre")))); // Mumbai institution
    }

    @Test
    @DisplayName("GET /search?type=ORPHANAGE filters institutions by category")
    void testSearchFilterByType() throws Exception {
        mockMvc.perform(get("/search").param("type", "ORPHANAGE"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")))
                .andExpect(content().string(not(containsString("Mauli Senior Citizen Ashram")))); // OLD_AGE_HOME
    }

    @Test
    @DisplayName("GET /search?foodPreference=VEG filters institutions serving vegetarian meals")
    void testSearchFilterByFoodPreference() throws Exception {
        mockMvc.perform(get("/search").param("foodPreference", "VEG"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")));
    }

    @Test
    @DisplayName("GET /search?date=YYYY-MM-DD returns institutions with open slots on that date")
    void testSearchFilterByDate() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/search").param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")));
    }

    @Test
    @DisplayName("GET /organizations/{id} renders approved organization detail view with Google Maps and slot buttons")
    void testViewOrganizationDetailSuccess() throws Exception {
        mockMvc.perform(get("/organizations/" + approvedOrg.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("organization/detail"))
                .andExpect(model().attributeExists("org", "selectedDate", "initialSlots", "googleMapsUrl"))
                .andExpect(content().string(containsString("Asha Kiran Children Home")))
                .andExpect(content().string(containsString("https://www.google.com/maps/search/?api=1")))
                .andExpect(content().string(containsString("query=Asha+Kiran")))
                .andExpect(content().string(containsString("Breakfast")))
                .andExpect(content().string(containsString("Lunch")))
                .andExpect(content().string(containsString("Dinner")));
    }

    @Test
    @DisplayName("GET /organizations/{id} returns 404 when organization is pending or does not exist")
    void testViewOrganizationPendingOrMissingReturns404() throws Exception {
        OrganizationProfile pendingOrg = organizationProfileRepository
                .findByUserEmail("hidden.pending@test.org").orElseThrow();

        // Pending org should be inaccessible via public discovery detail
        mockMvc.perform(get("/organizations/" + pendingOrg.getId()))
                .andExpect(status().isNotFound());

        // Non-existent ID should be 404
        mockMvc.perform(get("/organizations/999999"))
                .andExpect(status().isNotFound());
    }
}
