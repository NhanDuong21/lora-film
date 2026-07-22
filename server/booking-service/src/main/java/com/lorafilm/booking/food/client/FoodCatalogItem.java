package com.lorafilm.booking.food.client;

import com.lorafilm.booking.food.enums.ProductType;
import java.math.BigDecimal;

public class FoodCatalogItem {

    private Long id;
    private String code;
    private String name;
    private ProductType type;
    private String imageUrl;
    private BigDecimal price;
    private boolean active;
    private boolean sellable;
    private boolean deleted;
    private boolean disabled;
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
