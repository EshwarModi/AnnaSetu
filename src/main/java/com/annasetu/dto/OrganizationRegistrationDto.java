package com.annasetu.dto;

import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Responsibility:
 * Form-backing DTO encapsulating welfare organization / shelter onboarding submissions.
 *
 * Domain & Architecture Context:
 * - Collects institutional operational details (capacity, dietary rules) and administrator credentials.
 * - Enforces Bean Validation rules before orchestrating user registration and pending profile creation.
 *
 * Interview Talking Points:
 * 1. Rich Validation Model: Uses numeric constraints (@Min for beneficiaries), regular expressions
 *    (@Pattern for pincode and phone), and enum validation (@NotNull for organization type).
 * 2. Strict Onboarding Invariant: Profiles created via this DTO are initialized with
 *    {@code ApprovalStatus.PENDING} to require administrative verification.
 */
public class OrganizationRegistrationDto {

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 150, message = "Organization name must be between 3 and 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City cannot exceed 50 characters")
    private String city;

    @NotBlank(message = "Area / Locality is required")
    @Size(max = 100, message = "Area cannot exceed 100 characters")
    private String area;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit Indian postal code")
    private String pincode;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 100, message = "Contact person cannot exceed 100 characters")
    private String contactPerson;

    @NotBlank(message = "Contact phone number is required")
    @Pattern(regexp = "^[0-9+()\\s-]{10,20}$", message = "Please enter a valid phone number")
    private String phone;

    @NotNull(message = "Number of people served is required")
    @Min(value = 1, message = "People served must be at least 1")
    private Integer peopleServed;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Food preference is required")
    private FoodPreference foodPreference;

    public OrganizationRegistrationDto() {
    }

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public OrganizationType getType() {
        return type;
    }

    public void setType(OrganizationType type) {
        this.type = type;
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

    public Integer getPeopleServed() {
        return peopleServed;
    }

    public void setPeopleServed(Integer peopleServed) {
        this.peopleServed = peopleServed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FoodPreference getFoodPreference() {
        return foodPreference;
    }

    public void setFoodPreference(FoodPreference foodPreference) {
        this.foodPreference = foodPreference;
    }
}
