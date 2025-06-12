package com.hmdrinks.Response;

import lombok.Data;

@Data
public class ConversationResponse {
    private String id;
    private Integer customerId;
    private Integer shipperId;
    private String shipmentId;
    private String lastMessage;
    private Long lastMessageAt;
}
