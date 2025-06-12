package com.hmdrinks.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.StatusGroupOrderMember;
import com.hmdrinks.Enum.TypePayment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class previewPaymentGroupOrderResponse {
    private int groupOrderId;
    private Double deliveryFee;
    private Double groupMemberDiscount;
    private Double subtotal;
    private Double totalPrice;
    private Integer quantity;
    List<CRUDCartGroupResponse> crudCartGroupResponse;
}
