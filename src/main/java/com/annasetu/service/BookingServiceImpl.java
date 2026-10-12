package com.annasetu.service;

import com.annasetu.dto.BookingRequestDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.exception.CancellationCutoffException;
import com.annasetu.exception.DuplicateBookingException;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.exception.SlotFullException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Responsibility:
 * Concrete business implementation for {@link BookingService}.
 *
 * Domain & Architecture Context:
 * - Executes concurrency-safe slot bookings using pessimistic write locks (SELECT ... FOR UPDATE).
 * - Enforces business invariants:
 *   1. Overbooking prevention under high concurrent contention.
 *   2. Rejection of duplicate bookings by the same donor on the same slot.
 *   3. Enforcing a non-cancellable cutoff window (default 12 hours before meal start).
 *   4. Capacity recycling when bookings are cancelled by donors or rejected by organizations.
 *
 * Interview Talking Points:
 * 1. Pessimistic Write Locking (PESSIMISTIC_WRITE): The database transaction acquires an exclusive row lock
 *    on the SlotAvailability entity. Other competing transactions serialize on this lock, guaranteeing that
 *    bookedCount is checked and incremented atomically without dirty reads or phantom race conditions.
 * 2. Why Pessimistic over Optimistic Locking: In high-demand scenarios (e.g., festivals), optimistic locking causes
 *    widespread OptimisticLockExceptions and failed retries; pessimistic locking queues transactions and gives
 *    instant, clean feedback when capacity is exhausted.
 * 3. Cutoff Time Calculation: Uses {@code slotDate} combined with {@code MealSlot.getDefaultStartTime()} to establish
 *    an exact temporal boundary for donor cancellations.
 */
@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final NotificationService notificationService;

    @Value("${annasetu.booking.cancellation-cutoff-hours:12}")
    private int cancellationCutoffHours;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              SlotAvailabilityRepository slotAvailabilityRepository,
                              DonorProfileRepository donorProfileRepository,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.slotAvailabilityRepository = slotAvailabilityRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Booking book(Long slotId, Long donorId, BookingRequestDto requestDto) {
        // 1. Acquire pessimistic write lock on the slot record (SELECT ... FOR UPDATE)
        SlotAvailability slot = slotAvailabilityRepository.findByIdWithPessimisticLock(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal slot not found with ID: " + slotId));

        // 2. Organization verification guard
        if (slot.getOrganization().getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessRuleException("Cannot reserve a slot with an unapproved organization.");
        }

        // 3. Historical date guard
        if (slot.getSlotDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Cannot reserve meal slots on past dates.");
        }

        // 4. Capacity check under pessimistic lock
        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new SlotFullException("This meal slot is fully booked (" + slot.getBookedCount()
                    + "/" + slot.getCapacity() + "). Please choose another slot.");
        }

        // 5. Resolve Donor profile
        DonorProfile donor = donorProfileRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found with ID: " + donorId));

        // 6. Duplicate reservation check
        if (bookingRepository.existsBySlotIdAndDonorId(slot.getId(), donor.getId())) {
            throw new DuplicateBookingException("You have already reserved this meal slot.");
        }

        // 7. Atomically increment slot bookedCount
        slot.setBookedCount(slot.getBookedCount() + 1);
        slotAvailabilityRepository.save(slot);

        // 8. Create and persist booking in PENDING state awaiting host org confirmation
        Booking booking = new Booking(
                slot,
                donor,
                requestDto.getOccasion(),
                requestDto.getPeopleCount(),
                requestDto.getFoodDescription(),
                BookingStatus.PENDING
        );

        Booking savedBooking = bookingRepository.save(booking);
        notificationService.sendBookingCreatedNotice(savedBooking);
        return savedBooking;
    }

    @Override
    @Transactional
    public Booking cancel(Long bookingId, Long donorId) {
        Booking booking = getBookingById(bookingId);

        // Security check
        if (!booking.getDonor().getId().equals(donorId)) {
            throw new SecurityException("Unauthorized: You can only cancel your own bookings.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("This booking is already cancelled.");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only active bookings (PENDING or CONFIRMED) can be cancelled. Current status: " + booking.getStatus());
        }

        // Cutoff window check
        if (!isCancellable(booking)) {
            throw new CancellationCutoffException("Cancellations are only permitted at least " + cancellationCutoffHours
                    + " hours before the scheduled meal window.");
        }

        // Acquire lock on slot to decrement bookedCount
        SlotAvailability slot = slotAvailabilityRepository.findByIdWithPessimisticLock(booking.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated meal slot not found."));

        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotAvailabilityRepository.save(slot);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);
        notificationService.sendBookingCancellationNotice(cancelledBooking);
        return cancelledBooking;
    }

    @Override
    @Transactional
    public Booking confirmBooking(Long bookingId, Long organizationId) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getSlot().getOrganization().getId().equals(organizationId)) {
            throw new SecurityException("Unauthorized: You can only manage bookings for your own organization.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING bookings can be confirmed. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);
        notificationService.sendBookingConfirmationNotice(confirmedBooking);
        return confirmedBooking;
    }

    @Override
    @Transactional
    public Booking rejectBooking(Long bookingId, Long organizationId) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getSlot().getOrganization().getId().equals(organizationId)) {
            throw new SecurityException("Unauthorized: You can only manage bookings for your own organization.");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only active bookings can be rejected. Current status: " + booking.getStatus());
        }

        // Release slot capacity
        SlotAvailability slot = slotAvailabilityRepository.findByIdWithPessimisticLock(booking.getSlot().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated meal slot not found."));

        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotAvailabilityRepository.save(slot);

        booking.setStatus(BookingStatus.REJECTED);
        Booking rejectedBooking = bookingRepository.save(booking);
        notificationService.sendBookingCancellationNotice(rejectedBooking);
        return rejectedBooking;
    }

    @Override
    @Transactional
    public Booking completeBooking(Long bookingId, Long organizationId) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getSlot().getOrganization().getId().equals(organizationId)) {
            throw new SecurityException("Unauthorized: You can only manage bookings for your own organization.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED bookings can be marked as completed. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getDonorBookings(Long donorId) {
        return bookingRepository.findDonorBookingsWithDetails(donorId);
    }

    @Override
    public List<Booking> getOrganizationBookings(Long organizationId) {
        return bookingRepository.findOrganizationBookingsWithDetails(organizationId);
    }

    @Override
    public boolean isCancellable(Booking booking) {
        if (booking == null) return false;
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            return false;
        }

        LocalDate slotDate = booking.getSlot().getSlotDate();
        LocalTime startTime = booking.getSlot().getMealSlot().getDefaultStartTime();
        LocalDateTime mealDateTime = slotDate.atTime(startTime);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDateTime = mealDateTime.minusHours(cancellationCutoffHours);

        return now.isBefore(cutoffDateTime);
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }
}
