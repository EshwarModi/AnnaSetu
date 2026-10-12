package com.annasetu.service;

import com.annasetu.dto.SlotCreationDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility:
 * Concrete business implementation for {@link SlotService}.
 *
 * Domain & Architecture Context:
 * - Governs meal slot lifecycle, concurrency validation, and capacity rules for food donation centers.
 * - Enforces critical invariants:
 *   1. Only APPROVED organizations can create, modify, or block slots.
 *   2. Slots cannot be created on past dates.
 *   3. End date must be on or after start date, within a 90-day maximum generation window.
 *   4. Capacity cannot be reduced below active donor commitments (bookedCount).
 *   5. Slots or dates with active donor bookings cannot be removed or blocked.
 *
 * Interview Talking Points:
 * 1. Concurrency and Invariant Protection: Re-checks bookedCount within a database transaction before committing
 *    capacity reductions to prevent race conditions from violating DB check constraints.
 * 2. Idempotent Slot Generation: If a slot already exists for a (date, mealSlot) tuple, updates capacity safely
 *    if bookedCount allows, avoiding duplicate key constraint errors (uk_slot_org_date_meal).
 */
@Service
@Transactional(readOnly = true)
public class SlotServiceImpl implements SlotService {

    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final OrganizationProfileRepository organizationProfileRepository;

    public SlotServiceImpl(SlotAvailabilityRepository slotAvailabilityRepository,
                           OrganizationProfileRepository organizationProfileRepository) {
        this.slotAvailabilityRepository = slotAvailabilityRepository;
        this.organizationProfileRepository = organizationProfileRepository;
    }

    private OrganizationProfile verifyApprovedOrganization(Long organizationId) {
        OrganizationProfile org = organizationProfileRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessRuleException("Organization account is currently " + org.getApprovalStatus()
                    + ". Slot management is restricted until administrative approval is granted.");
        }
        return org;
    }

    @Override
    public List<SlotAvailability> getUpcomingSlots(Long organizationId) {
        return slotAvailabilityRepository
                .findByOrganizationIdAndSlotDateGreaterThanEqualOrderBySlotDateAscMealSlotAsc(
                        organizationId, LocalDate.now());
    }

    @Override
    public List<SlotAvailability> getSlotsForDateRange(Long organizationId, LocalDate startDate, LocalDate endDate) {
        return slotAvailabilityRepository
                .findByOrganizationIdAndSlotDateBetweenOrderBySlotDateAscMealSlotAsc(
                        organizationId, startDate, endDate);
    }

    @Override
    @Transactional
    public int createSlotsForDateRange(Long organizationId, SlotCreationDto dto) {
        OrganizationProfile org = verifyApprovedOrganization(organizationId);

        LocalDate startDate = dto.getStartDate();
        LocalDate endDate = dto.getEndDate();
        LocalDate today = LocalDate.now();

        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Start date and end date must both be provided.");
        }

        if (startDate.isBefore(today)) {
            throw new BusinessRuleException("Cannot create slots for past dates. Earliest allowed date is today (" + today + ").");
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date (" + endDate + ") cannot be earlier than start date (" + startDate + ").");
        }

        if (startDate.plusDays(90).isBefore(endDate)) {
            throw new BusinessRuleException("Date range exceeds the maximum allowed window of 90 days.");
        }

        if (dto.getMealSlots() == null || dto.getMealSlots().isEmpty()) {
            throw new BusinessRuleException("At least one meal slot (Breakfast, Lunch, or Dinner) must be selected.");
        }

        Integer capacity = dto.getCapacity();
        if (capacity == null || capacity < 1 || capacity > 2) {
            throw new BusinessRuleException("Capacity per slot must be either 1 (Exclusive) or 2 (Shared).");
        }

        int affectedCount = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (MealSlot mealSlot : dto.getMealSlots()) {
                var existingOpt = slotAvailabilityRepository
                        .findByOrganizationIdAndSlotDateAndMealSlot(organizationId, date, mealSlot);

                if (existingOpt.isPresent()) {
                    SlotAvailability existing = existingOpt.get();
                    // Do not lower capacity below active bookings
                    if (existing.getBookedCount() > capacity) {
                        // Preserve existing higher capacity to protect active bookings
                        continue;
                    }
                    existing.setCapacity(capacity);
                    slotAvailabilityRepository.save(existing);
                    affectedCount++;
                } else {
                    SlotAvailability newSlot = new SlotAvailability(org, date, mealSlot, capacity, 0);
                    slotAvailabilityRepository.save(newSlot);
                    affectedCount++;
                }
            }
        }

        return affectedCount;
    }

    @Override
    @Transactional
    public SlotAvailability updateSlotCapacity(Long organizationId, Long slotId, Integer newCapacity) {
        verifyApprovedOrganization(organizationId);

        SlotAvailability slot = slotAvailabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: " + slotId));

        if (!slot.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Access denied: This slot does not belong to your organization.");
        }

        if (slot.getSlotDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Cannot modify capacity for historical dates.");
        }

        if (newCapacity == null || newCapacity < 1 || newCapacity > 2) {
            throw new BusinessRuleException("Capacity must be either 1 (Exclusive) or 2 (Shared).");
        }

        if (newCapacity < slot.getBookedCount()) {
            throw new BusinessRuleException("Cannot reduce capacity to " + newCapacity
                    + ". This slot already has " + slot.getBookedCount() + " confirmed donor booking(s).");
        }

        slot.setCapacity(newCapacity);
        return slotAvailabilityRepository.save(slot);
    }

    @Override
    @Transactional
    public void deleteSlot(Long organizationId, Long slotId) {
        verifyApprovedOrganization(organizationId);

        SlotAvailability slot = slotAvailabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: " + slotId));

        if (!slot.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Access denied: This slot does not belong to your organization.");
        }

        if (slot.getBookedCount() > 0) {
            throw new BusinessRuleException("Cannot delete slot on " + slot.getSlotDate()
                    + " (" + slot.getMealSlot() + ") because it has " + slot.getBookedCount()
                    + " active donor booking(s).");
        }

        slotAvailabilityRepository.delete(slot);
    }

    @Override
    @Transactional
    public int blockDate(Long organizationId, LocalDate date) {
        verifyApprovedOrganization(organizationId);

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Cannot block past dates. Date must be today or in the future.");
        }

        List<SlotAvailability> slots = slotAvailabilityRepository.findByOrganizationIdAndSlotDate(organizationId, date);

        if (slots.isEmpty()) {
            return 0;
        }

        boolean hasActiveBookings = slots.stream().anyMatch(s -> s.getBookedCount() > 0);
        if (hasActiveBookings) {
            throw new BusinessRuleException("Cannot block date " + date
                    + " because one or more meal slots have confirmed donor bookings. Please review reservations first.");
        }

        int removedCount = slots.size();
        slotAvailabilityRepository.deleteAll(slots);
        return removedCount;
    }

    @Override
    public long countUpcomingSlots(Long organizationId) {
        return slotAvailabilityRepository.countByOrganizationIdAndSlotDateGreaterThanEqual(organizationId, LocalDate.now());
    }

    @Override
    public long countBookedUpcomingSlots(Long organizationId) {
        return slotAvailabilityRepository.countByOrganizationIdAndSlotDateGreaterThanEqualAndBookedCountGreaterThan(
                organizationId, LocalDate.now(), 0);
    }
}
