package com.annasetu.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Responsibility:
 * Form-backing DTO encapsulating donor registration form submissions.
 *
 * Domain & Architecture Context:
 * - Decouples presentation HTTP payload from JPA persistence entities (User and DonorProfile).
 * - Enforces Bean Validation (JSR-380 / Jakarta Validation) constraints before service processing.
 *
 * Interview Talking Points:
 * 1. Fail-Fast Validation: Ensures input validity (email format, password length, phone pattern)
 *    at the controller boundary before hitting the service layer or database.
 * 2. Password Confirmation: Provides both {@code password} and {@code confirmPassword} fields
 *    to verify user intent prior to BCrypt hashing.
 */
public class DonorRegistrationDto {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @Pattern(regexp = "^$|[0-9+()\\s-]{10,20}", message = "Phone must be a valid 10-digit number")
    private String phone;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;

    public DonorRegistrationDto() {
    }

    public DonorRegistrationDto(String fullName, String email, String password,
                                String confirmPassword, String phone, String city) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.phone = phone;
        this.city = city;
    }

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
