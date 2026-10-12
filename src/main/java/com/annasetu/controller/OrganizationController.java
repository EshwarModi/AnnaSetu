package com.annasetu.controller;

import com.annasetu.config.CustomUserDetails;
import com.annasetu.dto.OrganizationProfileUpdateDto;
import com.annasetu.dto.SlotCreationDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.UserRepository;
import com.annasetu.service.BookingService;
import com.annasetu.service.OrganizationService;
import com.annasetu.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility:
 * Presentation controller managing organization-facing workflows:
 * dashboard metrics, profile view and editing, calendar slot generation, capacity adjustments, and date blocking.
 *
 * Domain & Architecture Context:
 * - Centralizes all {@code /org/**} routes with Role-Based Access Control enforcing {@code ROLE_ORGANIZATION}.
 * - Enforces the institutional approval invariant: unapproved institutions can view and edit their profile,
 *   but slot generation, capacity changes, and deletion are strictly rejected with helpful user feedback.
 *
 * Interview Talking Points:
 * 1. Double-Layer Verification Guard: Checks approval status both in the presentation layer (for immediate redirect
 *    and flash error messaging) and in the business service layer (to safeguard domain invariants).
 * 2. PRG Pattern (Post/Redirect/Get): Form submissions redirect back with Spring {@link RedirectAttributes}
 *    flash messages to prevent duplicate form submissions and maintain pristine browser navigation history.
 * 3. Graceful Error Handling: Catches domain-specific {@link BusinessRuleException} and converts it into actionable
 *    feedback alerts in the UI.
 */
@Controller
@RequestMapping("/org")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final SlotService slotService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public OrganizationController(OrganizationService organizationService,
                                  SlotService slotService,
                                  BookingRepository bookingRepository,
                                  BookingService bookingService,
                                  UserRepository userRepository) {
        this.organizationService = organizationService;
        this.slotService = slotService;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    private OrganizationProfile getAuthenticatedOrganization(Authentication authentication,
                                                              CustomUserDetails userDetails) {
        Long userId = null;
        if (userDetails != null && userDetails.getUserId() != null) {
            userId = userDetails.getUserId();
        } else if (authentication != null && authentication.getName() != null) {
            userId = userRepository.findByEmail(authentication.getName())
                    .map(User::getId)
                    .orElse(null);
        }

        if (userId == null) {
            throw new ResourceNotFoundException("Authenticated organization user session could not be resolved.");
        }

        return organizationService.getOrganizationByUserId(userId);
    }

    /**
     * Organization Dashboard: Displays institutional overview, approval status banner,
     * quick capacity metrics, and upcoming donor bookings.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Authentication authentication,
                            Model model) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        List<Booking> orgBookings = bookingRepository.findOrganizationBookingsWithDetails(org.getId());
        long upcomingSlotsCount = slotService.countUpcomingSlots(org.getId());
        long bookedSlotsCount = slotService.countBookedUpcomingSlots(org.getId());

        LocalDate today = LocalDate.now();

        // 1. Today's bookings
        List<Booking> todayBookings = orgBookings.stream()
                .filter(b -> b.getSlot().getSlotDate().isEqual(today))
                .toList();

        // 2. Upcoming bookings grouped by date (ascending)
        java.util.Map<LocalDate, List<Booking>> upcomingBookingsByDate = orgBookings.stream()
                .filter(b -> !b.getSlot().getSlotDate().isBefore(today))
                .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getSlot().getSlotDate(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()
                ));

        model.addAttribute("org", org);
        model.addAttribute("bookings", orgBookings);
        model.addAttribute("todayBookings", todayBookings);
        model.addAttribute("upcomingBookingsByDate", upcomingBookingsByDate);
        model.addAttribute("today", today);
        model.addAttribute("upcomingSlotsCount", upcomingSlotsCount);
        model.addAttribute("bookedSlotsCount", bookedSlotsCount);
        model.addAttribute("pageTitle", "Organization Dashboard - AnnaSetu");
        return "org/dashboard";
    }

    /**
     * Backward-compatible pending notice URL redirecting directly to the unified dashboard.
     */
    @GetMapping("/pending")
    public String pendingRedirect() {
        return "redirect:/org/dashboard";
    }

    /**
     * Organization Profile: Displays profile details in an editable form.
     */
    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Authentication authentication,
                              Model model) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (!model.containsAttribute("profileDto")) {
            model.addAttribute("profileDto", organizationService.toUpdateDto(org));
        }

        model.addAttribute("org", org);
        model.addAttribute("orgTypes", OrganizationType.values());
        model.addAttribute("foodPreferences", FoodPreference.values());
        model.addAttribute("pageTitle", "Edit Organization Profile - AnnaSetu");
        return "org/profile";
    }

    /**
     * Updates organization profile attributes with Bean Validation.
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                Authentication authentication,
                                @Valid @ModelAttribute("profileDto") OrganizationProfileUpdateDto profileDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("org", org);
            model.addAttribute("orgTypes", OrganizationType.values());
            model.addAttribute("foodPreferences", FoodPreference.values());
            model.addAttribute("pageTitle", "Edit Organization Profile - AnnaSetu");
            return "org/profile";
        }

        try {
            organizationService.updateOrganizationProfile(org.getId(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Organization profile details updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update profile: " + ex.getMessage());
        }

        return "redirect:/org/profile";
    }

    /**
     * Slot Management: Lists upcoming meal availability and provides batch creation / blocking tools.
     */
    @GetMapping("/slots")
    public String manageSlots(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Authentication authentication,
                              Model model) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        List<SlotAvailability> upcomingSlots = slotService.getUpcomingSlots(org.getId());

        if (!model.containsAttribute("slotDto")) {
            SlotCreationDto slotDto = new SlotCreationDto();
            slotDto.setStartDate(LocalDate.now());
            slotDto.setEndDate(LocalDate.now().plusDays(7));
            slotDto.setCapacity(1);
            slotDto.setMealSlots(List.of(MealSlot.LUNCH, MealSlot.DINNER));
            model.addAttribute("slotDto", slotDto);
        }

        model.addAttribute("org", org);
        model.addAttribute("slots", upcomingSlots);
        model.addAttribute("availableMealSlots", MealSlot.values());
        model.addAttribute("pageTitle", "Meal Slot Management - AnnaSetu");
        return "org/slots";
    }

    /**
     * Batch creates meal availability slots for a date range.
     */
    @PostMapping("/slots/create")
    public String createSlots(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Authentication authentication,
                              @Valid @ModelAttribute("slotDto") SlotCreationDto slotDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your organization account is currently pending administrative verification. Slot management is restricted until approved.");
            return "redirect:/org/dashboard";
        }

        if (bindingResult.hasErrors()) {
            List<SlotAvailability> upcomingSlots = slotService.getUpcomingSlots(org.getId());
            model.addAttribute("org", org);
            model.addAttribute("slots", upcomingSlots);
            model.addAttribute("availableMealSlots", MealSlot.values());
            model.addAttribute("pageTitle", "Meal Slot Management - AnnaSetu");
            return "org/slots";
        }

        try {
            int count = slotService.createSlotsForDateRange(org.getId(), slotDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully configured " + count + " meal slots across the selected date range.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/org/slots";
    }

    /**
     * Modifies the donor capacity for an individual meal slot.
     */
    @PostMapping("/slots/{id}/capacity")
    public String updateCapacity(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Authentication authentication,
                                 @PathVariable("id") Long slotId,
                                 @RequestParam("capacity") Integer newCapacity,
                                 RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Slot management is restricted while your account is pending approval.");
            return "redirect:/org/dashboard";
        }

        try {
            SlotAvailability updated = slotService.updateSlotCapacity(org.getId(), slotId, newCapacity);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Capacity for " + updated.getSlotDate() + " (" + updated.getMealSlot()
                            + ") updated to " + newCapacity + ".");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/org/slots";
    }

    /**
     * Removes an unbooked meal slot.
     */
    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Authentication authentication,
                             @PathVariable("id") Long slotId,
                             RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Slot management is restricted while your account is pending approval.");
            return "redirect:/org/dashboard";
        }

        try {
            slotService.deleteSlot(org.getId(), slotId);
            redirectAttributes.addFlashAttribute("successMessage", "Meal slot removed successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/org/slots";
    }

    /**
     * Blocks and removes all meal availability slots for a calendar date.
     */
    @PostMapping("/slots/block-date")
    public String blockDate(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Authentication authentication,
                            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);

        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Slot management is restricted while your account is pending approval.");
            return "redirect:/org/dashboard";
        }

        try {
            int removedCount = slotService.blockDate(org.getId(), date);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully blocked date " + date + " (removed " + removedCount + " unbooked slots).");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/org/slots";
    }

    /**
     * Confirms a pending meal donation booking for this organization.
     */
    @PostMapping("/bookings/{id}/confirm")
    public String confirmBooking(@PathVariable("id") Long bookingId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);
        try {
            bookingService.confirmBooking(bookingId, org.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #BK-" + bookingId + " has been confirmed successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/org/dashboard";
    }

    /**
     * Rejects a meal donation booking and automatically releases the reserved slot back to the platform.
     */
    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable("id") Long bookingId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);
        try {
            bookingService.rejectBooking(bookingId, org.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #BK-" + bookingId + " has been rejected and the meal slot is reopened for other donors.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/org/dashboard";
    }

    /**
     * Marks a confirmed meal donation booking as fulfilled / completed.
     */
    @PostMapping("/bookings/{id}/complete")
    public String completeBooking(@PathVariable("id") Long bookingId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        OrganizationProfile org = getAuthenticatedOrganization(authentication, userDetails);
        try {
            bookingService.completeBooking(bookingId, org.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #BK-" + bookingId + " marked as completed. Thank you for facilitating this community meal!");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/org/dashboard";
    }
}
