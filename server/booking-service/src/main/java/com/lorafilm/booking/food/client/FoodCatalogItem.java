package com.lorafilm.booking.food.client;

import com.lorafilm.booking.food.enums.ProductType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_food_catalog_items")
public class FoodCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType type;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean sellable;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private boolean disabled;

    @Column(nullable = false, length = 10)
    private String currency;

    public FoodCatalogItem() {
    }

    public FoodCatalogItem(Long id, String code, String name, ProductType type, String imageUrl, BigDecimal price, boolean active, boolean sellable, boolean deleted, boolean disabled, String currency) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.imageUrl = imageUrl;
        this.price = price;
        this.active = active;
        this.sellable = sellable;
        this.deleted = deleted;
        this.disabled = disabled;
        this.currency = currency;
    }

    public FoodCatalogItem(Long id, String code, String name, ProductType type, String imageUrl, BigDecimal price) {
        this(id, code, name, type, imageUrl, price, true, true, false, false, "VND");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSellable() {
        return sellable;
    }

    public void setSellable(boolean sellable) {
        this.sellable = sellable;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
