package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentVNPayReq {
   @Min(value = 1, message = "OrderId phải lớn hơn 0")
   private Integer orderId;
   @Min(value = 1, message = "UserId phải lớn hơn 0")
   private Integer userId;
   private  String ipAddress;
   private String type;

}
