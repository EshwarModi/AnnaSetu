package com.annasetu.dto;

import com.annasetu.entity.enums.Occasion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Responsibility:
 * Data Transfer Object binding donor reservation form inputs when booking a meal slot.
 *
 * Domain & Architecture Context:
 * - Captures the donor's philanthropic intent: celebratory occasion (Birthday, Anniversary, Festival),
 *   estimated attendee size, and detailed food/menu description.
 * - Enforces Jakarta Bean Validation boundaries prior to transactional service execution.
 *
 * Interview Talking Points:
 * 1. Clean DTO Boundaries: Isolates entity creation from presentation layer form submission.
 * 2. Input Integrity: Enforces maximum boundary caps on menu length and attendees.
 */
public class BookingRequestDto {

    @NotNull(message = "Please select a valid meal slot")
    private Long slotId;

    @NotNull(message = "Please select an occasion for your meal donation")
    private Occasion occasion;

    @NotNull(message = "Please specify the estimated number of attendees / family members")
    @Min(value = 1, message = "Group size must be at least 1 person")
    @Max(value = 500, message = "Group size cannot exceed 500")
    private Integer peopleCount;

    @NotBlank(message = "Please describe the food items or menu you plan to sponsor")
    @Size(max = 1000, message = "Food description cannot exceed 1000 characters")
    private String foodDescription;

    public BookingRequestDto() {
    }

    public BookingRequestDto(Long slotId, Occasion occasion, Integer peopleCount, String foodDescription) {
        this.slotId = slotId;
        this.occasion = occasion;
        this.peopleCount = peopleCount;
        this.foodDescription = foodDescription;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Occasion getOccasion() {
        return occasion;
    }

    public void setOccasion(Occasion occasion) {
        this.occasion = occasion;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public void setFoodDescription(String foodDescription) {
        this.foodDescription = foodDescription;
    }

    @Override
    public String toString() {
        return "BookingRequestDto{" +
                "slotId=" + slotId +
                ", occasion=" + occasion +
                ", peopleCount=" + peopleCount +
                '}';
    }
}
