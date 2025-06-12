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
public class UpdateNameGroupReq {
    private String nameGroup;
    @Min(value = 1, message = "GroupId phải lớn hơn 0")
    private Integer groupId;

    @Min(value = 1, message = "LeaderId phải lớn hơn 0")
    private  Integer leaderId;
}
