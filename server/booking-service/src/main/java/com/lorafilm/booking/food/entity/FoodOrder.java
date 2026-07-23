package com.lorafilm.booking.food.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.enums.FoodOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import com.lorafilm.booking.booking.entity.Booking;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "booking_food_orders")
public class FoodOrder extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodOrderItem> items = new ArrayList<>();

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

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public FoodOrder() {
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

    public List<FoodOrderItem> getItems() {
        return items;
    }

    public void setItems(List<FoodOrderItem> items) {
        this.items = items;
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

    public void recalculateTotals() {
        this.totalQuantity = items.stream().mapToInt(FoodOrderItem::getQuantity).sum();
        this.subtotal = items.stream()
                .map(FoodOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Ensure discountAmount is initialized if null
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        this.finalAmount = this.subtotal.subtract(this.discountAmount);
    }

    public void addItem(FoodCatalogItem catalogItem, int quantity) {
        Optional<FoodOrderItem> existingItemOpt = this.items.stream()
                .filter(i -> i.getProductId().equals(catalogItem.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            FoodOrderItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.recalculateSubtotal();
        } else {
            FoodOrderItem newItem = new FoodOrderItem();
            newItem.setFoodOrder(this);
            newItem.setProductId(catalogItem.getId());
            newItem.setProductCode(catalogItem.getCode());
            newItem.setProductName(catalogItem.getName());
            newItem.setProductType(catalogItem.getType());
            newItem.setProductImage(catalogItem.getImageUrl());
            newItem.setUnitPrice(catalogItem.getPrice());
            newItem.setQuantity(quantity);
            newItem.setDiscountAmount(BigDecimal.ZERO);
            newItem.recalculateSubtotal();
            this.items.add(newItem);
        }
        this.recalculateTotals();
    }

    public void updateItemQuantity(Long itemId, int quantity) {
        Optional<FoodOrderItem> existingItemOpt = this.items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            FoodOrderItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(quantity);
            existingItem.recalculateSubtotal();
            this.recalculateTotals();
        }
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
