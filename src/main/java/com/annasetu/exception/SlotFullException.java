package com.annasetu.exception;

/**
 * Responsibility:
 * Custom unchecked domain exception thrown when an attempt is made to reserve a slot
 * that has reached its maximum donor capacity.
 *
 * Domain & Architecture Context:
 * - Raised inside {@link com.annasetu.service.BookingService#book(Long, Long, com.annasetu.dto.BookingRequestDto)}
 *   after acquiring a pessimistic write lock and discovering {@code bookedCount >= capacity}.
 * - Handled gracefully by {@link com.annasetu.exception.GlobalExceptionHandler} to display
 *   an informative, empathetic message to the donor.
 *
 * Interview Talking Points:
 * 1. Concurrency Boundary: Indicates a clean lock-serialized rejection when competing threads attempt
 *    to book the final remaining spot.
 * 2. Domain Meaningfulness: Provides clear operational feedback rather than generic database constraint errors.
 */
public class SlotFullException extends RuntimeException {

    public SlotFullException(String message) {
        super(message);
    }
}
