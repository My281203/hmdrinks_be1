package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncreaseDecreaseItemQuantityReq {
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "cartItemId phải lớn hơn 0")
    private int cartItemId;
    private int quantity;

}
