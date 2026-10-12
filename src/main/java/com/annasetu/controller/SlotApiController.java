package com.annasetu.controller;

import com.annasetu.dto.SlotAvailabilityResponseDto;
import com.annasetu.service.OrganizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsibility:
 * RESTful Web Service controller serving JSON slot availability data for asynchronous frontend updates.
 *
 * Domain & Architecture Context:
 * - Powers client-side vanilla JavaScript fetch calls on the organization detail page when donors toggle calendar dates.
 * - Decouples front-end UI state updates from full-page reloads, providing snappy, app-like responsiveness.
 *
 * Interview Talking Points:
 * 1. REST Architecture & Content Negotiation: Produces standard {@code application/json} payloads consumed
 *    directly by the browser's native {@code fetch()} API without heavy third-party UI libraries.
 * 2. Complete Enum Availability: Returns structured status for Breakfast, Lunch, and Dinner, explicitly signaling
 *    whether slots are open, limited, fully booked, or not configured.
 */
@RestController
@RequestMapping("/api")
public class SlotApiController {

    private final OrganizationService organizationService;

    public SlotApiController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * JSON Endpoint: Returns real-time slot availability for an organization on a specific calendar date.
     */
    @GetMapping("/organizations/{id}/slots")
    public ResponseEntity<List<SlotAvailabilityResponseDto>> getOrganizationSlots(
            @PathVariable("id") Long id,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<SlotAvailabilityResponseDto> slots = organizationService.getSlotAvailabilityForDate(id, queryDate);
        return ResponseEntity.ok(slots);
    }

    /**
     * Convenient query-param alternative: /api/slots?organizationId=...&date=...
     */
    @GetMapping("/slots")
    public ResponseEntity<List<SlotAvailabilityResponseDto>> getSlotsByQuery(
            @RequestParam("organizationId") Long organizationId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<SlotAvailabilityResponseDto> slots = organizationService.getSlotAvailabilityForDate(organizationId, queryDate);
        return ResponseEntity.ok(slots);
    }
}
