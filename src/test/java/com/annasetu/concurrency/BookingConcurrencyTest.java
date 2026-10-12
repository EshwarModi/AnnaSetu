package com.annasetu.concurrency;

import com.annasetu.dto.BookingRequestDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.DonorProfile;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.Occasion;
import com.annasetu.entity.enums.Role;
import com.annasetu.exception.SlotFullException;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.UserRepository;
import com.annasetu.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility:
 * Multithreaded integration test suite verifying pessimistic concurrency control in {@link BookingService}.
 *
 * Domain & Architecture Context:
 * - Simulates high-contention traffic spikes (e.g. festive food drives, flash bookings) where N concurrent
 *   threads attempt to book the final available spot of a meal slot at the exact same millisecond.
 * - Proves that pessimistic write locking ({@code SELECT ... FOR UPDATE} via {@code @Lock(PESSIMISTIC_WRITE)})
 *   serializes competing transactions, prevents dirty reads / lost updates, and guarantees that:
 *   1. {@code bookedCount} never exceeds {@code capacity}.
 *   2. Exactly the right number of bookings succeed.
 *   3. Excess concurrent transactions are cleanly rejected with {@link SlotFullException}.
 *
 * Interview Talking Points:
 * 1. Pessimistic vs. Optimistic Locking: Optimistic locking with {@code @Version} would fail 9 out of 10 threads
 *    with {@code ObjectOptimisticLockingFailureException}, requiring complex retry loops. Pessimistic write locking
 *    locks the row in MySQL InnoDB, letting transactions evaluate capacity cleanly one by one.
 * 2. Latch Synchronization Pattern: Uses a two-phase {@link CountDownLatch} (ready latch + start latch) to unleash
 *    all worker threads in perfect parallel synchrony against the database.
 * 3. Database Invariant Validation: Asserts that MySQL's check constraint {@code (booked_count <= capacity)} is
 *    honored without deadlocks or orphan reservations.
 */
@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OrganizationProfileRepository organizationProfileRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private OrganizationProfile testOrg;
    private final List<DonorProfile> testDonors = new ArrayList<>();
    private final List<SlotAvailability> testSlots = new ArrayList<>();

    @BeforeEach
    void setUp() {
        testOrg = organizationProfileRepository.findAll().stream()
                .filter(o -> o.getApprovalStatus() == ApprovalStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Approved organization required for test"));

        // Create 10 distinct donor users so that each concurrent thread represents an independent donor
        for (int i = 1; i <= 10; i++) {
            final int index = i;
            String email = "concurrency.donor." + index + "@test.org";
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = userRepository.save(new User(email, "hashed_pw", Role.DONOR, true));
            }
            DonorProfile profile = donorProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile == null) {
                profile = donorProfileRepository.save(new DonorProfile(user, "Concurrency Donor " + index, "+91 990000000" + index, "Pune"));
            }
            testDonors.add(profile);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up created bookings for test slots
        for (SlotAvailability slot : testSlots) {
            List<Booking> bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getSlot().getId().equals(slot.getId()))
                    .toList();
            bookingRepository.deleteAll(bookings);
            slotAvailabilityRepository.deleteById(slot.getId());
        }
        testSlots.clear();

        // Clean up test donors
        for (DonorProfile donor : testDonors) {
            List<Booking> donorBookings = bookingRepository.findDonorBookingsWithDetails(donor.getId());
            bookingRepository.deleteAll(donorBookings);
            donorProfileRepository.delete(donor);
            userRepository.delete(donor.getUser());
        }
        testDonors.clear();
    }

    @Test
    @DisplayName("Concurrency Test: 10 threads compete for 1 remaining spot -> Exactly 1 succeeds, 9 fail with SlotFullException")
    void testConcurrentBooking_ExclusiveSlot_CapacityOne() throws InterruptedException {
        LocalDate slotDate = LocalDate.now().plusDays(45);
        slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(testOrg.getId(), slotDate, MealSlot.LUNCH)
                .ifPresent(existing -> {
                    List<Booking> existingBookings = bookingRepository.findAll().stream()
                            .filter(b -> b.getSlot().getId().equals(existing.getId()))
                            .toList();
                    bookingRepository.deleteAll(existingBookings);
                    slotAvailabilityRepository.delete(existing);
                });

        // Create an exclusive slot with capacity = 1
        SlotAvailability slot = new SlotAvailability(testOrg, slotDate, MealSlot.LUNCH, 1);
        slot.setBookedCount(0);
        slot = slotAvailabilityRepository.save(slot);
        testSlots.add(slot);

        final Long slotId = slot.getId();
        final int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger slotFullCount = new AtomicInteger(0);
        AtomicInteger otherFailureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Long donorId = testDonors.get(i).getId();
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    // Block until all 10 threads are initialized and ready to execute
                    startLatch.await();

                    BookingRequestDto request = new BookingRequestDto(
                            slotId,
                            Occasion.BIRTHDAY,
                            30,
                            "Concurrent birthday lunch booking test"
                    );

                    bookingService.book(slotId, donorId, request);
                    successCount.incrementAndGet();
                } catch (SlotFullException ex) {
                    slotFullCount.incrementAndGet();
                } catch (Exception ex) {
                    otherFailureCount.incrementAndGet();
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait until all threads are running and blocked at startLatch
        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All worker threads should be ready");

        // Fire all 10 requests at the exact same instant
        startLatch.countDown();

        // Wait for all threads to terminate
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "All threads should complete within timeout");
        executorService.shutdown();

        // Reload slot from database to verify persistent state
        SlotAvailability finalSlot = slotAvailabilityRepository.findById(slotId).orElseThrow();

        // Assertions:
        // 1. Exactly one thread succeeded in reserving the single spot
        assertEquals(1, successCount.get(), "Exactly 1 concurrent thread should succeed");

        // 2. Exactly 9 threads were rejected due to slot capacity being full
        assertEquals(9, slotFullCount.get(), "Exactly 9 threads should fail with SlotFullException");

        // 3. Zero unexpected errors or deadlocks
        assertEquals(0, otherFailureCount.get(), "No unexpected exceptions or deadlocks should occur");

        // 4. Database bookedCount must be exactly 1 and never exceed capacity
        assertEquals(1, finalSlot.getBookedCount(), "Slot bookedCount must be exactly 1");
        assertTrue(finalSlot.getBookedCount() <= finalSlot.getCapacity(), "bookedCount must not exceed capacity");
    }

    @Test
    @DisplayName("Concurrency Test: 10 threads compete for 2 spots -> Exactly 2 succeed, 8 fail with SlotFullException")
    void testConcurrentBooking_SharedSlot_CapacityTwo() throws InterruptedException {
        LocalDate slotDate = LocalDate.now().plusDays(46);
        slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(testOrg.getId(), slotDate, MealSlot.DINNER)
                .ifPresent(existing -> {
                    List<Booking> existingBookings = bookingRepository.findAll().stream()
                            .filter(b -> b.getSlot().getId().equals(existing.getId()))
                            .toList();
                    bookingRepository.deleteAll(existingBookings);
                    slotAvailabilityRepository.delete(existing);
                });

        // Create a shared slot with capacity = 2
        SlotAvailability slot = new SlotAvailability(testOrg, slotDate, MealSlot.DINNER, 2);
        slot.setBookedCount(0);
        slot = slotAvailabilityRepository.save(slot);
        testSlots.add(slot);

        final Long slotId = slot.getId();
        final int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger slotFullCount = new AtomicInteger(0);
        AtomicInteger otherFailureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Long donorId = testDonors.get(i).getId();
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    BookingRequestDto request = new BookingRequestDto(
                            slotId,
                            Occasion.ANNIVERSARY,
                            25,
                            "Concurrent anniversary dinner booking test"
                    );

                    bookingService.book(slotId, donorId, request);
                    successCount.incrementAndGet();
                } catch (SlotFullException ex) {
                    slotFullCount.incrementAndGet();
                } catch (Exception ex) {
                    otherFailureCount.incrementAndGet();
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All worker threads should be ready");
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "All threads should complete within timeout");
        executorService.shutdown();

        SlotAvailability finalSlot = slotAvailabilityRepository.findById(slotId).orElseThrow();

        // Assertions:
        assertEquals(2, successCount.get(), "Exactly 2 concurrent threads should succeed");
        assertEquals(8, slotFullCount.get(), "Exactly 8 threads should fail with SlotFullException");
        assertEquals(0, otherFailureCount.get(), "No unexpected exceptions or deadlocks should occur");
        assertEquals(2, finalSlot.getBookedCount(), "Slot bookedCount must be exactly 2");
        assertTrue(finalSlot.getBookedCount() <= finalSlot.getCapacity(), "bookedCount must not exceed capacity");
    }
}
