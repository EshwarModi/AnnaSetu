package com.annasetu.dto;

import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.MealSlot;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * Responsibility:
 * Data Transfer Object serializing discrete meal slot availability for client-side AJAX / Fetch requests.
 *
 * Domain & Architecture Context:
 * - Powers live dynamic slot availability indicators on the organization detail page.
 * - Allows vanilla JavaScript frontend code to render real-time capacity states and disable fully booked buttons.
 *
 * Interview Talking Points:
 * 1. Client-Friendly API Payload: Pre-calculates flags ({@code isAvailable}, {@code isConfigured}, {@code remainingCapacity})
 *    to minimize client-side presentation logic.
 * 2. Complete Enum Coverage: Guarantees responses for all meal windows (Breakfast, Lunch, Dinner) even if
 *    an organization has not defined availability for a specific window.
 */
public class SlotAvailabilityResponseDto {

    private MealSlot mealSlot;
    private String mealSlotDisplayName;
    private Long slotId;
    private LocalDate slotDate;
    private Integer capacity;
    private Integer bookedCount;
    private Integer remainingCapacity;
    private boolean isAvailable;
    private boolean isConfigured;
    private String statusMessage;

    public SlotAvailabilityResponseDto() {
    }

    public static SlotAvailabilityResponseDto fromSlot(SlotAvailability slot) {
        SlotAvailabilityResponseDto dto = new SlotAvailabilityResponseDto();
        dto.mealSlot = slot.getMealSlot();
        dto.mealSlotDisplayName = formatMealDisplayName(slot.getMealSlot());
        dto.slotId = slot.getId();
        dto.slotDate = slot.getSlotDate();
        dto.capacity = slot.getCapacity();
        dto.bookedCount = slot.getBookedCount();
        dto.remainingCapacity = slot.getRemainingCapacity();
        dto.isAvailable = slot.isAvailable();
        dto.isConfigured = true;

        if (slot.isAvailable()) {
            dto.statusMessage = slot.getRemainingCapacity() == 1
                    ? "Available (1 spot left)"
                    : "Available (" + slot.getRemainingCapacity() + " spots left)";
        } else {
            dto.statusMessage = "Fully Booked (0 spots left)";
        }

        return dto;
    }

    public static SlotAvailabilityResponseDto notConfigured(MealSlot mealSlot, LocalDate date) {
        SlotAvailabilityResponseDto dto = new SlotAvailabilityResponseDto();
        dto.mealSlot = mealSlot;
        dto.mealSlotDisplayName = formatMealDisplayName(mealSlot);
        dto.slotId = null;
        dto.slotDate = date;
        dto.capacity = 0;
        dto.bookedCount = 0;
        dto.remainingCapacity = 0;
        dto.isAvailable = false;
        dto.isConfigured = false;
        dto.statusMessage = "Not Configured";
        return dto;
    }

    private static String formatMealDisplayName(MealSlot slot) {
        if (slot == null) return "";
        return switch (slot) {
            case BREAKFAST -> "Breakfast";
            case LUNCH -> "Lunch";
            case DINNER -> "Dinner";
        };
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public void setMealSlot(MealSlot mealSlot) {
        this.mealSlot = mealSlot;
    }

    public String getMealSlotDisplayName() {
        return mealSlotDisplayName;
    }

    public void setMealSlotDisplayName(String mealSlotDisplayName) {
        this.mealSlotDisplayName = mealSlotDisplayName;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Integer bookedCount) {
        this.bookedCount = bookedCount;
    }

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    @JsonProperty("isAvailable")
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @JsonProperty("isConfigured")
    public boolean isConfigured() {
        return isConfigured;
    }

    public void setConfigured(boolean configured) {
        isConfigured = configured;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
