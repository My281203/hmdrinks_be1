package com.hmdrinks.Request;

import com.hmdrinks.Enum.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlacklistGroupReq {
    @Min(value = 1, message = "GroupOrderId phải lớn hơn 0")
    private  Integer groupOrderId;
    @Min(value = 1, message = "leaderId phải lớn hơn 0")
    private  Integer leaderId;
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private  Integer userId;
}
