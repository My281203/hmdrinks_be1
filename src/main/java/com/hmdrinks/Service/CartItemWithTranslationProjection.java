package com.hmdrinks.Service;

import com.hmdrinks.Enum.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
public class CartItemWithTranslationProjection {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String size;
    private Double totalPrice;
    private Integer quantity;
    private String thumbnail;

    public CartItemWithTranslationProjection(Long cartItemId, Long productId, String productName,
                                             String size, Double totalPrice, Integer quantity,
                                             String thumbnail) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.size = size;
        this.totalPrice = totalPrice;
        this.quantity = quantity;
        this.thumbnail = thumbnail;
    }

    // getters (có thể cần cho Jackson hoặc để debug)
}




