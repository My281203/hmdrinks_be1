package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrdersReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "cartId phải lớn hơn 0")
    private int cartId;
    private String voucherId;
    private float pointCoinUse;
    private String note;
    Language language;
}
