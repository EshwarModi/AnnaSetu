package com.annasetu.service;

import com.annasetu.dto.OrganizationProfileUpdateDto;
import com.annasetu.dto.OrganizationSearchCriteria;
import com.annasetu.dto.OrganizationSearchResultDto;
import com.annasetu.dto.SlotAvailabilityResponseDto;
import com.annasetu.entity.OrganizationProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Responsibility:
 * Business service contract governing organization profiles, verification status checks,
 * profile maintenance, and public discovery search execution with live slot availability queries.
 *
 * Domain & Architecture Context:
 * - Mediates all organizational domain updates, ensuring data integrity and profile consistency.
 * - Powers donor search: executes paginated queries that dynamically verify that only APPROVED institutions
 *   with free matching slots are shown to prospective donors.
 * - Powers real-time slot checking JSON APIs for the organization detail page.
 *
 * Interview Talking Points:
 * 1. DTO-to-Entity Mapping: Populates existing managed entities safely without exposing sensitive
 *    identifiers, foreign keys, or approval statuses to mass-assignment vulnerabilities.
 * 2. Composable Pagination: Returns Spring Data {@link Page} containing read-optimized {@link OrganizationSearchResultDto}
 *    cards to decouple UI rendering from underlying JPA persistence states.
 */
public interface OrganizationService {

    /**
     * Resolves an organization profile associated with the specified User entity identifier.
     */
    Optional<OrganizationProfile> findByUserId(Long userId);

    /**
     * Retrieves an organization profile by its primary key or throws ResourceNotFoundException.
     */
    OrganizationProfile getOrganizationById(Long id);

    /**
     * Retrieves an organization profile by primary key, asserting that it has APPROVED status.
     * Throws ResourceNotFoundException if not found or not approved.
     */
    OrganizationProfile getApprovedOrganizationById(Long id);

    /**
     * Retrieves an organization profile by the authenticated User id or throws ResourceNotFoundException.
     */
    OrganizationProfile getOrganizationByUserId(Long userId);

    /**
     * Updates editable institutional profile attributes from a validated DTO.
     */
    OrganizationProfile updateOrganizationProfile(Long orgId, OrganizationProfileUpdateDto dto);

    /**
     * Checks whether the organization has received administrative verification approval.
     */
    boolean isOrganizationApproved(Long orgId);

    /**
     * Prepopulates an OrganizationProfileUpdateDto from an existing entity instance.
     */
    OrganizationProfileUpdateDto toUpdateDto(OrganizationProfile profile);

    /**
     * Executes donor search queries using JPA Specifications and returns a paginated list
     * of approved organizations that have at least one free slot matching the filters.
     */
    Page<OrganizationSearchResultDto> searchOrganizations(OrganizationSearchCriteria criteria, Pageable pageable);

    /**
     * Retrieves slot availability for an organization on a specific calendar date across all meal windows.
     */
    List<SlotAvailabilityResponseDto> getSlotAvailabilityForDate(Long organizationId, LocalDate date);

    /**
     * Lists distinct cities where approved organizations operate.
     */
    List<String> getAvailableCities();
}
