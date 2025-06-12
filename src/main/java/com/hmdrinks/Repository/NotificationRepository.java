package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
//    List<Notification> findByUser_UserIdOrderByTimeDesc(Integer userId);
//    List<Notification> findByUser_UserIdAndIsReadFalseOrderByTimeDesc(Integer userId);
    List<Notification> findByUserUserIdOrderByTimeDesc(Integer userId);
    List<Notification> findByUserUserIdAndIsReadFalseOrderByTimeDesc(Integer userId);
    @Modifying
    @Transactional
//    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId")
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId")
    void markAllNotificationsAsRead(Integer userId);

    List<Notification> findByUserUserIdAndTypeInOrderByTimeDesc(Integer userId, List<String> types);


}
