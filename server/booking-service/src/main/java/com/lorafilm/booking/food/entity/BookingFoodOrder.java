package com.lorafilm.booking.food.entity;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.common.entity.BaseEntity;
import com.lorafilm.booking.food.enums.FoodOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "booking_food_orders")
public class BookingFoodOrder extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingFoodItem> items = new ArrayList<>();

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FoodOrderStatus status = FoodOrderStatus.PENDING;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BookingFoodOrder() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public FoodOrderStatus getStatus() {
        return status;
    }

    public void setStatus(FoodOrderStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BookingFoodItem> getItems() {
        return items;
    }

    public void setItems(List<BookingFoodItem> items) {
        this.items = items;
    }
}
