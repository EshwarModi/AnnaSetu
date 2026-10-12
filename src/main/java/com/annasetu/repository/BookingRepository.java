package com.annasetu.repository;

import com.annasetu.entity.Booking;
import com.annasetu.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Responsibility:
 * Data access operations for {@link Booking} reservation records.
 *
 * Domain & Architecture Context:
 * - Queries donor booking history for the Donor Dashboard.
 * - Queries upcoming reservations for an organization's meal calendar.
 * - Verifies if a donor has already reserved a particular slot before allowing a new submission.
 *
 * Interview Talking Points:
 * 1. Employs deep JOIN fetching via JPQL when pulling booking summaries with associated donor and
 *    organization details to eliminate N+1 queries.
 * 2. Employs derived existence checks {@code existsBySlotIdAndDonorId} for fast conflict checks before booking.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    List<Booking> findBySlotOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<Booking> findBySlotId(Long slotId);

    boolean existsBySlotIdAndDonorId(Long slotId, Long donorId);

    List<Booking> findByStatus(BookingStatus status);

    long countByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.slot s " +
           "JOIN FETCH s.organization o " +
           "WHERE b.donor.id = :donorId " +
           "ORDER BY s.slotDate DESC, s.mealSlot ASC")
    List<Booking> findDonorBookingsWithDetails(@Param("donorId") Long donorId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.slot s " +
           "JOIN FETCH b.donor d " +
           "WHERE s.organization.id = :orgId " +
           "ORDER BY s.slotDate ASC, s.mealSlot ASC")
    List<Booking> findOrganizationBookingsWithDetails(@Param("orgId") Long orgId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.slot s " +
           "JOIN FETCH s.organization o " +
           "JOIN FETCH b.donor d " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookingsWithDetails(org.springframework.data.domain.Pageable pageable);
}
