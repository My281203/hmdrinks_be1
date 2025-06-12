package com.hmdrinks.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddVoucherPause {
   @Min(value = 1, message = "userId phải lớn hơn 0")
   private int userId;
   @Min(value = 1, message = "orderId phải lớn hơn 0")
   private Integer orderId;
   @Min(value = 1, message = "voucherId phải lớn hơn 0")
   private String voucherId;
}

