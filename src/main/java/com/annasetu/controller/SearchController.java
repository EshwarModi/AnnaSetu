package com.annasetu.controller;

import com.annasetu.dto.OrganizationSearchCriteria;
import com.annasetu.dto.OrganizationSearchResultDto;
import com.annasetu.dto.SlotAvailabilityResponseDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.OrganizationType;
import com.annasetu.service.OrganizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility:
 * Presentation controller managing public donor search, multi-criteria filtering,
 * paginated institution discovery, and institution detail views.
 *
 * Domain & Architecture Context:
 * - Publicly accessible to unauthenticated visitors and donors.
 * - Enforces the invariant: only approved organizations with free slots (bookedCount < capacity)
 *   matching the selected criteria are returned.
 * - Renders the organization detail page preloaded with server-side slot availability for progressive enhancement.
 *
 * Interview Talking Points:
 * 1. Pagination Integration: Converts web query parameters into Spring Data {@link Pageable} requests,
 *    feeding results seamlessly to reusable Thymeleaf pagination fragments.
 * 2. Progressive Enhancement: Detail page loads initial slot availability server-side while providing
 *    vanilla JavaScript client-side fetch for dynamic calendar switching.
 * 3. Geolocation Without API Keys: Calculates external Google Maps Universal Search URLs cleanly for each institution.
 */
@Controller
public class SearchController {

    private final OrganizationService organizationService;

    public SearchController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Search & Discovery: Filters approved organizations by city, area, pincode, type, date, meal, and diet.
     */
    @GetMapping("/search")
    public String search(@ModelAttribute("criteria") OrganizationSearchCriteria criteria,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "size", defaultValue = "6") int size,
                         Model model) {
        criteria.setPage(page);
        criteria.setSize(size);

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), Sort.by(Sort.Direction.ASC, "name"));
        Page<OrganizationSearchResultDto> results = organizationService.searchOrganizations(criteria, pageable);

        model.addAttribute("results", results);
        model.addAttribute("criteria", criteria);
        model.addAttribute("cities", organizationService.getAvailableCities());
        model.addAttribute("orgTypes", OrganizationType.values());
        model.addAttribute("mealSlots", MealSlot.values());
        model.addAttribute("foodPreferences", FoodPreference.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("pageTitle", "Find Food Donation Slots - AnnaSetu");
        return "search";
    }

    /**
     * Organization Detail View: Displays institution mission, location, and interactive meal slot picker.
     */
    @GetMapping("/organizations/{id}")
    public String viewOrganization(@PathVariable("id") Long id,
                                   @RequestParam(value = "date", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   Model model) {
        OrganizationProfile org = organizationService.getApprovedOrganizationById(id);

        LocalDate selectedDate = (date != null && !date.isBefore(LocalDate.now())) ? date : LocalDate.now();
        List<SlotAvailabilityResponseDto> initialSlots = organizationService.getSlotAvailabilityForDate(id, selectedDate);

        String mapsQuery = String.join(", ", org.getName(), org.getAddress(), org.getArea(), org.getCity(), org.getPincode());
        String googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" +
                URLEncoder.encode(mapsQuery, StandardCharsets.UTF_8);

        model.addAttribute("org", org);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("initialSlots", initialSlots);
        model.addAttribute("googleMapsUrl", googleMapsUrl);
        model.addAttribute("pageTitle", org.getName() + " - AnnaSetu");
        return "organization/detail";
    }
}
