package com.annasetu.entity;

import com.annasetu.entity.enums.BookingStatus;
import com.annasetu.entity.enums.Occasion;
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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Responsibility:
 * Represents the transactional reservation contract between a Donor and an Organization's Meal Slot.
 *
 * Domain & Architecture Context:
 * - Records the donor's intent to sponsor food on a specific occasion (e.g., child's birthday).
 * - Tracks guest/family member attendance (peopleCount) and proposed menu (foodDescription).
 * - Manages the booking lifecycle state (PENDING -> CONFIRMED / REJECTED / CANCELLED / COMPLETED).
 *
 * Interview Talking Points:
 * 1. Business Constraint: {@code UNIQUE(slot_id, donor_id)} prevents accidental double-booking by the same
 *    donor on the same meal slot.
 * 2. Referential Integrity: Many-to-One relationships with {@link SlotAvailability} and {@link DonorProfile}
 *    with lazy fetching to prevent unintended cartesian joins during batch queries.
 * 3. Lifecycle State Pattern: The status transitions drive capacity recalculation and notifications.
 * 4. Hibernate 6 Enum Mapping: Uses @JdbcTypeCode(SqlTypes.VARCHAR) for portable VARCHAR schema validation.
 */
@Entity
@Table(
        name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booking_slot_donor",
                        columnNames = {"slot_id", "donor_id"}
                )
        },
        indexes = {
                @Index(name = "idx_booking_status", columnList = "status")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private SlotAvailability slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private DonorProfile donor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "occasion", nullable = false, length = 30)
    private Occasion occasion;

    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @Column(name = "food_description", columnDefinition = "TEXT")
    private String foodDescription;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Booking() {
    }

    public Booking(SlotAvailability slot, DonorProfile donor, Occasion occasion,
                   Integer peopleCount, String foodDescription, BookingStatus status) {
        this.slot = slot;
        this.donor = donor;
        this.occasion = occasion;
        this.peopleCount = peopleCount;
        this.foodDescription = foodDescription;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SlotAvailability getSlot() {
        return slot;
    }

    public void setSlot(SlotAvailability slot) {
        this.slot = slot;
    }

    public DonorProfile getDonor() {
        return donor;
    }

    public void setDonor(DonorProfile donor) {
        this.donor = donor;
    }

    public Occasion getOccasion() {
        return occasion;
    }

    public void setOccasion(Occasion occasion) {
        this.occasion = occasion;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public void setFoodDescription(String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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
        if (!(o instanceof Booking booking)) return false;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", occasion=" + occasion +
                ", peopleCount=" + peopleCount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
