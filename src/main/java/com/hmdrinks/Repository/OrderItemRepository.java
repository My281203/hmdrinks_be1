package com.hmdrinks.Repository;

import com.hmdrinks.Entity.OrderItem;
import com.hmdrinks.Entity.Post;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
       OrderItem findByUserUserIdAndCartCartId(Integer userId, Integer cartId);
       @Transactional
       @Modifying
       @Query("UPDATE OrderItem oi SET oi.totalPrice = 0.0, oi.quantity = 0, oi.dateUpdated = CURRENT_TIMESTAMP WHERE oi.orderItemId = :orderItemId")
       void updateOrderItemAfterDelete(@Param("orderItemId") Integer orderItemId);
}
