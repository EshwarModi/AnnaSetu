package com.annasetu.entity;

import com.annasetu.entity.enums.MealSlot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Responsibility:
 * Represents a discrete calendar meal donation slot for an organization, tracking total and reserved capacity.
 *
 * Domain & Architecture Context:
 * - A meal slot represents a specific organization, date, and meal window (BREAKFAST, LUNCH, DINNER).
 * - Enforces business capacity rules: an organization can accept 1 or more simultaneous donors per meal
 *   (e.g., capacity=2 means 2 separate families can sponsor the lunch feast together).
 * - Prevents overbooking through the database-level CHECK constraint (booked_count <= capacity) and
 *   strict concurrency control.
 *
 * Interview Talking Points:
 * 1. Composite Unique Constraint: Enforces {@code UNIQUE(organization_id, slot_date, meal_slot)} so that
 *    an organization cannot inadvertently define duplicate slots for the same date and meal time.
 * 2. Strategic Indexing: Index on {@code slot_date} and composite {@code (organization_id, slot_date)}
 *    allows near-instantaneous monthly calendar rendering without full table scans.
 * 3. Domain helper methods: {@code isAvailable()} and {@code getRemainingCapacity()} encapsulate business logic.
 * 4. Hibernate 6 Enum Mapping: Uses @JdbcTypeCode(SqlTypes.VARCHAR) for portable VARCHAR schema validation.
 */
@Entity
@Table(
        name = "slot_availabilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_slot_org_date_meal",
                        columnNames = {"organization_id", "slot_date", "meal_slot"}
                )
        },
        indexes = {
                @Index(name = "idx_slot_date", columnList = "slot_date"),
                @Index(name = "idx_slot_org_date", columnList = "organization_id, slot_date")
        }
)
public class SlotAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationProfile organization;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "meal_slot", nullable = false, length = 20)
    private MealSlot mealSlot;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SlotAvailability() {
    }

    public SlotAvailability(OrganizationProfile organization, LocalDate slotDate,
                            MealSlot mealSlot, Integer capacity) {
        this(organization, slotDate, mealSlot, capacity, 0);
    }

    public SlotAvailability(OrganizationProfile organization, LocalDate slotDate,
                            MealSlot mealSlot, Integer capacity, Integer bookedCount) {
        this.organization = organization;
        this.slotDate = slotDate;
        this.mealSlot = mealSlot;
        this.capacity = capacity;
        this.bookedCount = bookedCount;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (bookedCount == null) {
            bookedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this slot has remaining donor capacity.
     */
    public boolean isAvailable() {
        return bookedCount != null && capacity != null && bookedCount < capacity;
    }

    /**
     * Computes the number of donor slots still open for booking.
     */
    public int getRemainingCapacity() {
        if (capacity == null || bookedCount == null) return 0;
        return Math.max(0, capacity - bookedCount);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrganizationProfile getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationProfile organization) {
        this.organization = organization;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public MealSlot getMealSlot() {
        return mealSlot;
    }

    public void setMealSlot(MealSlot mealSlot) {
        this.mealSlot = mealSlot;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Integer bookedCount) {
        this.bookedCount = bookedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotAvailability that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SlotAvailability{" +
                "id=" + id +
                ", slotDate=" + slotDate +
                ", mealSlot=" + mealSlot +
                ", capacity=" + capacity +
                ", bookedCount=" + bookedCount +
                '}';
    }
}
