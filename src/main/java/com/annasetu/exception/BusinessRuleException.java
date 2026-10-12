package com.annasetu.exception;

/**
 * Responsibility:
 * Custom unchecked domain exception thrown when an operation violates platform business invariants.
 *
 * Domain & Architecture Context:
 * - Raised when attempting to perform restricted actions, such as:
 *   * Reducing slot capacity below existing booked donors.
 *   * Adding or modifying availability on past dates.
 *   * Creating slots when an organization account has not yet received administrator approval.
 *   * Deleting or blocking meal slots that already have confirmed bookings.
 *
 * Interview Talking Points:
 * 1. Expressive Domain Exceptions: Distinguishes domain rule violations from structural technical errors,
 *    allowing presentation controllers to convert them into user-friendly flash messages.
 * 2. Unchecked Hierarchy: Extends {@link RuntimeException} to participate cleanly in Spring's transaction rollback.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
