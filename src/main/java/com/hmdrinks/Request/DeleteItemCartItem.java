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
public class DeleteItemCartItem {
    @Min(value = 1, message = "CartItemId phải lớn hơn 0")
    private int cartItemId;
}
