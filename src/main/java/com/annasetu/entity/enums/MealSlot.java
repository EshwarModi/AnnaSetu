package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Represents the distinct meal serving windows available within a calendar day.
 *
 * Domain Context:
 * - BREAKFAST: Morning meal slot.
 * - LUNCH: Afternoon meal slot (most common for celebratory family visits and feasts).
 * - DINNER: Evening meal slot.
 *
 * Interview Talking Point:
 * Combined with {@code organization_id} and {@code slot_date}, this forms the unique business key
 * in the {@code slot_availabilities} table: {@code UNIQUE(organization_id, slot_date, meal_slot)}.
 */
import java.time.LocalTime;

public enum MealSlot {
    BREAKFAST,
    LUNCH,
    DINNER;

    /**
     * Returns the default meal serving start time for cancellation cutoff and schedule computations.
     */
    public LocalTime getDefaultStartTime() {
        return switch (this) {
            case BREAKFAST -> LocalTime.of(8, 0);
            case LUNCH -> LocalTime.of(12, 30);
            case DINNER -> LocalTime.of(19, 30);
        };
    }
}
