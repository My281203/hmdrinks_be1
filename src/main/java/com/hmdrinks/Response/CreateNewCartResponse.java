package com.hmdrinks.Response;

import com.hmdrinks.Enum.Status_Cart;
import lombok.*;

@Data   // Dùng @Data là đủ, không cần thêm @Getter, @Setter nữa
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNewCartResponse {
    private int cartId;
    private double price;              // sửa Price -> price
    private int totalProduct;
    private int userId;
    private Status_Cart statusCart;

    public CreateNewCartResponse(Integer cartId, Double totalPrice, Integer totalProduct, Integer userId, String status) {
        this.cartId = cartId != null ? cartId : 0;
        this.price = totalPrice != null ? totalPrice : 0.0;
        this.totalProduct = totalProduct != null ? totalProduct : 0;
        this.userId = userId != null ? userId : 0;
        this.statusCart = status != null ? Status_Cart.valueOf(status) : null;
    }

    public CreateNewCartResponse(Integer cartId, Double totalPrice, Integer totalProduct, Integer userId) {
        this.cartId = cartId != null ? cartId : 0;
        this.price = totalPrice != null ? totalPrice : 0.0;
        this.totalProduct = totalProduct != null ? totalProduct : 0;
        this.userId = userId != null ? userId : 0;
    }
}
