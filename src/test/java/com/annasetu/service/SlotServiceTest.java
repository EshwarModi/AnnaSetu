package com.annasetu.service;

import com.annasetu.dto.SlotCreationDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Responsibility:
 * Unit tests validating business rules and domain invariants of {@link SlotServiceImpl}.
 *
 * Domain & Architecture Context:
 * - Asserts that unapproved organizations cannot publish or modify slots.
 * - Asserts that past dates, reversed ranges, and ranges exceeding 90 days are strictly rejected.
 * - Asserts that slot capacity can never be reduced below the active {@code bookedCount}.
 * - Asserts that slots or dates with confirmed bookings cannot be deleted or blocked.
 *
 * Interview Talking Points:
 * 1. Isolated Unit Testing: Employs Mockito to verify service business rules without database overhead.
 * 2. Boundary Condition Coverage: Systematically exercises date bounds, capacity thresholds, and status guards.
 */
@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;

    @Mock
    private OrganizationProfileRepository organizationProfileRepository;

    @InjectMocks
    private SlotServiceImpl slotService;

    private OrganizationProfile approvedOrg;
    private OrganizationProfile pendingOrg;

    @BeforeEach
    void setUp() {
        approvedOrg = new OrganizationProfile();
        approvedOrg.setId(10L);
        approvedOrg.setName("Asha Kiran Orphanage");
        approvedOrg.setApprovalStatus(ApprovalStatus.APPROVED);

        pendingOrg = new OrganizationProfile();
        pendingOrg.setId(20L);
        pendingOrg.setName("Shanti Elder Care");
        pendingOrg.setApprovalStatus(ApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("createSlotsForDateRange: Generates slots for approved organization")
    void testCreateSlots_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));
        when(slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        SlotCreationDto dto = new SlotCreationDto(start, end, List.of(MealSlot.LUNCH, MealSlot.DINNER), 2);

        int count = slotService.createSlotsForDateRange(10L, dto);

        assertEquals(4, count); // 2 days * 2 meal slots
        verify(slotAvailabilityRepository, times(4)).save(any(SlotAvailability.class));
    }

    @Test
    @DisplayName("createSlotsForDateRange: Throws exception when organization is PENDING approval")
    void testCreateSlots_RejectsPendingOrganization() {
        when(organizationProfileRepository.findById(20L)).thenReturn(Optional.of(pendingOrg));

        SlotCreationDto dto = new SlotCreationDto(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                List.of(MealSlot.LUNCH), 1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.createSlotsForDateRange(20L, dto));

        assertTrue(ex.getMessage().contains("Slot management is restricted until administrative approval"));
        verify(slotAvailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSlotsForDateRange: Throws exception when start date is in the past")
    void testCreateSlots_RejectsPastDates() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotCreationDto dto = new SlotCreationDto(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2),
                List.of(MealSlot.LUNCH), 1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.createSlotsForDateRange(10L, dto));

        assertTrue(ex.getMessage().contains("Cannot create slots for past dates"));
    }

    @Test
    @DisplayName("createSlotsForDateRange: Throws exception when end date is earlier than start date")
    void testCreateSlots_RejectsReversedDates() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotCreationDto dto = new SlotCreationDto(
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(2),
                List.of(MealSlot.LUNCH), 1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.createSlotsForDateRange(10L, dto));

        assertTrue(ex.getMessage().contains("cannot be earlier than start date"));
    }

    @Test
    @DisplayName("createSlotsForDateRange: Throws exception when range exceeds 90 days")
    void testCreateSlots_RejectsRangeExceeding90Days() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotCreationDto dto = new SlotCreationDto(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(95),
                List.of(MealSlot.LUNCH), 1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.createSlotsForDateRange(10L, dto));

        assertTrue(ex.getMessage().contains("maximum allowed window of 90 days"));
    }

    @Test
    @DisplayName("createSlotsForDateRange: Preserves higher capacity when slot already has confirmed bookings")
    void testCreateSlots_PreservesCapacityWhenBooked() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        LocalDate targetDate = LocalDate.now().plusDays(3);
        SlotAvailability existingBookedSlot = new SlotAvailability(approvedOrg, targetDate, MealSlot.LUNCH, 2, 2);

        when(slotAvailabilityRepository.findByOrganizationIdAndSlotDateAndMealSlot(10L, targetDate, MealSlot.LUNCH))
                .thenReturn(Optional.of(existingBookedSlot));

        // Attempting to batch-set capacity to 1
        SlotCreationDto dto = new SlotCreationDto(targetDate, targetDate, List.of(MealSlot.LUNCH), 1);

        int count = slotService.createSlotsForDateRange(10L, dto);

        assertEquals(0, count); // Did not overwrite capacity because bookedCount (2) > requested capacity (1)
        assertEquals(2, existingBookedSlot.getCapacity());
        verify(slotAvailabilityRepository, never()).save(existingBookedSlot);
    }

    @Test
    @DisplayName("updateSlotCapacity: Successfully updates capacity from 1 to 2")
    void testUpdateSlotCapacity_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotAvailability slot = new SlotAvailability(approvedOrg, LocalDate.now().plusDays(5), MealSlot.DINNER, 1, 0);
        slot.setId(101L);

        when(slotAvailabilityRepository.findById(101L)).thenReturn(Optional.of(slot));
        when(slotAvailabilityRepository.save(any(SlotAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SlotAvailability updated = slotService.updateSlotCapacity(10L, 101L, 2);

        assertNotNull(updated);
        assertEquals(2, updated.getCapacity());
        verify(slotAvailabilityRepository).save(slot);
    }

    @Test
    @DisplayName("updateSlotCapacity: Invariant rejects reducing capacity below active bookedCount")
    void testUpdateSlotCapacity_RejectsCapacityBelowBookedCount() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        // Slot has capacity 2, and 2 active bookings
        SlotAvailability slot = new SlotAvailability(approvedOrg, LocalDate.now().plusDays(5), MealSlot.LUNCH, 2, 2);
        slot.setId(102L);

        when(slotAvailabilityRepository.findById(102L)).thenReturn(Optional.of(slot));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.updateSlotCapacity(10L, 102L, 1));

        assertTrue(ex.getMessage().contains("Cannot reduce capacity to 1") &&
                   ex.getMessage().contains("already has 2 confirmed donor booking(s)"));
        assertEquals(2, slot.getCapacity());
        verify(slotAvailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSlotCapacity: Rejects modification of slots belonging to another organization")
    void testUpdateSlotCapacity_RejectsOtherOrgSlot() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        OrganizationProfile otherOrg = new OrganizationProfile();
        otherOrg.setId(999L);

        SlotAvailability otherSlot = new SlotAvailability(otherOrg, LocalDate.now().plusDays(5), MealSlot.LUNCH, 1, 0);
        otherSlot.setId(103L);

        when(slotAvailabilityRepository.findById(103L)).thenReturn(Optional.of(otherSlot));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.updateSlotCapacity(10L, 103L, 2));

        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("deleteSlot: Deletes unbooked slot successfully")
    void testDeleteSlot_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotAvailability slot = new SlotAvailability(approvedOrg, LocalDate.now().plusDays(5), MealSlot.BREAKFAST, 1, 0);
        slot.setId(104L);

        when(slotAvailabilityRepository.findById(104L)).thenReturn(Optional.of(slot));

        slotService.deleteSlot(10L, 104L);

        verify(slotAvailabilityRepository).delete(slot);
    }

    @Test
    @DisplayName("deleteSlot: Rejects deletion of slot with active confirmed bookings")
    void testDeleteSlot_RejectsWhenBookingsExist() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        SlotAvailability slot = new SlotAvailability(approvedOrg, LocalDate.now().plusDays(5), MealSlot.BREAKFAST, 1, 1);
        slot.setId(105L);

        when(slotAvailabilityRepository.findById(105L)).thenReturn(Optional.of(slot));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.deleteSlot(10L, 105L));

        assertTrue(ex.getMessage().contains("because it has 1 active donor booking(s)"));
        verify(slotAvailabilityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("blockDate: Removes unbooked slots on specified date")
    void testBlockDate_Success() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        LocalDate blockTarget = LocalDate.now().plusDays(10);
        SlotAvailability slot1 = new SlotAvailability(approvedOrg, blockTarget, MealSlot.BREAKFAST, 1, 0);
        SlotAvailability slot2 = new SlotAvailability(approvedOrg, blockTarget, MealSlot.LUNCH, 1, 0);

        when(slotAvailabilityRepository.findByOrganizationIdAndSlotDate(10L, blockTarget))
                .thenReturn(List.of(slot1, slot2));

        int removed = slotService.blockDate(10L, blockTarget);

        assertEquals(2, removed);
        verify(slotAvailabilityRepository).deleteAll(List.of(slot1, slot2));
    }

    @Test
    @DisplayName("blockDate: Rejects blocking when any slot on that date has active bookings")
    void testBlockDate_RejectsWhenActiveBookingsExist() {
        when(organizationProfileRepository.findById(10L)).thenReturn(Optional.of(approvedOrg));

        LocalDate blockTarget = LocalDate.now().plusDays(10);
        SlotAvailability unbookedSlot = new SlotAvailability(approvedOrg, blockTarget, MealSlot.BREAKFAST, 1, 0);
        SlotAvailability bookedSlot = new SlotAvailability(approvedOrg, blockTarget, MealSlot.LUNCH, 1, 1);

        when(slotAvailabilityRepository.findByOrganizationIdAndSlotDate(10L, blockTarget))
                .thenReturn(List.of(unbookedSlot, bookedSlot));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                slotService.blockDate(10L, blockTarget));

        assertTrue(ex.getMessage().contains("because one or more meal slots have confirmed donor bookings"));
        verify(slotAvailabilityRepository, never()).deleteAll(any());
    }
}
