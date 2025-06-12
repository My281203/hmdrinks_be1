package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.TypeGroupOrder;
import com.hmdrinks.Enum.TypePayment;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JoinGroupReq {
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
    private String code;
    private TypePayment typePayment;
}
