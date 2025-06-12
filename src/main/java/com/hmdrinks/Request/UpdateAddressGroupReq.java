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
public class UpdateAddressGroupReq {
    private String address;
    @Min(value = 1, message = "GroupId phải lớn hơn 0")
    private Integer groupId;
    @Min(value = 1, message = "LeaderId phải lớn hơn 0")
    private  Integer leaderId;
}
