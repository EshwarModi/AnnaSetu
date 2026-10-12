package com.annasetu.dto;

import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Responsibility:
 * Data Transfer Object capturing editable organization profile details.
 *
 * Domain & Architecture Context:
 * - Decouples presentation inputs from the underlying {@link com.annasetu.entity.OrganizationProfile} entity.
 * - Prevents malicious mass-assignment of sensitive fields such as {@code approvalStatus}, {@code id}, or {@code user}.
 * - Enforces data formatting integrity for contact details, operational scale, and dietary capabilities.
 *
 * Interview Talking Points:
 * 1. Security by DTO: Sensitive metadata (approval status, user foreign keys, audit timestamps) cannot be altered
 *    via this form object.
 * 2. Strong Validation: Enforces phone pattern matching, 6-digit Indian PIN codes, and capacity upper bounds.
 */
public class OrganizationProfileUpdateDto {

    @NotBlank(message = "Organization name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 100, message = "Contact person name must not exceed 100 characters")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Please enter a valid phone number (7-20 digits/symbols)")
    private String phone;

    @NotBlank(message = "Physical address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    @NotBlank(message = "Area / Locality is required")
    @Size(max = 100, message = "Area must not exceed 100 characters")
    private String area;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be a 6-digit number")
    private String pincode;

    @NotNull(message = "Daily residents / people served count is required")
    @Min(value = 1, message = "People served must be at least 1")
    @Max(value = 10000, message = "People served cannot exceed 10,000")
    private Integer peopleServed;

    @NotNull(message = "Food preference is required")
    private FoodPreference foodPreference;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    public OrganizationProfileUpdateDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganizationType getType() {
        return type;
    }

    public void setType(OrganizationType type) {
        this.type = type;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Integer getPeopleServed() {
        return peopleServed;
    }

    public void setPeopleServed(Integer peopleServed) {
        this.peopleServed = peopleServed;
    }

    public FoodPreference getFoodPreference() {
        return foodPreference;
    }

    public void setFoodPreference(FoodPreference foodPreference) {
        this.foodPreference = foodPreference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
