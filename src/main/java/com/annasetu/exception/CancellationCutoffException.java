package com.annasetu.exception;

/**
 * Responsibility:
 * Custom unchecked domain exception thrown when a donor attempts to cancel a reservation
 * within the non-cancellable cutoff window prior to the meal event.
 *
 * Domain & Architecture Context:
 * - Protects care centers from eleventh-hour cancellations that leave residents without anticipated meals.
 * - Enforces the platform invariant: cancellations are locked within 12 hours (configurable) of meal start.
 *
 * Interview Talking Points:
 * 1. Operational Trust Guard: Balances donor flexibility with care institution operational planning.
 */
public class CancellationCutoffException extends RuntimeException {

    public CancellationCutoffException(String message) {
        super(message);
    }
}
