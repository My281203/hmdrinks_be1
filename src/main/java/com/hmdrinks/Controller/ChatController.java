package com.hmdrinks.Controller;

import com.hmdrinks.Entity.Message;
import com.hmdrinks.Request.SendMessageRequest;
import com.hmdrinks.Service.ChatService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<Message> sendMessage(@RequestBody @Valid  SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request));
    }
    @GetMapping("/messages/shipment/{shipmentId}")
    public ResponseEntity<List<Message>> getMessagesByShipmentId(@PathVariable String shipmentId) {

        return ResponseEntity.ok(chatService.getMessagesByShipmentId(shipmentId));
    }
    }
