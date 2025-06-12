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
public class GetVoucherReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
    @Min(value = 1, message = "VoucherId phải lớn hơn 0")
    private Integer voucherId;
}
