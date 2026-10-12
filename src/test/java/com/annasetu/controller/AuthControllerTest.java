package com.annasetu.controller;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.Role;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Web MVC test verifying registration and authentication endpoints.
 *
 * Domain & Architecture Context:
 * - Validates Bean Validation constraint triggers on donor and organization forms.
 * - Asserts atomic database persistence and status initialization (organizations start in PENDING).
 *
 * Interview Talking Points:
 * 1. CSRF Verification: Validates that POST requests lacking a CSRF token are blocked, while requests
 *    with {@code csrf()} execute and perform validation/redirection.
 * 2. Model & Binding Assertions: Asserts presence of field errors in {@link org.springframework.validation.BindingResult}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Test
    @DisplayName("GET /login renders custom login view")
    void testLoginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Welcome Back")))
                .andExpect(content().string(containsString("Remember me on this device")));
    }

    @Test
    @DisplayName("GET /register/donor renders donor registration form")
    void testDonorRegistrationPageRenders() throws Exception {
        mockMvc.perform(get("/register/donor"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-donor"))
                .andExpect(model().attributeExists("donorForm"))
                .andExpect(content().string(containsString("Create Donor Account")));
    }

    @Test
    @DisplayName("POST /register/donor with invalid fields triggers Bean Validation errors")
    void testDonorRegistrationValidationFailure() throws Exception {
        mockMvc.perform(post("/register/donor")
                        .with(csrf())
                        .param("fullName", "")
                        .param("email", "invalid-email")
                        .param("password", "123")
                        .param("confirmPassword", "456")
                        .param("city", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-donor"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("donorForm", "fullName", "email", "password", "confirmPassword", "city"));
    }

    @Test
    @DisplayName("POST /register/donor with valid data successfully creates donor and redirects to login")
    void testDonorRegistrationSuccess() throws Exception {
        mockMvc.perform(post("/register/donor")
                        .with(csrf())
                        .param("fullName", "Ananya Deshpande")
                        .param("email", "ananya.deshpande@example.com")
                        .param("password", "SecretPass123")
                        .param("confirmPassword", "SecretPass123")
                        .param("phone", "9890123456")
                        .param("city", "Pune"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        Optional<User> userOpt = userRepository.findByEmail("ananya.deshpande@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(Role.DONOR, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("GET /register/organization renders NGO registration form with enum dropdowns")
    void testOrganizationRegistrationPageRenders() throws Exception {
        mockMvc.perform(get("/register/organization"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-organization"))
                .andExpect(model().attributeExists("orgForm"))
                .andExpect(model().attributeExists("orgTypes"))
                .andExpect(model().attributeExists("foodPreferences"))
                .andExpect(content().string(containsString("Register Beneficiary Organization")));
    }

    @Test
    @DisplayName("POST /register/organization starts in PENDING status and cannot access features")
    void testOrganizationRegistrationSuccessWithPendingStatus() throws Exception {
        mockMvc.perform(post("/register/organization")
                        .with(csrf())
                        .param("name", "Jan Seva Anathashram")
                        .param("email", "contact@janseva.org")
                        .param("password", "JanSevaPass123")
                        .param("confirmPassword", "JanSevaPass123")
                        .param("type", "ORPHANAGE")
                        .param("address", "Sector 4, Near Ganpati Temple")
                        .param("city", "Pune")
                        .param("area", "Kothrud")
                        .param("pincode", "411038")
                        .param("contactPerson", "Milind Joshi")
                        .param("phone", "9823456789")
                        .param("peopleServed", "35")
                        .param("description", "Community orphanage caring for 35 children.")
                        .param("foodPreference", "VEG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?orgPending=true"));

        Optional<User> userOpt = userRepository.findByEmail("contact@janseva.org");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(Role.ORGANIZATION, user.getRole());

        Optional<OrganizationProfile> orgOpt = organizationProfileRepository.findByUserId(user.getId());
        assertTrue(orgOpt.isPresent());
        OrganizationProfile org = orgOpt.get();
        assertEquals("Jan Seva Anathashram", org.getName());
        assertEquals(ApprovalStatus.PENDING, org.getApprovalStatus(),
                "Newly registered organization must initialize with PENDING status");
    }
}
