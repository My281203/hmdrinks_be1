package com.hmdrinks.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class fetchAllInformationGroupOrderResponse {
    private Integer groupOrderId;
    CRUDGroupOrderResponse groupOrderDetail;
    CRUDShipmentResponse shipmentGroupDetail;
    CRUDPaymentGroupResponse paymentDetail;
    List<CRUDGroupOrderMemberDetailResponse> listMemberDetail;
    List<CRUDCartGroupResponse> listDetailCartGroup;
}
