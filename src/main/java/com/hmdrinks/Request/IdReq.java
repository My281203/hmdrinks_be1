package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class IdReq {
    @Min(value = 1, message = "Id phải lớn hơn 0")
    private Integer id;

}
