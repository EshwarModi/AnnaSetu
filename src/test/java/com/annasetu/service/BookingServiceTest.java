package com.annasetu.service;

import com.annasetu.dto.BookingRequestDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Occasion;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.exception.CancellationCutoffException;
import com.annasetu.exception.DuplicateBookingException;
import com.annasetu.exception.SlotFullException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit test suite validating business rules and domain invariants of {@link BookingServiceImpl}.
 *
 * Domain & Architecture Context:
 * - Asserts pessimistic write lock acquisition logic and capacity validation.
 * - Asserts duplicate booking prevention per donor per slot.
 * - Asserts 12-hour cancellation cutoff window logic and slot capacity recycling.
 * - Asserts organization confirmation, rejection, and completion state machine transitions.
 *
 * Interview Talking Points:
 * 1. State Machine Invariants: Verifies that transitions between PENDING, CONFIRMED, REJECTED,
 *    CANCELLED, and COMPLETED strictly follow business rules.
 * 2. Cutoff Boundary Verification: Simulates edge cases before and after the 12-hour cutoff window.
 * 3. Capacity Conservation: Confirms that bookedCount increments on book() and decrements on cancel() or reject().
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private OrganizationProfile approvedOrg;
    private DonorProfile donor;
    private SlotAvailability availableSlot;
    private BookingRequestDto requestDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "cancellationCutoffHours", 12);

        approvedOrg = new OrganizationProfile();
        approvedOrg.setId(10L);
        approvedOrg.setName("Asha Kiran Orphanage");
        approvedOrg.setApprovalStatus(ApprovalStatus.APPROVED);

        donor = new DonorProfile();
        donor.setId(20L);
        donor.setFullName("Rahul Sharma");

        availableSlot = new SlotAvailability(approvedOrg, LocalDate.now().plusDays(5), MealSlot.LUNCH, 1);
        availableSlot.setId(100L);
        availableSlot.setBookedCount(0);

        requestDto = new BookingRequestDto(100L, Occasion.BIRTHDAY, 50, "Vegetarian festive lunch");
    }

    @Test
    @DisplayName("book() succeeds: increments bookedCount and saves booking in PENDING state")
    void testBook_Success() {
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));
        when(donorProfileRepository.findById(20L)).thenReturn(Optional.of(donor));
        when(bookingRepository.existsBySlotIdAndDonorId(100L, 20L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        Booking result = bookingService.book(100L, 20L, requestDto);

        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(1, availableSlot.getBookedCount(), "Slot bookedCount must be incremented to 1");
        verify(slotAvailabilityRepository).save(availableSlot);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("book() fails: throws SlotFullException when bookedCount >= capacity")
    void testBook_SlotFull_ThrowsException() {
        availableSlot.setBookedCount(1); // Capacity is 1, so already full
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));

        assertThrows(SlotFullException.class, () -> bookingService.book(100L, 20L, requestDto));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("book() fails: throws DuplicateBookingException when donor has already booked the slot")
    void testBook_DuplicateBooking_ThrowsException() {
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));
        when(donorProfileRepository.findById(20L)).thenReturn(Optional.of(donor));
        when(bookingRepository.existsBySlotIdAndDonorId(100L, 20L)).thenReturn(true);

        assertThrows(DuplicateBookingException.class, () -> bookingService.book(100L, 20L, requestDto));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("book() fails: throws BusinessRuleException when organization is not approved")
    void testBook_UnapprovedOrg_ThrowsException() {
        approvedOrg.setApprovalStatus(ApprovalStatus.PENDING);
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));

        assertThrows(BusinessRuleException.class, () -> bookingService.book(100L, 20L, requestDto));
    }

    @Test
    @DisplayName("book() fails: throws BusinessRuleException when slot date is in the past")
    void testBook_PastDate_ThrowsException() {
        availableSlot.setSlotDate(LocalDate.now().minusDays(1));
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));

        assertThrows(BusinessRuleException.class, () -> bookingService.book(100L, 20L, requestDto));
    }

    @Test
    @DisplayName("cancel() succeeds: decrements bookedCount and transitions status to CANCELLED")
    void testCancel_Success() {
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.PENDING);
        booking.setId(500L);
        availableSlot.setBookedCount(1);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.cancel(500L, 20L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(0, availableSlot.getBookedCount(), "Slot bookedCount must be decremented on cancellation");
        verify(slotAvailabilityRepository).save(availableSlot);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("cancel() fails: throws SecurityException when requested by different donor")
    void testCancel_UnauthorizedDonor_ThrowsException() {
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.PENDING);
        booking.setId(500L);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        assertThrows(SecurityException.class, () -> bookingService.cancel(500L, 999L));
    }

    @Test
    @DisplayName("cancel() fails: throws CancellationCutoffException within 12 hours of meal")
    void testCancel_InsideCutoff_ThrowsException() {
        // Slot is today with a start time that makes cutoff pass
        availableSlot.setSlotDate(LocalDate.now());
        availableSlot.setMealSlot(MealSlot.BREAKFAST); // Default start time 08:00
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Breakfast", BookingStatus.PENDING);
        booking.setId(500L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        assertThrows(CancellationCutoffException.class, () -> bookingService.cancel(500L, 20L));
    }

    @Test
    @DisplayName("confirmBooking() succeeds: updates status from PENDING to CONFIRMED")
    void testConfirmBooking_Success() {
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.PENDING);
        booking.setId(500L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking confirmed = bookingService.confirmBooking(500L, 10L);

        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        verify(bookingRepository).save(confirmed);
    }

    @Test
    @DisplayName("rejectBooking() succeeds: updates status to REJECTED and releases slot capacity")
    void testRejectBooking_Success() {
        availableSlot.setBookedCount(1);
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.PENDING);
        booking.setId(500L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(slotAvailabilityRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(availableSlot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking rejected = bookingService.rejectBooking(500L, 10L);

        assertEquals(BookingStatus.REJECTED, rejected.getStatus());
        assertEquals(0, availableSlot.getBookedCount(), "Rejecting booking must decrement bookedCount");
        verify(slotAvailabilityRepository).save(availableSlot);
        verify(bookingRepository).save(rejected);
    }

    @Test
    @DisplayName("completeBooking() succeeds: updates status from CONFIRMED to COMPLETED")
    void testCompleteBooking_Success() {
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.CONFIRMED);
        booking.setId(500L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking completed = bookingService.completeBooking(500L, 10L);

        assertEquals(BookingStatus.COMPLETED, completed.getStatus());
        verify(bookingRepository).save(completed);
    }

    @Test
    @DisplayName("completeBooking() fails: throws BusinessRuleException if status is not CONFIRMED")
    void testCompleteBooking_NotConfirmed_ThrowsException() {
        Booking booking = new Booking(availableSlot, donor, Occasion.BIRTHDAY, 50, "Lunch", BookingStatus.PENDING);
        booking.setId(500L);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessRuleException.class, () -> bookingService.completeBooking(500L, 10L));
    }
}
