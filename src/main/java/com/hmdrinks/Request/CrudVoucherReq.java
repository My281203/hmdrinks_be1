package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.Status_Voucher;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class CrudVoucherReq {
    @Min(value = 1, message = "VoucherId phải lớn hơn 0")
    private int voucherId;
    private String key;

    @Min(value = 1, message = "number phải lớn hơn 0")
    private  int number;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;


    private Double discount;
    private Status_Voucher status;
    @Min(value = 1, message = "PostId phải lớn hơn 0")
    private int postId;
}
