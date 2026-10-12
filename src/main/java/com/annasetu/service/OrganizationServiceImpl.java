package com.annasetu.service;

import com.annasetu.dto.OrganizationProfileUpdateDto;
import com.annasetu.dto.OrganizationSearchCriteria;
import com.annasetu.dto.OrganizationSearchResultDto;
import com.annasetu.dto.SlotAvailabilityResponseDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.SlotAvailability;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.exception.ResourceNotFoundException;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import com.annasetu.repository.specification.OrganizationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsibility:
 * Concrete business implementation for {@link OrganizationService}.
 *
 * Domain & Architecture Context:
 * - Executes transactional profile modifications and guarantees that security-critical fields
 *   (e.g., approvalStatus, user foreign key) are never mutated during self-service profile updates.
 * - Executes multi-parameter search queries via {@link OrganizationSpecification} and projects
 *   results to {@link OrganizationSearchResultDto} view models with available slot counts and Google Maps links.
 * - Powers live AJAX slot checking for the organization detail view.
 *
 * Interview Talking Points:
 * 1. Transactional Integrity: Uses Spring's @Transactional(readOnly = true) at the class level and
 *    explicit @Transactional for mutating methods to ensure database atomicity.
 * 2. Specifications with Count Projections: Converts persistent entities into card view models enriched
 *    with matching capacity metrics via derived repository counts.
 * 3. Full Window Coverage: Populates missing meal slots as {@code notConfigured} so frontend buttons
 *    consistently render Breakfast, Lunch, and Dinner states.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationProfileRepository organizationProfileRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;

    public OrganizationServiceImpl(OrganizationProfileRepository organizationProfileRepository,
                                   SlotAvailabilityRepository slotAvailabilityRepository) {
        this.organizationProfileRepository = organizationProfileRepository;
        this.slotAvailabilityRepository = slotAvailabilityRepository;
    }

    @Override
    public Optional<OrganizationProfile> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return organizationProfileRepository.findByUserId(userId);
    }

    @Override
    public OrganizationProfile getOrganizationById(Long id) {
        return organizationProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
    }

    @Override
    public OrganizationProfile getApprovedOrganizationById(Long id) {
        OrganizationProfile org = getOrganizationById(id);
        if (org.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResourceNotFoundException("Organization is not approved or active with ID: " + id);
        }
        return org;
    }

    @Override
    public OrganizationProfile getOrganizationByUserId(Long userId) {
        return organizationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization profile not found for user ID: " + userId));
    }

    @Override
    @Transactional
    public OrganizationProfile updateOrganizationProfile(Long orgId, OrganizationProfileUpdateDto dto) {
        OrganizationProfile org = getOrganizationById(orgId);

        org.setName(dto.getName());
        org.setType(dto.getType());
        org.setContactPerson(dto.getContactPerson());
        org.setPhone(dto.getPhone());
        org.setAddress(dto.getAddress());
        org.setCity(dto.getCity());
        org.setArea(dto.getArea());
        org.setPincode(dto.getPincode());
        org.setPeopleServed(dto.getPeopleServed());
        org.setFoodPreference(dto.getFoodPreference());
        org.setDescription(dto.getDescription());

        return organizationProfileRepository.save(org);
    }

    @Override
    public boolean isOrganizationApproved(Long orgId) {
        return organizationProfileRepository.findById(orgId)
                .map(org -> org.getApprovalStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }

    @Override
    public OrganizationProfileUpdateDto toUpdateDto(OrganizationProfile profile) {
        OrganizationProfileUpdateDto dto = new OrganizationProfileUpdateDto();
        dto.setName(profile.getName());
        dto.setType(profile.getType());
        dto.setContactPerson(profile.getContactPerson());
        dto.setPhone(profile.getPhone());
        dto.setAddress(profile.getAddress());
        dto.setCity(profile.getCity());
        dto.setArea(profile.getArea());
        dto.setPincode(profile.getPincode());
        dto.setPeopleServed(profile.getPeopleServed());
        dto.setFoodPreference(profile.getFoodPreference());
        dto.setDescription(profile.getDescription());
        return dto;
    }

    @Override
    public Page<OrganizationSearchResultDto> searchOrganizations(OrganizationSearchCriteria criteria, Pageable pageable) {
        Page<OrganizationProfile> orgPage = organizationProfileRepository.findAll(
                OrganizationSpecification.withFilters(criteria), pageable);

        LocalDate filterDate = criteria != null ? criteria.getDate() : null;
        MealSlot filterMealSlot = criteria != null ? criteria.getMealSlot() : null;

        return orgPage.map(org -> {
            long count = slotAvailabilityRepository.countAvailableSlotsMatching(org.getId(), filterDate, filterMealSlot);
            return new OrganizationSearchResultDto(org, (int) count, filterDate);
        });
    }

    @Override
    public List<SlotAvailabilityResponseDto> getSlotAvailabilityForDate(Long organizationId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Ensure organization exists and is approved
        getApprovedOrganizationById(organizationId);

        List<SlotAvailability> slots = slotAvailabilityRepository.findByOrganizationIdAndSlotDate(organizationId, targetDate);

        Map<MealSlot, SlotAvailability> slotMap = slots.stream()
                .collect(Collectors.toMap(SlotAvailability::getMealSlot, Function.identity(), (s1, s2) -> s1));

        List<SlotAvailabilityResponseDto> result = new ArrayList<>();
        for (MealSlot meal : List.of(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)) {
            if (slotMap.containsKey(meal)) {
                result.add(SlotAvailabilityResponseDto.fromSlot(slotMap.get(meal)));
            } else {
                result.add(SlotAvailabilityResponseDto.notConfigured(meal, targetDate));
            }
        }

        return result;
    }

    @Override
    public List<String> getAvailableCities() {
        return organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED);
    }
}
