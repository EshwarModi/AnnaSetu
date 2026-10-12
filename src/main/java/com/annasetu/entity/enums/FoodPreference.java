package com.annasetu.entity.enums;

/**
 * Responsibility:
 * Expresses dietary restrictions and preferences accepted by an organization.
 *
 * Domain Context:
 * - VEG: Only vegetarian food accepted (common for senior citizen homes or religious trusts).
 * - NON_VEG: Non-vegetarian food accepted.
 * - BOTH: Both vegetarian and non-vegetarian menus are accepted and accommodated.
 *
 * Interview Talking Point:
 * Helps avoid food wastage and ensures cultural and health compliance before the donor books a slot.
 */
public enum FoodPreference {
    VEG,
    NON_VEG,
    BOTH
}
