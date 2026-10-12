package com.annasetu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Responsibility:
 * Custom unchecked runtime exception thrown when a requested domain entity (User, Organization, Slot, Booking)
 * cannot be resolved in the database.
 *
 * Domain & Architecture Context:
 * - Decouples data access errors from HTTP transport layer.
 * - Mapped with @ResponseStatus(HttpStatus.NOT_FOUND) to automatically emit HTTP 404 responses.
 *
 * Interview Talking Points:
 * 1. Extending {@link RuntimeException} avoids forcing checked-exception throws clauses across service interfaces.
 * 2. Provides contextual resource name, field name, and lookup value for diagnostic traceability.
 * 3. @ResponseStatus(HttpStatus.NOT_FOUND) instructs Spring's ResponseStatusExceptionResolver to send a 404 status.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Resource";
        this.fieldName = "identifier";
        this.fieldValue = null;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
