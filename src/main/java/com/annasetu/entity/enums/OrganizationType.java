package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Categorizes the beneficiary organizations registered on AnnaSetu.
 *
 * Domain Context:
 * Enables donors to filter and choose organizations based on their specific giving intent:
 * - ORPHANAGE: Children's shelter homes and orphanages.
 * - OLD_AGE_HOME: Assisted living elder care homes.
 * - SHELTER: Homeless night shelters, destitute homes, and rehabilitation centers.
 * - OTHER: Community kitchens, rural education centers, or animal/mixed shelters.
 *
 * Interview Talking Point:
 * Using an enum persisted as {@code @Enumerated(EnumType.STRING)} avoids magic numbers and
 * maintains database readability during auditing and reporting queries.
 */
public enum OrganizationType {
    ORPHANAGE,
    OLD_AGE_HOME,
    SHELTER,
    OTHER
}
