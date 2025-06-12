package com.hmdrinks.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationResponse {
    private Integer userId;
    private Integer shipmentId;
    private String message;
    private LocalDateTime time;
    private Boolean isRead;
}

