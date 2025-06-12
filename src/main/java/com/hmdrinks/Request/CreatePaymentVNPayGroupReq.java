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
public class CreatePaymentVNPayGroupReq {
   @Min(value = 1, message = "GroupOrderId phải lớn hơn 0")
   private Integer groupOrderId;
   @Min(value = 1, message = "LeaderUserId phải lớn hơn 0")
   private Integer leaderUserId;
   private  String ipAddress;
   private String type;

}
