package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Defines the user authorization roles supported in the AnnaSetu platform.
 *
 * Domain & Architecture Context:
 * - DONOR: End-user who seeks meal donation slots to sponsor food on special occasions.
 * - ORGANIZATION: Beneficiary institution (orphanage, shelter, old age home) managing slots.
 * - ADMIN: System administrator responsible for verifying and approving organizations.
 *
 * Interview Talking Point:
 * Implements Role-Based Access Control (RBAC). In Spring Security 6, these map cleanly to
 * authority roles (e.g., 'ROLE_' + role.name() or hasRole('DONOR')).
 */
public enum Role {
    DONOR,
    ORGANIZATION,
    ADMIN
}
