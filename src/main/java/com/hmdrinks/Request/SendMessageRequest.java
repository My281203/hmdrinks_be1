package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.List;

@Data
public class SendMessageRequest {
    @Min(value = 1, message = "SenderId phải lớn hơn 0")
    private Integer senderId;
    @Min(value = 1, message = "ReceiverId phải lớn hơn 0")
    private Integer receiverId;
    @Min(value = 1, message = "ShipmentId phải lớn hơn 0")
    private Integer shipmentId;
    private String message;
    private String messageType;
    private List<String> attachments;
}
