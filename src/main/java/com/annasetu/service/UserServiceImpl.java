package com.annasetu.service;

import com.annasetu.dto.DonorRegistrationDto;
import com.annasetu.dto.OrganizationRegistrationDto;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.Role;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Responsibility:
 * Concrete implementation of {@link UserService} managing user lifecycle and role onboarding.
 *
 * Domain & Architecture Context:
 * - Implements transactional boundary for user and profile persistence.
 * - Enforces security and validation constraints: BCrypt password hashing, email uniqueness,
 *   and initializing newly registered organizations in {@code ApprovalStatus.PENDING}.
 *
 * Interview Talking Points:
 * 1. Atomicity: {@code @Transactional} ensures that the {@link User} entity and corresponding
 *    {@link DonorProfile} or {@link OrganizationProfile} are persisted within a single database transaction.
 * 2. Case-Insensitive Email Normalization: All emails are trimmed and converted to lower case to eliminate
 *    duplicate account creation vulnerabilities.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final OrganizationProfileRepository organizationProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           DonorProfileRepository donorProfileRepository,
                           OrganizationProfileRepository organizationProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.organizationProfileRepository = organizationProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerDonor(DonorRegistrationDto dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists: " + dto.getEmail());
        }

        if (!dto.isPasswordConfirmed()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 1. Create and persist User security identity
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(dto.getPassword()),
                Role.DONOR,
                true
        );
        User savedUser = userRepository.save(user);

        // 2. Create and persist associated DonorProfile
        DonorProfile profile = new DonorProfile(
                savedUser,
                dto.getFullName().trim(),
                dto.getPhone() != null ? dto.getPhone().trim() : null,
                dto.getCity().trim()
        );
        donorProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    @Transactional
    public User registerOrganization(OrganizationRegistrationDto dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists: " + dto.getEmail());
        }

        if (!dto.isPasswordConfirmed()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 1. Create and persist User security identity
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(dto.getPassword()),
                Role.ORGANIZATION,
                true
        );
        User savedUser = userRepository.save(user);

        // 2. Create and persist associated OrganizationProfile in PENDING state
        OrganizationProfile profile = new OrganizationProfile(
                savedUser,
                dto.getName().trim(),
                dto.getType(),
                dto.getAddress().trim(),
                dto.getCity().trim(),
                dto.getArea().trim(),
                dto.getPincode().trim(),
                dto.getContactPerson().trim(),
                dto.getPhone().trim(),
                dto.getPeopleServed(),
                dto.getDescription() != null ? dto.getDescription().trim() : null,
                dto.getFoodPreference(),
                ApprovalStatus.PENDING // Starts PENDING as specified in requirements
        );
        organizationProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
}
