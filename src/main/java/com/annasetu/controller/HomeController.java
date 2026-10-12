package com.annasetu.controller;

import com.annasetu.dto.PlatformStatsDto;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.service.PlatformStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Responsibility:
 * Web controller handling the public root landing page of AnnaSetu.
 *
 * Domain & Architecture Context:
 * - Coordinates between the HTTP presentation layer and {@link PlatformStatsService}.
 * - Prepares the Thymeleaf view model with platform impact figures and approved organizations.
 *
 * Interview Talking Points:
 * 1. Strict Layered Architecture: Delegates business calculations to {@link PlatformStatsService}
 *    without touching repositories or raw entities directly.
 * 2. Model View Separation: Binds display-ready attributes into Spring's {@link Model} for SSR rendering.
 */
@Controller
public class HomeController {

    private final PlatformStatsService platformStatsService;

    public HomeController(PlatformStatsService platformStatsService) {
        this.platformStatsService = platformStatsService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        PlatformStatsDto stats = platformStatsService.getPlatformStats();
        List<OrganizationProfile> featuredOrganizations = platformStatsService.getFeaturedOrganizations();
        List<String> cities = platformStatsService.getAvailableCities();

        model.addAttribute("stats", stats);
        model.addAttribute("organizations", featuredOrganizations);
        model.addAttribute("cities", cities);
        model.addAttribute("mealSlots", com.annasetu.entity.enums.MealSlot.values());
        model.addAttribute("pageTitle", "AnnaSetu - Food Donation Slot Booking Platform");

        return "home";
    }
}
