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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Integration test suite verifying Administrator Console endpoints:
 * metrics overview, pending organization vetting approvals and rejections, and user access toggling.
 *
 * Domain & Architecture Context:
 * - Proves that only administrators ({@code ROLE_ADMIN}) can access {@code /admin/**}.
 * - Asserts Post/Redirect/Get flow and flash attributes across vetting decisions and account toggling.
 *
 * Interview Talking Points:
 * 1. RBAC Isolation: Verifies that unauthorized users (anonymous or donors) are blocked by Spring Security.
 * 2. PRG Verification: Asserts that successful status updates redirect cleanly with feedback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    private OrganizationProfile testOrg;
    private User regularUser;

    @BeforeEach
    void setUp() {
        User user = userRepository.findByEmail("test.pending.org@test.org").orElseGet(() ->
                userRepository.save(new User("test.pending.org@test.org", "hash", Role.ORGANIZATION, true))
        );

        testOrg = organizationProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            OrganizationProfile org = new OrganizationProfile(
                    user,
                    "Pending Test Shelter",
                    OrganizationType.SHELTER,
                    "789 Test Lane",
                    "Pune",
                    "Kothrud",
                    "411038",
                    "Coordinator Test",
                    "+91 99000 11223",
                    40,
                    "A shelter offering meals to those in need.",
                    FoodPreference.VEG,
                    ApprovalStatus.PENDING
            );
            return organizationProfileRepository.save(org);
        });

        regularUser = userRepository.findByEmail("test.regular.user@test.org").orElseGet(() ->
                userRepository.save(new User("test.regular.user@test.org", "hash", Role.DONOR, true))
        );
    }

    @Test
    @DisplayName("GET /admin/dashboard - Authorized administrator accesses dashboard with metrics")
    @WithMockUser(username = "admin@annasetu.org", roles = "ADMIN")
    void testAdminDashboard_Authorized() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats", "recentBookings", "pendingOrgs", "allOrgs", "allUsers"));
    }

    @Test
    @DisplayName("GET /admin/dashboard - Unauthenticated access redirects to login")
    void testAdminDashboard_Unauthenticated_Redirects() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /admin/dashboard - Donor access forbidden (403)")
    @WithMockUser(username = "rahul.sharma@example.com", roles = "DONOR")
    void testAdminDashboard_Donor_Forbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/organizations/{id}/approve - Approves pending organization and redirects")
    @WithMockUser(username = "admin@annasetu.org", roles = "ADMIN")
    void testApproveOrganization_Success() throws Exception {
        mockMvc.perform(post("/admin/organizations/" + testOrg.getId() + "/approve")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /admin/organizations/{id}/reject - Rejects organization with reason and redirects")
    @WithMockUser(username = "admin@annasetu.org", roles = "ADMIN")
    void testRejectOrganization_Success() throws Exception {
        mockMvc.perform(post("/admin/organizations/" + testOrg.getId() + "/reject")
                        .with(csrf())
                        .param("reason", "Incomplete background documentation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/toggle-status - Toggles user account active/disabled status")
    @WithMockUser(username = "admin@annasetu.org", roles = "ADMIN")
    void testToggleUserStatus_Success() throws Exception {
        mockMvc.perform(post("/admin/users/" + regularUser.getId() + "/toggle-status")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
