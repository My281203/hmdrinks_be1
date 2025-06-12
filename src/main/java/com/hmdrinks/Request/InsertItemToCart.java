package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
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
public class InsertItemToCart {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "cartId phải lớn hơn 0")
    private int cartId;
    @Min(value = 1, message = "proId phải lớn hơn 0")
    private int proId;

    private Size size;
    private int quantity;
    Language language;
}
