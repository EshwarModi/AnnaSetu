package com.annasetu.service;

import com.annasetu.dto.AdminStatsDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.Role;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit test suite validating administrative business rules in {@link AdminServiceImpl}.
 *
 * Domain & Architecture Context:
 * - Asserts metric aggregation calculation across platform entities.
 * - Asserts institutional partner approval and rejection state changes with reason recording.
 * - Asserts user access status toggling and self-disabling lockout protection invariant.
 *
 * Interview Talking Points:
 * 1. Boundary & Guard Testing: Tests that administrators cannot inadvertently disable themselves.
 * 2. Notification Integration: Verifies that vetting events dispatch coordinator notices.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private OrganizationProfileRepository organizationProfileRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private OrganizationProfile pendingOrg;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        pendingOrg = new OrganizationProfile();
        pendingOrg.setId(10L);
        pendingOrg.setName("Shelter Hope");
        pendingOrg.setApprovalStatus(ApprovalStatus.PENDING);

        testUser = new User("donor@test.org", "hash", Role.DONOR, true);
        testUser.setId(100L);

        adminUser = new User("admin@annasetu.org", "hash", Role.ADMIN, true);
        adminUser.setId(1L);
    }

    @Test
    @DisplayName("getAdminStats() computes correct platform metric aggregates")
    void testGetAdminStats() {
        when(donorProfileRepository.count()).thenReturn(5L);
        when(organizationProfileRepository.count()).thenReturn(8L);
        when(organizationProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED)).thenReturn(7L);
        when(organizationProfileRepository.countByApprovalStatus(ApprovalStatus.PENDING)).thenReturn(1L);
        when(organizationProfileRepository.countByApprovalStatus(ApprovalStatus.REJECTED)).thenReturn(0L);
        when(slotAvailabilityRepository.count()).thenReturn(720L);
        when(bookingRepository.count()).thenReturn(15L);

        AdminStatsDto stats = adminService.getAdminStats();

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalDonors());
        assertEquals(8L, stats.getTotalOrganizations());
        assertEquals(7L, stats.getApprovedOrgsCount());
        assertEquals(1L, stats.getPendingOrgsCount());
        assertEquals(0L, stats.getRejectedOrgsCount());
        assertEquals(720L, stats.getTotalSlots());
        assertEquals(15L, stats.getTotalBookings());
    }

    @Test
    @DisplayName("approveOrganization() updates status to APPROVED, clears rejection reason, and notifies coordinator")
    void testApproveOrganization_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(pendingOrg));
        when(organizationProfileRepository.save(any(OrganizationProfile.class))).thenAnswer(i -> i.getArgument(0));

        OrganizationProfile approved = adminService.approveOrganization(10L);

        assertEquals(ApprovalStatus.APPROVED, approved.getApprovalStatus());
        assertNull(approved.getRejectionReason());
        verify(organizationProfileRepository).save(pendingOrg);
        verify(notificationService).sendOrganizationApprovalNotice(approved);
    }

    @Test
    @DisplayName("rejectOrganization() updates status to REJECTED, records rejection reason, and notifies coordinator")
    void testRejectOrganization_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(pendingOrg));
        when(organizationProfileRepository.save(any(OrganizationProfile.class))).thenAnswer(i -> i.getArgument(0));

        String reason = "Registration address could not be verified";
        OrganizationProfile rejected = adminService.rejectOrganization(10L, reason);

        assertEquals(ApprovalStatus.REJECTED, rejected.getApprovalStatus());
        assertEquals(reason, rejected.getRejectionReason());
        verify(organizationProfileRepository).save(pendingOrg);
        verify(notificationService).sendOrganizationRejectionNotice(rejected, reason);
    }

    @Test
    @DisplayName("toggleUserStatus() toggles enabled state from true to false")
    void testToggleUserStatus_Disable() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User toggled = adminService.toggleUserStatus(100L, "admin@annasetu.org");

        assertFalse(toggled.isEnabled(), "User should now be disabled");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("toggleUserStatus() throws BusinessRuleException when admin attempts self-disabling")
    void testToggleUserStatus_SelfLockout_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(BusinessRuleException.class, () ->
                adminService.toggleUserStatus(1L, "admin@annasetu.org")
        );
        verify(userRepository, never()).save(any(User.class));
    }
}
