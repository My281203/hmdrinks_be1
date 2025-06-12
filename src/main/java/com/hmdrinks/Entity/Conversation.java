package com.hmdrinks.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "conversations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conversation {
    @Id
    private String id;
    private String customerId;
    private String shipperId;
    private String shipmentId;
    private String lastMessage;
    private Long lastMessageAt;
    private Long createdAt;
    private Long updatedAt;
    private boolean _destroy;
}
