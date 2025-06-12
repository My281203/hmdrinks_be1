package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsertItemToCartLeader {
    @Min(value = 1, message = "groupOrderId phải lớn hơn 0")
    private int groupOrderId;
    @Min(value = 1, message = "UserIdMember phải lớn hơn 0")
    private int userIdMember;
    @Min(value = 1, message = "UserIdLeader phải lớn hơn 0")
    private int userIdLeader;
    @Min(value = 1, message = "CartId phải lớn hơn 0")
    private int cartId;
    @Min(value = 1, message = "ProId phải lớn hơn 0")
    private int proId;
    private Size size;
    @Min(value = 1, message = "Quantity phải lớn hơn 0")
    private int quantity;
    Language language;
}
