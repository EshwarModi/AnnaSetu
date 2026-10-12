package com.annasetu.service;

import com.annasetu.dto.PlatformStatsDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.repository.BookingRepository;
import com.annasetu.repository.DonorProfileRepository;
import com.annasetu.repository.OrganizationProfileRepository;
import com.annasetu.repository.SlotAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Responsibility:
 * Concrete implementation of {@link PlatformStatsService}.
 *
 * Domain & Architecture Context:
 * - Aggregates data from multiple repositories under a read-only transactional boundary.
 * - Powers the landing page counters and featured organization carousel/cards.
 *
 * Interview Talking Points:
 * 1. {@code @Transactional(readOnly = true)}: Signals to Hibernate and the database engine that transactions
 *    are purely queries, optimizing dirty checking, snapshot memory footprint, and read locks.
 * 2. Constructor-based Dependency Injection: Eliminates field-injection hazards and simplifies test fixture creation.
 */
@Service
@Transactional(readOnly = true)
public class PlatformStatsServiceImpl implements PlatformStatsService {

    private final OrganizationProfileRepository organizationProfileRepository;
    private final SlotAvailabilityRepository slotAvailabilityRepository;
    private final BookingRepository bookingRepository;
    private final DonorProfileRepository donorProfileRepository;

    public PlatformStatsServiceImpl(OrganizationProfileRepository organizationProfileRepository,
                                   SlotAvailabilityRepository slotAvailabilityRepository,
                                   BookingRepository bookingRepository,
                                   DonorProfileRepository donorProfileRepository) {
        this.organizationProfileRepository = organizationProfileRepository;
        this.slotAvailabilityRepository = slotAvailabilityRepository;
        this.bookingRepository = bookingRepository;
        this.donorProfileRepository = donorProfileRepository;
    }

    @Override
    public PlatformStatsDto getPlatformStats() {
        long orgCount = organizationProfileRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long cityCount = organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED).size();
        long slotCount = slotAvailabilityRepository.count();
        long bookingCount = bookingRepository.count();
        long donorCount = donorProfileRepository.count();

        return new PlatformStatsDto(orgCount, cityCount, slotCount, bookingCount, donorCount);
    }

    @Override
    public List<OrganizationProfile> getFeaturedOrganizations() {
        return organizationProfileRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Override
    public List<String> getAvailableCities() {
        return organizationProfileRepository.findDistinctCitiesByApprovalStatus(ApprovalStatus.APPROVED);
    }
}
