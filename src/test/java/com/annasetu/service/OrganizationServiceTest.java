package com.annasetu.service;

import com.annasetu.dto.OrganizationProfileUpdateDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.repository.OrganizationProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit tests validating profile updates, security immutability, and approval state queries in {@link OrganizationServiceImpl}.
 *
 * Domain & Architecture Context:
 * - Ensures that sensitive attributes (user reference, approval status, database identity) are untouched during updates.
 * - Asserts that editable presentation attributes correctly map to the persistent aggregate root.
 *
 * Interview Talking Points:
 * 1. Immutability Verification: Confirms that mass-assignment attempts cannot escalate privileges or flip approvalStatus.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationProfileRepository organizationProfileRepository;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private OrganizationProfile existingProfile;
    private User orgUser;

    @BeforeEach
    void setUp() {
        orgUser = new User("org@annasetu.org", "secretHash", com.annasetu.entity.enums.Role.ORGANIZATION);
        orgUser.setId(100L);

        existingProfile = new OrganizationProfile(
                orgUser, "Asha Home", OrganizationType.ORPHANAGE, "123 Old St", "Pune",
                "Kothrud", "411038", "Sunita", "9822012345", 60, "Description",
                FoodPreference.VEG, ApprovalStatus.APPROVED
        );
        existingProfile.setId(10L);
    }

    @Test
    @DisplayName("updateOrganizationProfile: Updates editable fields while preserving user and approval status")
    void testUpdateProfile_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(existingProfile));
        when(organizationProfileRepository.save(any(OrganizationProfile.class))).thenAnswer(i -> i.getArgument(0));

        OrganizationProfileUpdateDto dto = new OrganizationProfileUpdateDto();
        dto.setName("Asha Kiran Rehabilitation Center");
        dto.setType(OrganizationType.SHELTER);
        dto.setContactPerson("Ramesh Deshmukh");
        dto.setPhone("+91 98220 99999");
        dto.setAddress("456 New Road");
        dto.setCity("Mumbai");
        dto.setArea("Andheri");
        dto.setPincode("400058");
        dto.setPeopleServed(85);
        dto.setFoodPreference(FoodPreference.BOTH);
        dto.setDescription("Updated mission statement");

        OrganizationProfile updated = organizationService.updateOrganizationProfile(10L, dto);

        assertEquals("Asha Kiran Rehabilitation Center", updated.getName());
        assertEquals(OrganizationType.SHELTER, updated.getType());
        assertEquals("Ramesh Deshmukh", updated.getContactPerson());
        assertEquals("+91 98220 99999", updated.getPhone());
        assertEquals("456 New Road", updated.getAddress());
        assertEquals("Mumbai", updated.getCity());
        assertEquals("Andheri", updated.getArea());
        assertEquals("400058", updated.getPincode());
        assertEquals(85, updated.getPeopleServed());
        assertEquals(FoodPreference.BOTH, updated.getFoodPreference());
        assertEquals("Updated mission statement", updated.getDescription());

        // Invariant: ID, User, and ApprovalStatus MUST remain unaltered
        assertEquals(10L, updated.getId());
        assertEquals(orgUser, updated.getUser());
        assertEquals(ApprovalStatus.APPROVED, updated.getApprovalStatus());

        verify(organizationProfileRepository).save(existingProfile);
    }

    @Test
    @DisplayName("isOrganizationApproved: Returns true for APPROVED and false for PENDING")
    void testIsOrganizationApproved() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(existingProfile));

        OrganizationProfile pendingOrg = new OrganizationProfile();
        pendingOrg.setId(20L);
        pendingOrg.setApprovalStatus(ApprovalStatus.PENDING);
        when(organizationProfileRepository.findById(20L)).thenReturn(Optional.of(pendingOrg));

        assertTrue(organizationService.isOrganizationApproved(10L));
        assertFalse(organizationService.isOrganizationApproved(20L));
    }
}
