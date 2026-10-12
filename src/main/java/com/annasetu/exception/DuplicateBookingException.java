package com.annasetu.exception;

/**
 * Responsibility:
 * Custom unchecked domain exception thrown when a donor attempts to reserve a meal slot
 * they have already previously booked.
 *
 * Domain & Architecture Context:
 * - Enforces the invariant modeled by {@code UNIQUE(slot_id, donor_id)}.
 * - Intercepted before database constraint violation triggers, ensuring clean transaction state.
 *
 * Interview Talking Points:
 * 1. Proactive Conflict Detection: Verifies existence derived query {@code existsBySlotIdAndDonorId}
 *    under row lock to prevent raw {@link java.sql.SQLIntegrityConstraintViolationException} traces.
 */
public class DuplicateBookingException extends RuntimeException {

    public DuplicateBookingException(String message) {
        super(message);
    }
}
