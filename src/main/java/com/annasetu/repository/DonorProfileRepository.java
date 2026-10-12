package com.annasetu.repository;

import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Responsibility:
 * Data access operations for {@link DonorProfile} entities.
 *
 * Domain & Architecture Context:
 * - Bridges security identity (User) with donor profile attributes (name, phone, city).
 * - Queried when an authenticated donor accesses their dashboard or initiates a booking.
 *
 * Interview Talking Points:
 * 1. Supports traversal queries such as {@code findByUserEmail(String email)} which leverage
 *    Hibernate's nested property resolution (`user.email`).
 * 2. Strict 1:1 binding ensures one user profile per donor identity.
 */
@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUserId(Long userId);

    Optional<DonorProfile> findByUser(User user);

    Optional<DonorProfile> findByUserEmail(String email);
}
