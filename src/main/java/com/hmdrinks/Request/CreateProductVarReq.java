package com.hmdrinks.Request;


import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductVarReq {
    @Min(value = 1, message = "proId phải lớn hơn 0")
    private int proId;
    private Size size;
    private Double price;
    @Min(value = 0, message = "stock phải lớn hơn 0")
    private int stock;
}
