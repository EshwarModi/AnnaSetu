package com.annasetu.repository;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Responsibility:
 * Data access operations for {@link OrganizationProfile} entities.
 *
 * Domain & Architecture Context:
 * - Powers the donor search engine by filtering approved institutions by city, category, and approval state.
 * - Powers dynamic multi-parameter criteria searches via JPA Specification execution.
 * - Powers administrative dashboards to list pending registrations needing verification.
 *
 * Interview Talking Points:
 * 1. Leverages database indexes on {@code city} and {@code approval_status} for high throughput search.
 * 2. Extends {@link JpaSpecificationExecutor} to enable type-safe, composable Criteria API query filtering with pagination.
 * 3. Uses JPQL custom projections (e.g., {@code findDistinctCitiesApproved()}) to efficiently feed dropdown
 *    filters without hydrating full entity graphs.
 */
@Repository
public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, Long>,
        JpaSpecificationExecutor<OrganizationProfile> {

    Optional<OrganizationProfile> findByUserId(Long userId);

    Optional<OrganizationProfile> findByUserEmail(String email);

    List<OrganizationProfile> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<OrganizationProfile> findByCityIgnoreCaseAndApprovalStatus(String city, ApprovalStatus approvalStatus);

    List<OrganizationProfile> findByTypeAndApprovalStatus(OrganizationType type, ApprovalStatus approvalStatus);

    List<OrganizationProfile> findByCityIgnoreCaseAndTypeAndApprovalStatus(String city, OrganizationType type, ApprovalStatus approvalStatus);

    @Query("SELECT DISTINCT o.city FROM OrganizationProfile o WHERE o.approvalStatus = :status ORDER BY o.city ASC")
    List<String> findDistinctCitiesByApprovalStatus(@Param("status") ApprovalStatus status);

    long countByApprovalStatus(ApprovalStatus approvalStatus);
}
