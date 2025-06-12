package com.hmdrinks.Controller;

import com.hmdrinks.Entity.Notification;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Repository.NotificationRepository;
import com.hmdrinks.Request.NotificationRequest;
import com.hmdrinks.Response.NotificationResponse;
import com.hmdrinks.Service.NotificationService;
import com.hmdrinks.Service.UserService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.models.auth.In;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    public NotificationController(NotificationService notificationService, NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody @Valid  NotificationRequest notificationRequest) {
//        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, notificationRequest.getUserId());
//
//        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
//            return authResponse;
//        }
        Integer userId = notificationRequest.getUserId();
        Integer shipmentId = notificationRequest.getShipmentId();
        String message = notificationRequest.getMessage();
        NotificationResponse response = notificationService.sendNotification(userId, shipmentId, message);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/send-join-group")
    public ResponseEntity<NotificationResponse> sendNotificationJoinGroup(@RequestBody @Valid  NotificationRequest notificationRequest) {
//        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, notificationRequest.getUserId());
//
//        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
//            return authResponse;
//        }
        Integer userId = notificationRequest.getUserId();
        Integer shipmentId = notificationRequest.getGroupOrderId();
        String message = notificationRequest.getMessage();
        NotificationResponse response = notificationService.sendGroupJoinNotification(userId,shipmentId, message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-member-leave")
    public ResponseEntity<NotificationResponse> sendNotificationMemberLeaveGroup(@RequestBody @Valid  NotificationRequest notificationRequest) {
//        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, notificationRequest.getUserId());
//
//        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
//            return authResponse;
//        }
        Integer userId = notificationRequest.getUserId();
        Integer shipmentId = notificationRequest.getGroupOrderId();
        String message = notificationRequest.getMessage();
        NotificationResponse response = notificationService.sendMemberGroupLeaveNotification(userId,shipmentId, message);
        return ResponseEntity.ok(response);
    }

    public class NotificationDTO {
        private Integer id;
        private Integer userId;
        private LocalDateTime time;
        private Boolean isRead;
        private String message;
        private Integer shipmentId;

        public NotificationDTO(Notification notification) {
            this.id = notification.getId();
            this.userId = (notification.getUser() != null) ? notification.getUser().getUserId() : null;
            this.time = notification.getTime();
            this.isRead = notification.getIsRead();
            this.message = notification.getMessage();
            this.shipmentId = notification.getShipmentId();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getNotifications(@PathVariable Integer userId) {

        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));

    }
    @Transactional
    @PutMapping("/read/{notificationId}")
    public ResponseEntity<String> markNotificationAsRead(@PathVariable Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Thông báo đã được đánh dấu là đã đọc.");
    }
    @PutMapping("/read/all/{userId}")
    public ResponseEntity<String> markAllNotificationsAsRead(@PathVariable Integer userId) {
        notificationRepository.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok("Tất cả thông báo đã được đánh dấu là đã đọc.");
    }
}

