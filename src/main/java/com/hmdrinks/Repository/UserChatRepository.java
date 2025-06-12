package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Product;
import com.hmdrinks.Entity.UserChatHistory;
import com.hmdrinks.Entity.UserCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserChatRepository extends JpaRepository<UserChatHistory,Integer> {


    UserChatHistory findByUserChatId(int userId);

    @Query("SELECT u FROM UserChatHistory u WHERE u.user.userId = :userId AND u.isDeleted = false ORDER BY u.dateCreated DESC")
    List<UserChatHistory> findByUserUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") int userId);

    List<UserChatHistory> findByUserUserIdAndIsDeletedFalse(int userId);

    UserChatHistory findByUserUserIdAndUserChatId(int userId,int userChatId);
    UserChatHistory findByChatNameAndUserChatIdNot(String chatName, Integer chatId);
}
