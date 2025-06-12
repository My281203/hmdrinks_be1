package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AddItemOrderConfirmRequest {
    @Min(value = 1, message = "orderId phải lớn hơn 0")
    private Integer orderId;
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
}
