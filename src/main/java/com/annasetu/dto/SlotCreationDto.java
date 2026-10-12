package com.annasetu.dto;

import com.annasetu.entity.enums.MealSlot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility:
 * Form backing object and Data Transfer Object for bulk meal slot creation across a date range.
 *
 * Domain & Architecture Context:
 * - Allows organization administrators to define meal availability in batches (e.g., publishing slots
 *   for the upcoming month) rather than adding each day individually.
 * - Enforces capacity constraints: 1 represents exclusive single-donor sponsorship, while 2 represents
 *   shared sponsorship where two donors can participate in the same meal event.
 *
 * Interview Talking Points:
 * 1. Clean Validation Boundaries: Applies Jakarta Bean Validation constraints (@NotNull, @NotEmpty, @Min, @Max)
 *    before passing to the service layer.
 * 2. Multi-Select Enums: Binds a collection of {@link MealSlot} items from checkbox inputs.
 * 3. Date Formatting: Uses @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) for reliable ISO-8601 parsing.
 */
public class SlotCreationDto {

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotEmpty(message = "Please select at least one meal slot")
    private List<MealSlot> mealSlots = new ArrayList<>();

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 (Exclusive)")
    @Max(value = 2, message = "Capacity cannot exceed 2 (Shared)")
    private Integer capacity = 1;

    public SlotCreationDto() {
    }

    public SlotCreationDto(LocalDate startDate, LocalDate endDate, List<MealSlot> mealSlots, Integer capacity) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.mealSlots = mealSlots;
        this.capacity = capacity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<MealSlot> getMealSlots() {
        return mealSlots;
    }

    public void setMealSlots(List<MealSlot> mealSlots) {
        this.mealSlots = mealSlots;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "SlotCreationDto{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", mealSlots=" + mealSlots +
                ", capacity=" + capacity +
                '}';
    }
}
