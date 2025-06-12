package com.hmdrinks.Request;

import io.swagger.models.auth.In;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class ConfirmCancelOrderReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
    @Min(value = 1, message = "orderId phải lớn hơn 0")
    private Integer orderId;
}
