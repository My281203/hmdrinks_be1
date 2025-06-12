package com.hmdrinks.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {
    @Id
    private String id;
    private String senderId;
    private String receiverId;
    private String shipmentId;
    private String conversationId;
    private String message;
    private String messageType;
    private List<String> attachments;
    private boolean read;
    private Long createdAt;
    private Long updatedAt;
    private boolean _destroy;
}
