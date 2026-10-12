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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsibility:
 * Authenticates users against the database by implementing Spring Security's {@link UserDetailsService}.
 *
 * Domain & Architecture Context:
 * - Loads credentials and profile metadata when a user submits the login form.
 * - Enriches the {@link UserDetails} principal with domain profile IDs and approval status.
 *
 * Interview Talking Points:
 * 1. Read-Only Transactional Boundary: Uses {@code @Transactional(readOnly = true)} to ensure all associated
 *    profile lookups occur within a single atomic database read session.
 * 2. Polymorphic Identity Loading: Detects user role and retrieves specific donor/organization identity
 *    seamlessly, avoiding runtime cast errors.
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final OrganizationProfileRepository organizationProfileRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    DonorProfileRepository donorProfileRepository,
                                    OrganizationProfileRepository organizationProfileRepository) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.organizationProfileRepository = organizationProfileRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with email: " + email));

        String displayName = user.getEmail();
        Long profileId = null;
        ApprovalStatus approvalStatus = null;

        if (user.getRole() == Role.DONOR) {
            DonorProfile donor = donorProfileRepository.findByUserId(user.getId()).orElse(null);
            if (donor != null) {
                displayName = donor.getFullName();
                profileId = donor.getId();
            }
        } else if (user.getRole() == Role.ORGANIZATION) {
            OrganizationProfile org = organizationProfileRepository.findByUserId(user.getId()).orElse(null);
            if (org != null) {
                displayName = org.getName();
                profileId = org.getId();
                approvalStatus = org.getApprovalStatus();
            }
        } else if (user.getRole() == Role.ADMIN) {
            displayName = "Administrator";
        }

        return new CustomUserDetails(user, displayName, profileId, approvalStatus);
    }
}
