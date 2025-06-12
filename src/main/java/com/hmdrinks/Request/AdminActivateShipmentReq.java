package com.hmdrinks.Request;

import com.hmdrinks.Enum.Status_Shipment;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivateShipmentReq {
    @Min(value = 1, message = "shipmentId phải lớn hơn 0")
    private int shipmentId;
    Status_Shipment status;
}
