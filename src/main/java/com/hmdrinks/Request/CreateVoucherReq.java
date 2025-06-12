package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.Status_Voucher;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherReq {
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    private String keyVoucher;
    @DecimalMin(value = "0.01", inclusive = true, message = "discount lớn hơn 0")
    private Double discount;
    @Min(value = 1, message = "Number phải lớn hơn 0")
    private int number;
    private Status_Voucher status;
    @Min(value = 1, message = "PostId phải lớn hơn 0")
    private int postId;
}

