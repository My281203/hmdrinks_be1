package com.hmdrinks.Response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OrderCancelPaymentRefund {
    CreateOrdersResponse order;
    CRUDPaymentResponse payment;
    List<CRUDCartItemResponse> listItem;
}
