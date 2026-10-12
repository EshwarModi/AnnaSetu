package com.annasetu.dto;

import com.annasetu.entity.enums.FoodPreference;
import com.annasetu.entity.enums.MealSlot;
import com.annasetu.entity.enums.OrganizationType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Responsibility:
 * Data Transfer Object binding donor search and discovery query parameters.
 *
 * Domain & Architecture Context:
 * - Collects multi-dimensional filtering criteria: geography (city, area, pincode),
 *   institutional classification (orphanage, elder care, shelter), dietary restrictions (VEG, NON_VEG, BOTH),
 *   and calendar availability requirements (specific date, meal window).
 * - Feeds directly into {@link com.annasetu.repository.specification.OrganizationSpecification}.
 *
 * Interview Talking Points:
 * 1. Encapsulated Filter State: Binds cleanly to Spring MVC query string parameters.
 * 2. Sensible Defaults: Normalizes pagination parameters and parses ISO-8601 calendar dates safely.
 */
public class OrganizationSearchCriteria {

    private String city;
    private String areaOrPincode;
    private OrganizationType type;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private MealSlot mealSlot;
    private FoodPreference foodPreference;

    private int page = 0;
    private int size = 6;

    public OrganizationSearchCriteria() {
    }

    public OrganizationSearchCriteria(String city, String areaOrPincode, OrganizationType type,
                                      LocalDate date, MealSlot mealSlot, FoodPreference foodPreference) {
        this.city = city;
        this.areaOrPincode = areaOrPincode;
        this.type = type;
        this.date = date;
        this.mealSlot = mealSlot;
        this.foodPreference = foodPreference;
    }

    public boolean hasActiveFilters() {
        return (city != null && !city.isBlank()) ||
                (areaOrPincode != null && !areaOrPincode.isBlank()) ||
                type != null ||
                date != null ||
                mealSlot != null ||
                foodPreference != null;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAreaOrPincode() {
        return areaOrPincode;
    }

    public void setAreaOrPincode(String areaOrPincode) {
        this.areaOrPincode = areaOrPincode;
    }

    public OrganizationType getType() {
        return type;
    }

    public void setType(OrganizationType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public void setMealSlot(MealSlot mealSlot) {
        this.mealSlot = mealSlot;
    }

    public FoodPreference getFoodPreference() {
        return foodPreference;
    }

    public void setFoodPreference(FoodPreference foodPreference) {
        this.foodPreference = foodPreference;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size > 0 ? size : 6;
    }

    @Override
    public String toString() {
        return "OrganizationSearchCriteria{" +
                "city='" + city + '\'' +
                ", areaOrPincode='" + areaOrPincode + '\'' +
                ", type=" + type +
                ", date=" + date +
                ", mealSlot=" + mealSlot +
                ", foodPreference=" + foodPreference +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}
