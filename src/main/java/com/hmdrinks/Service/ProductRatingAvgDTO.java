package com.hmdrinks.Service;

import io.swagger.models.auth.In;

public class ProductRatingAvgDTO {
    private Integer productId;
    private Double avgRating;

    public ProductRatingAvgDTO(Integer productId, Double avgRating) {
        this.productId = productId;
        this.avgRating = avgRating;
    }

    public Integer getProductId() {
        return productId;
    }

    public Double getAvgRating() {
        return avgRating;
    }
}
