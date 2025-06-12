package com.hmdrinks.Request;


import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRUDProductVarReq {
    @Min(value = 1, message = "VarId phải lớn hơn 0")
    private int varId;
    @Min(value = 1, message = "ProId phải lớn hơn 0")
    private int proId;
    private Size size;

    @DecimalMin(value = "0.01", inclusive = true, message = "Giá phải lớn hơn 0")
    private Double price;

    @Min(value = 0, message = "Stock phải lớn hơn 0")
    private int stock;
}
