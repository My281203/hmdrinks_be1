package com.hmdrinks.Service;

import com.hmdrinks.Enum.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductVariantProjection1 {
    private int varId;
    private Size size;
    private double price;
    private int stock;
    private Boolean isDeleted;
    private LocalDateTime dateCreated;
    private LocalDateTime dateUpdated;
    private LocalDateTime dateDeleted;

    public ProductVariantProjection1(int varId, Size size, double price, int stock, Boolean isDeleted,
                                     LocalDateTime dateCreated, LocalDateTime dateUpdated, LocalDateTime dateDeleted) {
        this.varId = varId;
        this.size = size;
        this.price = price;
        this.stock = stock;
        this.isDeleted = isDeleted;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateDeleted = dateDeleted;
    }
}
