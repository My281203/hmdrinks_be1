package com.hmdrinks.Service;

import com.hmdrinks.Config.MyWebSocketHandler;
import com.hmdrinks.Entity.Conversation;
import com.hmdrinks.Entity.Message;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Repository.ConversationRepository;
import com.hmdrinks.Repository.MessageRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Request.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationRepository conversationRepository;
    private  final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MyWebSocketHandler myWebSocketHandler;

    public Conversation getOrCreateConversation(Integer customerId, Integer shipperId, String shipmentId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(customerId);
        if(user == null){
            return null;
        }
        User shipper = userRepository.findByUserIdAndIsDeletedFalse(shipperId);
        if(shipper == null){
            return null;
        }
        Optional<Conversation> conversationOpt = conversationRepository.findByShipmentId(shipmentId);
        return conversationOpt.orElseGet(() -> {
            Conversation conversation = Conversation.builder()
                    .customerId(String.valueOf(customerId))
                    .shipperId(String.valueOf(shipperId))
                    .shipmentId(shipmentId)
                    .lastMessage("")
                    .lastMessageAt(Instant.now().toEpochMilli())
                    .createdAt(Instant.now().toEpochMilli())
                    .updatedAt(null)
                    ._destroy(false)
                    .build();
            return conversationRepository.save(conversation);
        });
    }

    public Message sendMessage(SendMessageRequest request) {
        Conversation conversation = getOrCreateConversation(request.getSenderId(), request.getReceiverId(), String.valueOf(request.getShipmentId()));

        Message message = Message.builder()
                .senderId(String.valueOf(request.getSenderId()))
                .receiverId(String.valueOf(request.getReceiverId()))
                .shipmentId(String.valueOf(request.getShipmentId()))
                .conversationId(conversation.getId())
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .attachments(request.getAttachments())
                .read(false)
                .createdAt(Instant.now().toEpochMilli())
                .updatedAt(null)
                ._destroy(false)
                .build();

        messageRepository.save(message);

        // Cập nhật lastMessage của cuộc trò chuyện
        conversation.setLastMessage(request.getMessage());
        conversation.setLastMessageAt(Instant.now().toEpochMilli());
        conversationRepository.save(conversation);
        messagingTemplate.convertAndSend("/topic/chat/" + request.getShipmentId(), message);
        myWebSocketHandler.sendMessageToUser2(request.getSenderId(),request.getReceiverId(), request.getMessage(), request.getShipmentId(), "NEW_MESSAGE");
        return message;
    }

    public List<Message> getMessagesByShipmentId(String shipmentId) {
        return messageRepository.findByShipmentId(shipmentId);
    }

}
