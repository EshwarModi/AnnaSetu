package com.annasetu.service;

import com.annasetu.dto.AdminStatsDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Responsibility:
 * Concrete business service implementation for {@link AdminService}.
 *
 * Domain & Architecture Context:
 * - Executes administrative state transitions on organization vetting and user account access.
 * - Enforces business guards protecting the platform from administrative lockouts (disabling self).
 * - Dispatches transactional email notifications to partner coordinators via {@link NotificationService}.
 *
 * Interview Talking Points:
 * 1. Transactional Governance: Vetting transitions are wrapped in atomic {@code @Transactional} boundaries.
 * 2. Self-Lockout Prevention: Checks authenticated caller email against the target user to guarantee system continuity.
 * 3. Proactive Notifications: Triggers approval / rejection emails as a natural part of the service lifecycle.
 */
@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final OrganizationProfileRepository organizationProfileRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AdminServiceImpl(OrganizationProfileRepository organizationProfileRepository,
                            DonorProfileRepository donorProfileRepository,
                            SlotAvailabilityRepository slotAvailabilityRepository,
                            BookingRepository bookingRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.organizationProfileRepository = organizationProfileRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.slotAvailabilityRepository = slotAvailabilityRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public AdminStatsDto getAdminStats() {
        long totalDonors = donorProfileRepository.count();
        long totalOrgs = organizationProfileRepository.count();
        long approvedOrgs = organizationProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long pendingOrgs = organizationProfileRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long rejectedOrgs = organizationProfileRepository.countByApprovalStatus(ApprovalStatus.REJECTED);
        long totalSlots = slotAvailabilityRepository.count();
        long totalBookings = bookingRepository.count();

        return new AdminStatsDto(
                totalDonors,
                totalOrgs,
                approvedOrgs,
                pendingOrgs,
                rejectedOrgs,
                totalSlots,
                totalBookings
        );
    }

    @Override
    public List<Booking> getRecentBookings(int limit) {
        return bookingRepository.findRecentBookingsWithDetails(PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    public List<OrganizationProfile> getPendingOrganizations() {
        return organizationProfileRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    @Override
    public List<OrganizationProfile> getAllOrganizations() {
        return organizationProfileRepository.findAll();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public OrganizationProfile approveOrganization(Long orgId) {
        OrganizationProfile org = organizationProfileRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        org.setApprovalStatus(ApprovalStatus.APPROVED);
        org.setRejectionReason(null);
        OrganizationProfile savedOrg = organizationProfileRepository.save(org);

        notificationService.sendOrganizationApprovalNotice(savedOrg);
        return savedOrg;
    }

    @Override
    @Transactional
    public OrganizationProfile rejectOrganization(Long orgId, String reason) {
        OrganizationProfile org = organizationProfileRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        org.setApprovalStatus(ApprovalStatus.REJECTED);
        org.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Application requirements not fulfilled.");
        OrganizationProfile savedOrg = organizationProfileRepository.save(org);

        notificationService.sendOrganizationRejectionNotice(savedOrg, reason);
        return savedOrg;
    }

    @Override
    @Transactional
    public User toggleUserStatus(Long userId, String authenticatedAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found with ID: " + userId));

        if (authenticatedAdminEmail != null && user.getEmail().equalsIgnoreCase(authenticatedAdminEmail)) {
            throw new BusinessRuleException("Administrative self-lockout prevented: You cannot disable your own active account.");
        }

        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }
}
