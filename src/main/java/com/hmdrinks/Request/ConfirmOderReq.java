package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOderReq {
    @Min(value = 1, message = "orderId phải lớn hơn 0")
    private int  orderId;
    private LocalDateTime timeConfirmed;
}
