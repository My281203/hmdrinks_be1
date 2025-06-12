package com.hmdrinks.Request;

import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSizeItemLeaderReq {
    @Min(value = 1, message = "GroupOrderId phải lớn hơn 0")
    private int groupOrderId;
    @Min(value = 1, message = "UserIdMember phải lớn hơn 0")
    private int userIdMember;
    @Min(value = 1, message = "UserIdLeader phải lớn hơn 0")
    private int userIdLeader;
    @Min(value = 1, message = "cartItemId phải lớn hơn 0")
    private int cartItemId;
    private Size size;

}
