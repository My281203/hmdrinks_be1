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
public class DeleteAllCartItemLeaderReq {
    @Min(value = 1, message = "GroupOrderId phải lớn hơn 0")
    private int groupOrderId;
    @Min(value = 1, message = "UserIdMember phải lớn hơn 0")
    private int userIdMember;
    @Min(value = 1, message = "UserIdLeader phải lớn hơn 0")
    private int userIdLeader;
    @Min(value = 1, message = "CartId phải lớn hơn 0")
    private int cartId;
}
