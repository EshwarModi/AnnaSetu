package com.annasetu.service;

import com.annasetu.dto.DonorRegistrationDto;
import com.annasetu.dto.OrganizationRegistrationDto;
import com.annasetu.entity.User;

import java.util.Optional;

/**
 * Responsibility:
 * Business service contract for user authentication identities and role registration workflows.
 *
 * Domain & Architecture Context:
 * - Coordinates transactional creation of {@link User} identities alongside {@link com.annasetu.entity.DonorProfile}
 *   and {@link com.annasetu.entity.OrganizationProfile}.
 * - Guarantees BCrypt password encoding prior to persistence.
 * - Enforces business rules such as email uniqueness and initial organization {@code PENDING} approval state.
 *
 * Interview Talking Points:
 * 1. Transactional Integrity: Registration operations are wrapped in {@code @Transactional} so if profile
 *    creation fails, the user record is automatically rolled back, preventing orphaned credentials.
 * 2. Strict Password Hashing: Password strings are never stored or logged in plain text.
 */
public interface UserService {

    User registerDonor(DonorRegistrationDto dto);

    User registerOrganization(OrganizationRegistrationDto dto);

    boolean isEmailTaken(String email);

    Optional<User> findByEmail(String email);
}
