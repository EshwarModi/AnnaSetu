package com.annasetu.repository;

import com.annasetu.entity.User;
import com.annasetu.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Responsibility:
 * Data access operations for {@link User} entities.
 *
 * Domain & Architecture Context:
 * - Supplies primary authentication lookups required by Spring Security's UserDetailsService.
 * - Handles email uniqueness checks during donor and organization registration.
 *
 * Interview Talking Points:
 * 1. Leverages Spring Data JPA derived query methods (e.g., {@code findByEmail}), which generate
 *    optimized parameterized queries avoiding manual JDBC boilerplate.
 * 2. Returns {@link Optional} to promote null-safety across service layers.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndEnabledTrue(String email);

    long countByRole(Role role);
}
