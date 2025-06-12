package com.hmdrinks.Response;

import com.hmdrinks.Enum.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CRUDCartItemGroupResponse {
    private  int cartItemGroupId;
    private  int proId;
    private String proName;
    private  int cartGroupId;
    private Size size;
    private Double itemPrice;
    private Double totalPrice;
    private  Integer quantity;
    private String imageUrl;
}
