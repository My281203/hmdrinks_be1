package com.hmdrinks.Request;

import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItem {
    @Min(value = 1, message = "cartItemId phải lớn hơn 0")
    private int cartItemId;

    @Min(value = 1, message = "CartId phải lớn hơn 0")
    private int cartId;

    @Min(value = 1, message = "ProId phải lớn hơn 0")
    private int proId;
    private Size size;

    @Min(value = 0, message = "Quantity phải lớn hơn 0")
    private int quantity;
}
