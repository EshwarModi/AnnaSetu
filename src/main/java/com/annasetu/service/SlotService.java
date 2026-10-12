package com.annasetu.service;

import com.annasetu.dto.SlotCreationDto;
import com.annasetu.entity.SlotAvailability;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility:
 * Business service contract governing meal slot lifecycle, batch availability generation,
 * capacity adjustments, and slot calendar blocking.
 *
 * Domain & Architecture Context:
 * - Coordinates slot capacity management to ensure orphanages and care centers do not overcommit or underutilize
 *   their meal donation facilities.
 * - Enforces the platform invariant: slot capacity can never be reduced below the active {@code bookedCount}.
 * - Guarantees that only approved organizations can generate, modify, or block availability slots.
 *
 * Interview Talking Points:
 * 1. Business Invariant Protection: Ensures that even if a frontend validation fails or is bypassed,
 *    the service layer strictly rejects past dates, reversed ranges, unapproved organizations,
 *    and capacity reductions below active commitments.
 * 2. Idempotent Batch Generation: Handles batch slot generation smoothly, creating new slots or safely updating
 *    unbooked slots without corrupting existing reservations.
 */
public interface SlotService {

    /**
     * Lists all upcoming slots (from today onwards) for an organization, ordered chronologically.
     */
    List<SlotAvailability> getUpcomingSlots(Long organizationId);

    /**
     * Retrieves slots within a specific date range for calendar views.
     */
    List<SlotAvailability> getSlotsForDateRange(Long organizationId, LocalDate startDate, LocalDate endDate);

    /**
     * Batch creates or updates meal availability slots for a validated date range and meal slot selection.
     * Returns the total count of slots successfully generated or refreshed.
     */
    int createSlotsForDateRange(Long organizationId, SlotCreationDto dto);

    /**
     * Modifies the donor capacity of an existing slot (1 for Exclusive, 2 for Shared).
     * Rejects reductions below current bookedCount.
     */
    SlotAvailability updateSlotCapacity(Long organizationId, Long slotId, Integer newCapacity);

    /**
     * Deletes a specific slot if no donor bookings are currently confirmed against it.
     */
    void deleteSlot(Long organizationId, Long slotId);

    /**
     * Blocks and deletes all meal availability slots for a specific calendar date,
     * provided none of the slots have active donor bookings.
     */
    int blockDate(Long organizationId, LocalDate date);

    /**
     * Total number of upcoming slots configured from today onwards.
     */
    long countUpcomingSlots(Long organizationId);

    /**
     * Total number of upcoming slots that have at least one active booking.
     */
    long countBookedUpcomingSlots(Long organizationId);
}
