package com.annasetu.repository;

import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.MealSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Responsibility:
 * Data access operations for {@link SlotAvailability} entities.
 *
 * Domain & Architecture Context:
 * - Fetches monthly calendar schedules for organizations.
 * - Locates available slots where {@code bookedCount < capacity} for donor booking flows.
 * - Resolves slot conflicts using composite keys (organizationId, slotDate, mealSlot).
 * - Queries upcoming slots and reservations for organization slot management workspaces.
 *
 * Interview Talking Points:
 * 1. Leverages the compound index {@code (organization_id, slot_date)} for high-speed calendar range queries.
 * 2. Uses JPQL queries with explicit date ranges and capacity filtering to avoid loading unnecessary
 *    historical or fully booked records.
 * 3. Derived query methods provide flexible filtering for organization management dashboards.
 */
@Repository
public interface SlotAvailabilityRepository extends JpaRepository<SlotAvailability, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlotAvailability s WHERE s.id = :id")
    Optional<SlotAvailability> findByIdWithPessimisticLock(@Param("id") Long id);

    List<SlotAvailability> findByOrganizationIdAndSlotDateGreaterThanEqualOrderBySlotDateAscMealSlotAsc(
            Long organizationId, LocalDate fromDate);

    List<SlotAvailability> findByOrganizationIdAndSlotDateBetweenOrderBySlotDateAscMealSlotAsc(
            Long organizationId, LocalDate startDate, LocalDate endDate);

    Optional<SlotAvailability> findByOrganizationIdAndSlotDateAndMealSlot(
            Long organizationId, LocalDate slotDate, MealSlot mealSlot);

    List<SlotAvailability> findByOrganizationIdAndSlotDate(Long organizationId, LocalDate slotDate);

    @Query("SELECT s FROM SlotAvailability s " +
           "WHERE s.organization.id = :orgId " +
           "AND s.slotDate >= :fromDate " +
           "AND s.bookedCount < s.capacity " +
           "ORDER BY s.slotDate ASC, s.mealSlot ASC")
    List<SlotAvailability> findAvailableSlotsByOrganization(
            @Param("orgId") Long orgId, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT COUNT(s) FROM SlotAvailability s " +
           "WHERE s.organization.id = :orgId " +
           "AND s.bookedCount < s.capacity " +
           "AND ((:date IS NULL AND s.slotDate >= CURRENT_DATE) OR (:date IS NOT NULL AND s.slotDate = :date)) " +
           "AND (:mealSlot IS NULL OR s.mealSlot = :mealSlot)")
    long countAvailableSlotsMatching(
            @Param("orgId") Long orgId,
            @Param("date") LocalDate date,
            @Param("mealSlot") MealSlot mealSlot);

    long countByOrganizationIdAndSlotDateGreaterThanEqual(Long organizationId, LocalDate fromDate);

    long countByOrganizationIdAndSlotDateGreaterThanEqualAndBookedCountGreaterThan(
            Long organizationId, LocalDate fromDate, Integer minBookedCount);

    long countBySlotDateBetween(LocalDate startDate, LocalDate endDate);
}
