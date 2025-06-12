package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;

import lombok.Setter;
import org.springframework.lang.NonNull;

@Data
@Getter
@Setter
public class NotificationRequest {
    @NonNull
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private Integer userId;
    @NonNull
    @Min(value = 1, message = "ShipmentId phải lớn hơn 0")
    private Integer shipmentId;

    @NonNull
    private String message;

    private Boolean isRead = false;

    private Integer groupOrderId;

    public NotificationRequest() {
    }
    public NotificationRequest(Integer userId, Integer shipmentId, String message) {
        this.userId = userId;
        this.shipmentId = shipmentId;
        this.message = message;
        this.isRead = false;
    }
}
