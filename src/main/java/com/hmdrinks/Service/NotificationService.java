package com.hmdrinks.Service;

import com.hmdrinks.Config.MyWebSocketHandler;
import com.hmdrinks.Entity.Notification;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Repository.NotificationRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Response.CRUDNotificationResponse;
import com.hmdrinks.Response.CreateNewCartResponse;
import com.hmdrinks.Response.ListNotificationResponse;
import com.hmdrinks.Response.NotificationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.Int;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    @Autowired
    private  NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    private final MyWebSocketHandler myWebSocketHandler;
    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository, MyWebSocketHandler myWebSocketHandler) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.myWebSocketHandler = myWebSocketHandler;
    }


    @Transactional
    public NotificationResponse sendNotification(Integer userId, Integer shipmentId, String message) {
        LocalDateTime time = LocalDateTime.now();
        if (userId == null)
        {
            throw new IllegalArgumentException("UserId không được null");
        }
        Notification notification = new Notification();
        User user = userRepository.findByUserId(Integer.parseInt(String.valueOf(userId)));
        notification.setUser(user);
        notification.setShipmentId(shipmentId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setGroupOrderId(null);
        notification.setType("NEW_NOTIFICATION");
        notificationRepository.save(notification);
        String destination = "/topic/shipper/" + userId;
        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", notification.getShipmentId());
        notificationMap.put("message", notification.getMessage());
        notificationMap.put("time", notification.getTime());
        notificationMap.put("isRead", notification.getIsRead());
        notificationMap.put("userId", notification.getUser().getUserId());
        notificationMap.put("groupOrderId", null);
        notificationMap.put("type","NEW_NOTIFICATION");
        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần
        System.out.println("websocket thuan" + userId + message + shipmentId + "New_notifi");
        myWebSocketHandler.sendMessageToUser(userId, message, shipmentId, "NEW_NOTIFICATION");
       return new NotificationResponse(userId, shipmentId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendGroupJoinNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("NEW_GROUP_JOIN");
        notificationRepository.save(notification);

        String destination = "/topic/group/leader/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","NEW_GROUP_JOIN");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "NEW_GROUP_JOIN");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendProductUpdateNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("UPDATE_CART");
        notificationRepository.save(notification);

        String destination = "/topic/group/member/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","UPDATE_CART");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "UPDATE_CART");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendProductLeaderUpdateNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("UPDATE_CART_LEADER");
        notificationRepository.save(notification);

        String destination = "/topic/group/member/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","UPDATE_CART");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "UPDATE_CART_LEADER");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendCheckoutNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("CHECKOUT");
        notificationRepository.save(notification);

        String destination = "/topic/group/member/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","UPDATE_CART");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "CHECKOUT");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendMemberGroupLeaveNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("MEMBER_LEFT_GROUP");
        notificationRepository.save(notification);

        String destination = "/topic/group/member/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","MEMBER_LEFT_GROUP");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "MEMBER_LEFT_GROUP");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }

    @Transactional
    public NotificationResponse sendMemberDeleteByLeaderNotification(Integer leaderId, Integer groupOrderId, String message) {
        LocalDateTime time = LocalDateTime.now();

        if (leaderId == null) {
            throw new IllegalArgumentException("Leader ID không được null");
        }

        Notification notification = new Notification();
        User leader = userRepository.findByUserId(leaderId);
        notification.setUser(leader);
        notification.setShipmentId(null);
        notification.setGroupOrderId(groupOrderId);
        notification.setMessage(message);
        notification.setTime(time);
        notification.setIsRead(false);
        notification.setType("MEMBER_KICKED");
        notificationRepository.save(notification);

        String destination = "/topic/group/member/" + leaderId;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("shipmentId", null);
        notificationMap.put("groupOrderId", groupOrderId);
        notificationMap.put("message", message);
        notificationMap.put("time", time);
        notificationMap.put("isRead", false);
        notificationMap.put("userId", leaderId);
        notificationMap.put("type","MEMBER_KICKED");

        // Gửi qua STOMP WebSocket
        messagingTemplate.convertAndSend(destination, notificationMap);

        // Gửi qua WebSocket thuần nếu cần
        myWebSocketHandler.sendMessageToUser(leaderId, message, groupOrderId, "MEMBER_KICKED");

        return new NotificationResponse(leaderId, groupOrderId, message, time, false);
    }


//    @Transactional
//    public ResponseEntity<?> getNotificationsByUser(Integer userId) {
//        List<Notification> notification =notificationRepository.findByUserUserIdOrderByTimeDesc(userId);
//        List<CRUDNotificationResponse> notifications = new ArrayList<>();
//        for (Notification notifications1: notification){
//            notifications.add(new CRUDNotificationResponse(
//                    notifications1.getId(),
//                    notifications1.getUser().getUserId(),
//                    notifications1.getShipmentId(),
//                    notifications1.getMessage(),
//                    notifications1.getTime(),
//                    notifications1.getIsRead()
//
//            ));
//        }
//        return ResponseEntity.ok(new ListNotificationResponse(
//                notifications.size(),
//                notifications
//        ));
//    }

    @Transactional
    public ResponseEntity<?> getNotificationsByUser(Integer userId) {
        List<String> allowedTypes = Arrays.asList(
                "NEW_NOTIFICATION",
                "NEW_GROUP_JOIN",
                "MEMBER_LEFT_GROUP",
                "MEMBER_KICKED"
        );

        List<Notification> notificationList = notificationRepository
                .findByUserUserIdAndTypeInOrderByTimeDesc(userId, allowedTypes);

        List<CRUDNotificationResponse> notifications = new ArrayList<>();
        for (Notification n : notificationList) {
            notifications.add(new CRUDNotificationResponse(
                    n.getId(),
                    n.getUser().getUserId(),
                    n.getShipmentId(),
                    n.getMessage(),
                    n.getTime(),
                    n.getIsRead()
            ));
        }

        return ResponseEntity.ok(new ListNotificationResponse(
                notifications.size(),
                notifications
        ));
    }


}
