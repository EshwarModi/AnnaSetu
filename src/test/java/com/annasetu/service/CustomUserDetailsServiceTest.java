package com.annasetu.service;

import com.annasetu.config.CustomUserDetails;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.Role;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit tests for {@link CustomUserDetailsService} isolating authentication principal loading.
 *
 * Domain & Architecture Context:
 * - Tests lookup of User entities by normalized email.
 * - Asserts polymorphic enrichment for DONOR, ORGANIZATION, and ADMIN roles into {@link CustomUserDetails}.
 * - Verifies correct exception throwing when an unknown username/email is provided.
 *
 * Interview Talking Points:
 * 1. Fast Isolated Unit Tests: Uses Mockito without loading Spring ApplicationContext.
 * 2. Role-Based Identity Synthesis: Verifies that custom claims (displayName, profileId, approvalStatus)
 *    are populated into the SecurityContext principal for UI rendering.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private OrganizationProfileRepository organizationProfileRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User donorUser;
    private User orgUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        donorUser = new User("donor@test.org", "encodedPass", Role.DONOR, true);
        donorUser.setId(101L);

        orgUser = new User("org@test.org", "encodedPass", Role.ORGANIZATION, true);
        orgUser.setId(102L);

        adminUser = new User("admin@test.org", "encodedPass", Role.ADMIN, true);
        adminUser.setId(103L);
    }

    @Test
    @DisplayName("loadUserByUsername with DONOR email populates donor display name and profile ID")
    void testLoadUserByUsername_Donor() {
        DonorProfile donor = new DonorProfile(donorUser, "Aarav Patel", "+91 98765 43210", "Pune");
        donor.setId(501L);

        when(userRepository.findByEmail("donor@test.org")).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.findByUserId(101L)).thenReturn(Optional.of(donor));

        UserDetails userDetails = userDetailsService.loadUserByUsername("donor@test.org");

        assertNotNull(userDetails);
        assertInstanceOf(CustomUserDetails.class, userDetails);
        CustomUserDetails cud = (CustomUserDetails) userDetails;
        assertEquals("Aarav Patel", cud.getDisplayName());
        assertEquals(501L, cud.getProfileId());
        assertEquals(Role.DONOR, cud.getRole());
    }

    @Test
    @DisplayName("loadUserByUsername with ORGANIZATION email populates org name and approval status")
    void testLoadUserByUsername_Organization() {
        OrganizationProfile org = new OrganizationProfile();
        org.setId(601L);
        org.setName("Care Shelter Pune");
        org.setApprovalStatus(ApprovalStatus.APPROVED);

        when(userRepository.findByEmail("org@test.org")).thenReturn(Optional.of(orgUser));
        when(organizationProfileRepository.findByUserId(102L)).thenReturn(Optional.of(org));

        UserDetails userDetails = userDetailsService.loadUserByUsername("org@test.org");

        assertNotNull(userDetails);
        CustomUserDetails cud = (CustomUserDetails) userDetails;
        assertEquals("Care Shelter Pune", cud.getDisplayName());
        assertEquals(601L, cud.getProfileId());
        assertEquals(ApprovalStatus.APPROVED, cud.getApprovalStatus());
        assertEquals(Role.ORGANIZATION, cud.getRole());
    }

    @Test
    @DisplayName("loadUserByUsername with ADMIN email sets Administrator display name")
    void testLoadUserByUsername_Admin() {
        when(userRepository.findByEmail("admin@test.org")).thenReturn(Optional.of(adminUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@test.org");

        assertNotNull(userDetails);
        CustomUserDetails cud = (CustomUserDetails) userDetails;
        assertEquals("Administrator", cud.getDisplayName());
        assertEquals(Role.ADMIN, cud.getRole());
    }

    @Test
    @DisplayName("loadUserByUsername with non-existent email throws UsernameNotFoundException")
    void testLoadUserByUsername_NotFound() {
        when(userRepository.findByEmail("unknown@test.org")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("unknown@test.org")
        );
    }
}
