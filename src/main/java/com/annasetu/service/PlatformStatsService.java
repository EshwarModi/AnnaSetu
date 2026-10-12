package com.annasetu.service;

import com.annasetu.dto.PlatformStatsDto;
import com.annasetu.entity.OrganizationProfile;

import java.util.List;

/**
 * Responsibility:
 * Business service contract for platform-wide statistics and public discovery landing data.
 *
 * Domain & Architecture Context:
 * - Sits between Web Controllers and Data Access Repositories.
 * - Gathers summary metrics demonstrating community impact to visiting donors.
 *
 * Interview Talking Points:
 * 1. Interface-Driven Design: Decouples controller consumers from concrete service implementation,
 *    facilitating easy unit testing and mocking.
 * 2. Layered Isolation: Ensures controllers do not directly query repositories.
 */
public interface PlatformStatsService {

    PlatformStatsDto getPlatformStats();

    List<OrganizationProfile> getFeaturedOrganizations();

    List<String> getAvailableCities();
}
