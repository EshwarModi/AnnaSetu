package com.annasetu.dto;

import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.OrganizationType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Responsibility:
 * Presentation-layer view model representing an organization card in donor search results.
 *
 * Domain & Architecture Context:
 * - Aggregates verified institutional profile information, distance-free area metadata,
 *   calculated available slot metrics, and direct external navigation links.
 * - Computes a Google Maps query link without requiring a paid Maps API key.
 *
 * Interview Talking Points:
 * 1. Read-Optimized Projection: Decouples view rendering from deep entity navigation, ensuring fast rendering
 *    without lazy initialization hazards.
 * 2. Keyless External Mapping: Uses the public Universal Google Maps search URL scheme
 *    ({@code https://www.google.com/maps/search/?api=1&query=...}) for reliable geolocation navigation.
 */
public class OrganizationSearchResultDto {

    private Long id;
    private String name;
    private OrganizationType type;
    private String address;
    private String city;
    private String area;
    private String pincode;
    private Integer peopleServed;
    private FoodPreference foodPreference;
    private String description;
    private int availableSlotsCount;
    private String googleMapsUrl;
    private String viewAndBookUrl;

    public OrganizationSearchResultDto() {
    }

    public OrganizationSearchResultDto(OrganizationProfile org, int availableSlotsCount, LocalDate selectedDate) {
        this.id = org.getId();
        this.name = org.getName();
        this.type = org.getType();
        this.address = org.getAddress();
        this.city = org.getCity();
        this.area = org.getArea();
        this.pincode = org.getPincode();
        this.peopleServed = org.getPeopleServed();
        this.foodPreference = org.getFoodPreference();
        this.description = org.getDescription();
        this.availableSlotsCount = availableSlotsCount;

        // Build Google Maps Search URL (no API key required)
        String query = String.join(", ", org.getName(), org.getAddress(), org.getArea(), org.getCity(), org.getPincode());
        this.googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8);

        // Build View & Book URL with optional date preservation
        if (selectedDate != null) {
            this.viewAndBookUrl = "/organizations/" + org.getId() + "?date=" + selectedDate;
        } else {
            this.viewAndBookUrl = "/organizations/" + org.getId();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganizationType getType() {
        return type;
    }

    public void setType(OrganizationType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Integer getPeopleServed() {
        return peopleServed;
    }

    public void setPeopleServed(Integer peopleServed) {
        this.peopleServed = peopleServed;
    }

    public FoodPreference getFoodPreference() {
        return foodPreference;
    }

    public void setFoodPreference(FoodPreference foodPreference) {
        this.foodPreference = foodPreference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAvailableSlotsCount() {
        return availableSlotsCount;
    }

    public void setAvailableSlotsCount(int availableSlotsCount) {
        this.availableSlotsCount = availableSlotsCount;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getViewAndBookUrl() {
        return viewAndBookUrl;
    }

    public void setViewAndBookUrl(String viewAndBookUrl) {
        this.viewAndBookUrl = viewAndBookUrl;
    }
}
