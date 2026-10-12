package com.annasetu.service;

import com.annasetu.dto.BookingRequestDto;
import com.annasetu.entity.Booking;

import java.util.List;

/**
 * Responsibility:
 * Business service contract governing the complete booking lifecycle:
 * concurrency-safe slot reservation, donor cancellations within cutoff rules,
 * and organization confirmation, rejection, and completion workflows.
 *
 * Domain & Architecture Context:
 * - Coordinates transactional reservations between Donors and Organization Slots.
 * - Enforces pessimistic locking to eliminate double-booking or overbooking under concurrent requests.
 * - Releases reserved capacity back to the organization when a booking is rejected or cancelled.
 *
 * Interview Talking Points:
 * 1. Pessimistic Write Locking: Prevents race conditions by locking slot records at the database level
 *    ({@code SELECT ... FOR UPDATE}) during the reservation transaction.
 * 2. Cutoff Window Business Rule: Prevents cancellations when less than 12 hours remain before the meal.
 * 3. Capacity Recycling: Automatically decrements {@code bookedCount} upon cancellation or rejection.
 */
public interface BookingService {

    /**
     * Reserves a meal slot for a donor under pessimistic concurrency control.
     * Throws SlotFullException if capacity is exhausted, or DuplicateBookingException if donor already booked.
     */
    Booking book(Long slotId, Long donorId, BookingRequestDto requestDto);

    /**
     * Cancels an active donor booking if within the permitted cancellation cutoff window.
     * Decrements the slot's bookedCount in the same transaction.
     */
    Booking cancel(Long bookingId, Long donorId);

    /**
     * Confirms a pending donor booking by the host organization.
     */
    Booking confirmBooking(Long bookingId, Long organizationId);

    /**
     * Rejects a booking by the host organization and releases the reserved slot back to the platform.
     */
    Booking rejectBooking(Long bookingId, Long organizationId);

    /**
     * Marks a confirmed donation as successfully fulfilled / completed.
     */
    Booking completeBooking(Long bookingId, Long organizationId);

    /**
     * Retrieves all bookings for a given donor, ordered chronologically.
     */
    List<Booking> getDonorBookings(Long donorId);

    /**
     * Retrieves all bookings for an organization, ordered chronologically.
     */
    List<Booking> getOrganizationBookings(Long organizationId);

    /**
     * Determines whether an active booking can currently be cancelled by the donor.
     */
    boolean isCancellable(Booking booking);

    /**
     * Retrieves a booking by its primary key or throws ResourceNotFoundException.
     */
    Booking getBookingById(Long id);
}
