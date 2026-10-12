package com.annasetu.controller;

import com.annasetu.dto.DonorRegistrationDto;
import com.annasetu.dto.OrganizationRegistrationDto;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Responsibility:
 * Presentation controller managing authentication views (login) and self-service registration
 * for both Donors and Welfare Organizations.
 *
 * Domain & Architecture Context:
 * - Coordinates validation binding, error presentation, and user creation workflows.
 * - Enforces Bean Validation and custom business validation (duplicate email, password confirmation matching).
 *
 * Interview Talking Points:
 * 1. Form-Backing Object Pattern: Binds HTML form fields directly to validated DTOs using
 *    {@code @ModelAttribute} and {@code @Valid}.
 * 2. Flash Attributes for Safe Post-Redirect-Get (PRG): Uses {@link RedirectAttributes} to carry
 *    one-time feedback messages across redirects, preventing duplicate form submissions on browser refresh.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Renders the custom styled login view.
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        @RequestParam(value = "orgPending", required = false) String orgPending,
                        Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully signed out.");
        }
        if (registered != null) {
            model.addAttribute("registeredMessage", "Your account has been created. Please sign in.");
        }
        if (orgPending != null) {
            model.addAttribute("orgPendingMessage", "Organization registered successfully! Your account is currently pending administrative verification.");
        }
        return "auth/login";
    }

    /**
     * Displays the donor registration form.
     */
    @GetMapping("/register/donor")
    public String showDonorRegistrationForm(Model model) {
        if (!model.containsAttribute("donorForm")) {
            model.addAttribute("donorForm", new DonorRegistrationDto());
        }
        return "auth/register-donor";
    }

    /**
     * Handles donor registration form submissions.
     */
    @PostMapping("/register/donor")
    public String processDonorRegistration(@Valid @ModelAttribute("donorForm") DonorRegistrationDto donorForm,
                                           BindingResult bindingResult,
                                           RedirectAttributes redirectAttributes,
                                           Model model) {
        // Business validation: Confirm password match
        if (!donorForm.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        // Business validation: Email uniqueness
        if (donorForm.getEmail() != null && userService.isEmailTaken(donorForm.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate", "An account with this email already exists");
        }

        // Return to form if Bean Validation or business checks failed
        if (bindingResult.hasErrors()) {
            return "auth/register-donor";
        }

        userService.registerDonor(donorForm);
        redirectAttributes.addFlashAttribute("successMessage", "Welcome to AnnaSetu! Please sign in with your new account.");
        return "redirect:/login?registered=true";
    }

    /**
     * Displays the organization registration form.
     */
    @GetMapping("/register/organization")
    public String showOrganizationRegistrationForm(Model model) {
        if (!model.containsAttribute("orgForm")) {
            model.addAttribute("orgForm", new OrganizationRegistrationDto());
        }
        model.addAttribute("orgTypes", OrganizationType.values());
        model.addAttribute("foodPreferences", FoodPreference.values());
        return "auth/register-organization";
    }

    /**
     * Handles organization registration form submissions.
     */
    @PostMapping("/register/organization")
    public String processOrganizationRegistration(@Valid @ModelAttribute("orgForm") OrganizationRegistrationDto orgForm,
                                                  BindingResult bindingResult,
                                                  RedirectAttributes redirectAttributes,
                                                  Model model) {
        // Business validation: Confirm password match
        if (!orgForm.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        // Business validation: Email uniqueness
        if (orgForm.getEmail() != null && userService.isEmailTaken(orgForm.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate", "An account with this email already exists");
        }

        // Return to form if validation failed
        if (bindingResult.hasErrors()) {
            model.addAttribute("orgTypes", OrganizationType.values());
            model.addAttribute("foodPreferences", FoodPreference.values());
            return "auth/register-organization";
        }

        userService.registerOrganization(orgForm);
        redirectAttributes.addFlashAttribute("infoMessage",
                "Organization registered successfully! Your account starts in PENDING status until verified by our platform team.");
        return "redirect:/login?orgPending=true";
    }
}
