package com.annasetu.controller;

import com.annasetu.config.CustomUserDetails;
import com.annasetu.dto.BookingRequestDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.Occasion;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import com.annasetu.service.BookingService;
import jakarta.validation.Valid;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsibility:
 * Presentation controller managing donor-facing workflows:
 * viewing personal booking history (upcoming & past), reserving meal slots, and cancelling reservations.
 *
 * Domain & Architecture Context:
 * - Secures all {@code /donor/**} endpoints under {@code ROLE_DONOR}.
 * - Bridges the donor's philanthropic intent with transactional slot booking in {@link BookingService}.
 * - Separates active upcoming reservations from historical commitments.
 *
 * Interview Talking Points:
 * 1. Persona Workspace Cohesion: Concentrates all donor operations in one dedicated controller.
 * 2. Temporal Segregation: Partitions reservations into active upcoming vs. past/cancelled for clear UX.
 * 3. PRG Pattern: Enforces Post/Redirect/Get flow with flash messages across bookings and cancellations.
 */
@Controller
@RequestMapping("/donor")
public class DonorController {

    private final BookingService bookingService;
    private final DonorProfileRepository donorProfileRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final UserRepository userRepository;

    public DonorController(BookingService bookingService,
                           DonorProfileRepository donorProfileRepository,
                           SlotAvailabilityRepository slotAvailabilityRepository,
                           UserRepository userRepository) {
        this.bookingService = bookingService;
        this.donorProfileRepository = donorProfileRepository;
        this.slotAvailabilityRepository = slotAvailabilityRepository;
        this.userRepository = userRepository;
    }

    private DonorProfile getAuthenticatedDonor(Authentication authentication, CustomUserDetails userDetails) {
        Long userId = null;
        if (userDetails != null && userDetails.getUserId() != null) {
            userId = userDetails.getUserId();
        } else if (authentication != null && authentication.getName() != null) {
            userId = userRepository.findByEmail(authentication.getName())
                    .map(User::getId)
                    .orElse(null);
        }

        if (userId == null) {
            throw new ResourceNotFoundException("Authenticated donor user session could not be resolved.");
        }

        final Long finalUserId = userId;
        return donorProfileRepository.findByUserId(finalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user ID: " + finalUserId));
    }

    /**
     * Donor Dashboard: Displays donor profile summary, upcoming reservations, and past donation history.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Authentication authentication,
                            Model model) {
        DonorProfile donor = getAuthenticatedDonor(authentication, userDetails);
        List<Booking> allBookings = bookingService.getDonorBookings(donor.getId());

        LocalDate today = LocalDate.now();

        // Segregate into upcoming active vs past/completed/cancelled
        List<Booking> upcomingBookings = allBookings.stream()
                .filter(b -> !b.getSlot().getSlotDate().isBefore(today)
                        && (b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED))
                .toList();

        List<Booking> pastBookings = allBookings.stream()
                .filter(b -> b.getSlot().getSlotDate().isBefore(today)
                        || b.getStatus() == BookingStatus.COMPLETED
                        || b.getStatus() == BookingStatus.CANCELLED
                        || b.getStatus() == BookingStatus.REJECTED)
                .toList();

        Map<Long, Boolean> cancellableMap = new HashMap<>();
        for (Booking b : upcomingBookings) {
            cancellableMap.put(b.getId(), bookingService.isCancellable(b));
        }

        model.addAttribute("donor", donor);
        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("pastBookings", pastBookings);
        model.addAttribute("cancellableMap", cancellableMap);
        model.addAttribute("pageTitle", "Donor Dashboard - AnnaSetu");
        return "donor/dashboard";
    }

    /**
     * Booking Form: Prepares reservation form for a selected meal slot.
     */
    @GetMapping("/book")
    public String showBookingForm(@RequestParam("slotId") Long slotId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Authentication authentication,
                                  Model model) {
        getAuthenticatedDonor(authentication, userDetails);

        SlotAvailability slot = slotAvailabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Selected meal slot not found with ID: " + slotId));

        if (!model.containsAttribute("bookingDto")) {
            BookingRequestDto bookingDto = new BookingRequestDto();
            bookingDto.setSlotId(slotId);
            bookingDto.setOccasion(Occasion.BIRTHDAY);
            bookingDto.setPeopleCount(slot.getOrganization().getPeopleServed());
            model.addAttribute("bookingDto", bookingDto);
        }

        model.addAttribute("slot", slot);
        model.addAttribute("org", slot.getOrganization());
        model.addAttribute("occasions", Occasion.values());
        model.addAttribute("pageTitle", "Book Food Donation Slot - AnnaSetu");
        return "donor/book";
    }

    /**
     * Processes slot booking form submission under pessimistic lock.
     */
    @PostMapping("/book")
    public String processBooking(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Authentication authentication,
                                 @Valid @ModelAttribute("bookingDto") BookingRequestDto bookingDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        DonorProfile donor = getAuthenticatedDonor(authentication, userDetails);

        if (bindingResult.hasErrors()) {
            SlotAvailability slot = slotAvailabilityRepository.findById(bookingDto.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected meal slot not found with ID: " + bookingDto.getSlotId()));
            model.addAttribute("slot", slot);
            model.addAttribute("org", slot.getOrganization());
            model.addAttribute("occasions", Occasion.values());
            model.addAttribute("pageTitle", "Book Food Donation Slot - AnnaSetu");
            return "donor/book";
        }

        bookingService.book(bookingDto.getSlotId(), donor.getId(), bookingDto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Food donation slot reserved successfully! Your booking is pending confirmation with the organization.");

        return "redirect:/donor/dashboard";
    }

    /**
     * Cancels an active donor reservation if before the cancellation cutoff.
     */
    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long bookingId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        DonorProfile donor = getAuthenticatedDonor(authentication, userDetails);

        bookingService.cancel(bookingId, donor.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Your booking has been cancelled and the slot has been released back to the platform.");

        return "redirect:/donor/dashboard";
    }

    /**
     * Renders a printable donation confirmation receipt for an individual meal sponsorship.
     */
    @GetMapping("/bookings/{id}/receipt")
    public String viewReceipt(@PathVariable("id") Long bookingId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Authentication authentication,
                              Model model) {
        DonorProfile donor = getAuthenticatedDonor(authentication, userDetails);
        Booking booking = bookingService.getBookingById(bookingId);

        if (!booking.getDonor().getId().equals(donor.getId())) {
            throw new SecurityException("Unauthorized: You can only view receipts for your own bookings.");
        }

        model.addAttribute("donor", donor);
        model.addAttribute("booking", booking);
        model.addAttribute("receiptNumber", "ANNA-REC-" + booking.getId() + "-" + (booking.getCreatedAt() != null ? booking.getCreatedAt().getYear() : LocalDate.now().getYear()));
        model.addAttribute("issueDate", LocalDate.now());
        model.addAttribute("pageTitle", "Donation Receipt #BK-" + booking.getId() + " - AnnaSetu");
        return "donor/receipt";
    }
}
