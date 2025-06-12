package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.Status_Voucher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactReq {

    private String email;
    private String phone;
    private String fullName;
    private String description;
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
}

