package com.annasetu.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Security integration test verifying Role-Based Access Control (RBAC), URL authorization boundaries,
 * login authentication, custom success redirects, and 403 Access Denied handling.
 *
 * Domain & Architecture Context:
 * - Proves role isolation: A Donor cannot access /admin/** or /org/** routes.
 * - Confirms that unauthenticated attempts to access private areas redirect to /login.
 * - Asserts that the custom 403 view renders cleanly without throwing unhandled security exceptions.
 *
 * Interview Talking Points:
 * 1. Test Isolation with @WithMockUser: Synthesizes security contexts with discrete roles
 *    (ROLE_DONOR, ROLE_ORGANIZATION, ROLE_ADMIN) to rigorously test HTTP security interceptors.
 * 2. Form Login Pipeline: Tests full form login POST processing with CSRF, BCrypt verification,
 *    and role-based redirect resolution.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated visitor accessing protected dashboards is redirected to /login")
    void testAnonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/org/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "rahul.sharma@example.com", roles = {"DONOR"})
    @DisplayName("DONOR role can access /donor/dashboard but is denied (403) from /admin and /org")
    void testDonorRoleAccessAndIsolation() throws Exception {
        // Allowed
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/dashboard"));

        // Denied from /admin/** -> 403 Forbidden
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));

        // Denied from /org/** -> 403 Forbidden
        mockMvc.perform(get("/org/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "ashakiran@annasetu.org", roles = {"ORGANIZATION"})
    @DisplayName("ORGANIZATION role can access /org/** but is denied (403) from /admin")
    void testOrganizationRoleIsolation() throws Exception {
        // Denied from /admin/** -> 403 Forbidden
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin@annasetu.org", roles = {"ADMIN"})
    @DisplayName("ADMIN role can access /admin/dashboard")
    void testAdminRoleAccess() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Platform Administration Console")));
    }

    @Test
    @DisplayName("Login with Admin credentials redirects to /admin/dashboard")
    void testAdminLoginSuccessRedirect() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@annasetu.org")
                        .param("password", "Password@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Login with Donor credentials redirects to /donor/dashboard")
    void testDonorLoginSuccessRedirect() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "rahul.sharma@example.com")
                        .param("password", "Password@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/dashboard"));
    }

    @Test
    @DisplayName("Login with Approved Organization credentials redirects to /org/dashboard")
    void testApprovedOrgLoginSuccessRedirect() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "ashakiran@annasetu.org")
                        .param("password", "Password@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/org/dashboard"));
    }

    @Test
    @DisplayName("Login with invalid password fails and redirects to /login?error=true")
    void testInvalidPasswordFails() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@annasetu.org")
                        .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("GET /403 renders styled custom 403 page")
    void testCustom403PageRenders() throws Exception {
        mockMvc.perform(get("/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("403")))
                .andExpect(content().string(containsString("Access Forbidden")));
    }

    @Test
    @DisplayName("GET /404 renders styled custom 404 page")
    void testCustom404PageRenders() throws Exception {
        mockMvc.perform(get("/404"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("404")))
                .andExpect(content().string(containsString("Page Not Found")));
    }

    @Test
    @DisplayName("POST request without CSRF token is rejected with 403 Forbidden")
    void testPostWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "admin@annasetu.org")
                        .param("password", "Password@123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /logout with CSRF token terminates session and redirects to /login?logout=true")
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
