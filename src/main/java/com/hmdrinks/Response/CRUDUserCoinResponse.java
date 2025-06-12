package com.hmdrinks.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.Status_Voucher;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRUDUserCoinResponse {
    private Integer userCoinId;
    private float pointCoin;
    private  Integer userId;

}
