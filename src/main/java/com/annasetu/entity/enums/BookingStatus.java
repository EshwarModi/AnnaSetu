package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Tracks the state machine lifecycle of a meal slot booking.
 *
 * Domain Context:
 * - PENDING: Booking submitted by donor, awaiting organization acknowledgement.
 * - CONFIRMED: Booking confirmed by organization; slot capacity decrement is active.
 * - REJECTED: Declined by organization (e.g., maintenance, kitchen closure).
 * - CANCELLED: Withdrawn by donor prior to slot date; frees up capacity.
 * - COMPLETED: Meal successfully served on the scheduled date.
 *
 * Interview Talking Point:
 * Governs business rules around slot availability: only PENDING and CONFIRMED bookings
 * consume slot capacity, while CANCELLED and REJECTED states release it.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
