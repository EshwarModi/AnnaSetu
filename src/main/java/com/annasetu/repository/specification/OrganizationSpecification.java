package com.annasetu.repository.specification;

import com.annasetu.dto.OrganizationSearchCriteria;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.FoodPreference;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility:
 * Composable JPA Criteria Specification encapsulating dynamic multi-attribute queries for verified organizations
 * with active, unbooked meal availability.
 *
 * Domain & Architecture Context:
 * - Solves the core platform discovery problem: Donors search across geography, meal types, and dates,
 *   and must ONLY see organizations that currently have free spots (bookedCount < capacity).
 * - Enforces business security invariant: Unapproved or rejected organizations are strictly excluded.
 *
 * Interview Talking Points:
 * 1. Subquery vs. Distinct Join: Employs a correlated {@code EXISTS (subquery)} on {@link SlotAvailability}
 *    rather than a SQL {@code JOIN} + {@code DISTINCT}. This completely prevents duplicate rows from bloating
 *    pagination count queries and avoids in-memory pagination warnings.
 * 2. Type-Safe Criteria API: Constructs dynamic SQL where-clauses safely without SQL injection risks.
 * 3. Dietary Compatibility: Expands diet queries (e.g., VEG matches both VEG-exclusive and BOTH facilities).
 */
public final class OrganizationSpecification {

    private OrganizationSpecification() {
        // Utility specification class
    }

    public static Specification<OrganizationProfile> withFilters(OrganizationSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Invariant: Only APPROVED organizations are visible in public discovery
            predicates.add(cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));

            if (criteria != null) {
                // 2. City filter (case-insensitive)
                if (StringUtils.hasText(criteria.getCity())) {
                    predicates.add(cb.equal(cb.lower(root.get("city")), criteria.getCity().trim().toLowerCase()));
                }

                // 3. Area or Pincode filter
                if (StringUtils.hasText(criteria.getAreaOrPincode())) {
                    String term = criteria.getAreaOrPincode().trim().toLowerCase();
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("area")), "%" + term + "%"),
                            cb.equal(root.get("pincode"), criteria.getAreaOrPincode().trim())
                    ));
                }

                // 4. Institutional Category Type
                if (criteria.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), criteria.getType()));
                }

                // 5. Dietary Compliance
                if (criteria.getFoodPreference() != null) {
                    if (criteria.getFoodPreference() == FoodPreference.VEG) {
                        predicates.add(root.get("foodPreference").in(FoodPreference.VEG, FoodPreference.BOTH));
                    } else if (criteria.getFoodPreference() == FoodPreference.NON_VEG) {
                        predicates.add(root.get("foodPreference").in(FoodPreference.NON_VEG, FoodPreference.BOTH));
                    } else {
                        predicates.add(root.get("foodPreference").in(FoodPreference.VEG, FoodPreference.NON_VEG, FoodPreference.BOTH));
                    }
                }

                // 6. Mandatory Free Slot Subquery:
                // An institution is only returned if it possesses at least ONE slot matching the criteria
                // where bookedCount < capacity.
                Subquery<Long> slotSubquery = query.subquery(Long.class);
                Root<SlotAvailability> slotRoot = slotSubquery.from(SlotAvailability.class);

                List<Predicate> slotPredicates = new ArrayList<>();
                slotPredicates.add(cb.equal(slotRoot.get("organization"), root));
                slotPredicates.add(cb.lt(slotRoot.get("bookedCount"), slotRoot.get("capacity")));

                LocalDate targetDate = criteria.getDate();
                if (targetDate != null) {
                    slotPredicates.add(cb.equal(slotRoot.get("slotDate"), targetDate));
                } else {
                    // Default: upcoming availability from today onwards
                    slotPredicates.add(cb.greaterThanOrEqualTo(slotRoot.get("slotDate"), LocalDate.now()));
                }

                if (criteria.getMealSlot() != null) {
                    slotPredicates.add(cb.equal(slotRoot.get("mealSlot"), criteria.getMealSlot()));
                }

                slotSubquery.select(slotRoot.get("id"))
                        .where(cb.and(slotPredicates.toArray(new Predicate[0])));

                predicates.add(cb.exists(slotSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
