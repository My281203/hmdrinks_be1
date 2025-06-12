package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Orders;
import com.hmdrinks.Enum.Status_Order;
import com.hmdrinks.Enum.Status_Payment;
import com.hmdrinks.Service.OrdersService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Orders, Integer> {


//  @Query("SELECT o FROM Orders o " +
//          "LEFT JOIN FETCH o.orderItem oi " +
//          "LEFT JOIN FETCH oi.cart c " +
//          "LEFT JOIN FETCH c.cartItems ci " +
//          "LEFT JOIN FETCH ci.productVariants pv " +
//          "LEFT JOIN FETCH pv.product p " +
//          "LEFT JOIN FETCH o.payment " +
//          "LEFT JOIN FETCH o.voucher " +
//          "WHERE o.user.userId = :userId AND o.status = :status")
//  List<Orders> fetchOrdersWithFullRelation(@Param("userId") int userId, @Param("status") Status_Order status);


  @Query("SELECT o FROM Orders o " +
          "JOIN FETCH o.user u " +
          "JOIN FETCH o.orderItem oi " +
          "JOIN FETCH oi.cart c " +
          "WHERE o.orderId = :orderId")
  Orders fetchFullOrderWithUserAndCart(@Param("orderId") int orderId);


  @Transactional
  @Modifying
  @Query("UPDATE Orders o SET o.totalPrice = 0.0, o.dateUpdated = CURRENT_TIMESTAMP WHERE o.orderId = :orderId")
  void updateOrderAfterDelete(@Param("orderId") Integer orderId);
  @Query("SELECT o FROM Orders o " +
          "JOIN FETCH o.orderItem oi " +
          "JOIN FETCH oi.cart c " +
          "LEFT JOIN FETCH c.cartItems ci " +
          "LEFT JOIN FETCH ci.productVariants pv " +
          "LEFT JOIN FETCH pv.product p " +
          "WHERE o.user.userId = :userId AND o.status = 'CONFIRMED'")
  List<Orders> findAllConfirmedWithItems(@Param("userId") int userId);







  long countByOrderDateBetweenAndStatus(LocalDateTime from, LocalDateTime to, Status_Order status);

  @Query("""
SELECT o FROM Orders o
JOIN FETCH o.orderItem oi
JOIN FETCH oi.cart c
JOIN FETCH c.cartItems ci
JOIN FETCH ci.productVariants pv
JOIN FETCH pv.product p
WHERE o.user.userId = :userId AND o.status = 'CONFIRMED'
""")
  List<Orders> fetchConfirmedOrdersWithDetails(@Param("userId") int userId);

  @Query("SELECT o FROM Orders o " +
          "JOIN FETCH o.orderItem oi " +
          "JOIN FETCH oi.cart c " +
          "JOIN FETCH c.cartItems ci " +
          "JOIN FETCH ci.productVariants pv " +
          "JOIN FETCH pv.product p " +
          "LEFT JOIN FETCH o.voucher v " +
          "WHERE o.user.userId = :userId AND o.status = :status AND o.payment IS NULL")
  List<Orders> findAllCanceledOrdersWithoutPayment(@Param("userId") int userId, @Param("status") Status_Order status);

//  @Query("SELECT o.orderId AS orderId, " +
//          "o.address AS address, " +
//          "o.deliveryFee AS deliveryFee, " +
//          "o.dateCreated AS dateCreated, " +
//          "o.dateDeleted AS dateDeleted, " +
//          "o.dateUpdated AS dateUpdated, " +
//          "o.deliveryDate AS deliveryDate, " +
//          "o.dateCanceled AS dateCanceled, " +
//          "o.discountPrice AS discountPrice, " +
//          "o.isDeleted AS isDeleted, " +
//          "o.note AS note, " +
//          "o.orderDate AS orderDate, " +
//          "o.phoneNumber AS phoneNumber, " +
//          "o.status AS status, " +
//          "o.totalPrice AS totalPrice, " +
//          "o.user.userId AS userId, " +
//          "v.voucherId AS voucherId, " +
//          "o.pointCoinUse AS pointCoinUse " +
//          "FROM Orders o " +
//          "LEFT JOIN o.voucher v " +
//          "WHERE o.user.userId = :userId AND o.status = 'CANCELLED'")
//  List<OrdersService.OrderCancelProjection> findAllCancelledOrders(@Param("userId") int userId);




  Orders findByOrderId(int id);
  @Query("SELECT o FROM Orders o " +
          "JOIN FETCH o.orderItem oi " +
          "JOIN FETCH oi.cart c " +
          "JOIN FETCH c.cartItems ci " +
          "JOIN FETCH ci.productVariants pv " +
          "JOIN FETCH pv.product p " +
          "LEFT JOIN FETCH o.payment pay " +
          "LEFT JOIN FETCH pay.shipment sh " +
          "LEFT JOIN FETCH o.voucher v " +
          "WHERE o.user.userId = :userId AND o.status = 'CANCELLED'")
  List<Orders> findAllCancelledOrdersWithDetails(@Param("userId") int userId, Sort by);
  @Query("SELECT o.orderId, o.user.userId, o.cancelReason FROM Orders o WHERE o.isDeleted = false AND o.cancelReason IS NOT NULL AND o.isCancelReason IS NULL")
  List<Object[]> findCancelReasonAwaitInfo();
  @Query("""
    SELECT o FROM Orders o
    JOIN FETCH o.payment p
    JOIN FETCH o.user u
    JOIN FETCH p.shipment s
    JOIN FETCH o.orderItem oi
    JOIN FETCH oi.cart c
    JOIN FETCH c.cartItems ci
    JOIN FETCH ci.productVariants pv
    JOIN FETCH pv.product prod
    WHERE u.userId = :userId
      AND o.status = 'CONFIRMED'
      AND s.isDeleted = false
      AND s.status = 'SHIPPING'
""")
  List<Orders> findConfirmedOrdersWithShipmentAndCartItems(@Param("userId") int userId);


  @Query("""
    SELECT o FROM Orders o
    JOIN FETCH o.orderItem oi
    JOIN FETCH oi.cart c
    JOIN FETCH c.cartItems ci
    JOIN FETCH ci.productVariants pv
    JOIN FETCH pv.product p
    JOIN FETCH o.payment pay
    LEFT JOIN FETCH o.voucher v
    WHERE o.user.userId = :userId AND o.status = :status AND pay.status = :paymentStatus
""")
  List<Orders> findAllCancelledAndRefundedOrdersWithDetails(
          @Param("userId") int userId,
          @Param("status") Status_Order status,
          @Param("paymentStatus") Status_Payment paymentStatus);

  @Query(value = """
    SELECT 
        o.order_id AS orderId, o.address, o.delivery_fee AS deliveryFee, o.date_created AS dateCreated,
        o.date_deleted AS dateDeleted, o.date_updated AS dateUpdated, o.delivery_date AS deliveryDate,
        o.date_canceled AS dateCanceled, o.discount_price AS discountPrice, o.is_deleted AS isDeleted,
        o.note, o.order_date AS orderDate, o.phone_number AS phoneNumber, o.status, o.total_price AS totalPrice,
        o.user_id AS userId, o.voucher_id AS voucherId, o.point_coin_use AS pointCoinUse,

        p.payment_id AS paymentId, p.amount, p.date_refunded AS dateRefunded, 
        p.payment_method AS paymentMethod, p.status AS paymentStatus, p.is_refund AS isRefund, p.link,

        ci.cart_item_id AS cartItemId, c.cart_id AS cartId, ci.size, ci.quantity, ci.total_price AS totalCartItemPrice,

        pr.pro_id AS productId, pr.pro_name AS proName, pr.list_pro_img AS proImg

    FROM orders o
    JOIN payment p ON p.order_id = o.order_id
    JOIN order_item oi ON oi.order_id = o.order_id
    JOIN cart c ON c.cart_id = oi.cart_id
    JOIN cart_item ci ON ci.cart_id = c.cart_id
    JOIN product_variants pv ON pv.var_id = ci.var_id
    JOIN product pr ON pr.pro_id = pv.pro_id

    WHERE o.user_id = :userId
      AND o.status = 'CANCELLED'
      AND p.status = 'REFUND'
""", nativeQuery = true)
  List<OrdersService.OrderRefundProjection> findAllRefundedOrdersWithDetails(@Param("userId") int userId);


  @Query("SELECT DISTINCT o FROM Orders o " +
          "JOIN FETCH o.payment p " +
          "JOIN FETCH o.orderItem oi " +
          "JOIN FETCH oi.cart c " +
          "JOIN FETCH c.cartItems ci " +
          "JOIN FETCH ci.productVariants pv " +
          "JOIN FETCH pv.product prod " +
          "LEFT JOIN FETCH o.voucher v " +
          "WHERE o.user.userId = :userId " +
          "AND o.status = 'CANCELLED' " +
          "AND p.status = 'REFUND'")
  List<Orders> findAllCancelledOrdersWithRefundAndFullDetails(@Param("userId") int userId);


  @Query("SELECT o FROM Orders o " +
          "JOIN o.payment p " +
          "WHERE o.user.userId = :userId " +
          "AND o.status = :status " +
          "AND p.status = :paymentStatus")
  List<Orders> findAllByUserUserIdAndStatusAndPaymentStatus(@Param("userId") int userId,
                                                            @Param("status") Status_Order status,
                                                            @Param("paymentStatus") Status_Payment paymentStatus);

  @Query("SELECT o FROM Orders o WHERE o.status = :statusOrder AND o.payment.status = :statusPayment")
  List<Orders> findAllByStatusAndPaymentStatus(@Param("statusOrder") Status_Order statusOrder,
                                               @Param("statusPayment") Status_Payment statusPayment);

  @Query("SELECT o FROM Orders o WHERE o.isDeleted = false AND o.cancelReason IS NOT NULL AND o.isCancelReason IS NULL")
  List<Orders> findAllCancelReasonAwait();

  Orders findByOrderIdAndUserUserIdAndIsDeletedFalse(int orderId, int id);

  Orders findByOrderIdAndIsDeletedFalse(int id);




  List<Orders> findAllByUserUserId(int userId, Sort sort);
  Page<Orders> findAllByUserUserId(int userId,Pageable pageable);
  @EntityGraph(attributePaths = {
          "payment",
          "payment.shipment",
          "orderItem.cart.cartItems.productVariants.product",
          "user",
          "voucher"
  })
  List<Orders> findAllByUserUserId(int userId);
  List<Orders> findAllByStatus(Status_Order status);
  List<Orders> findAllByIsDeletedFalse();

  List<Orders> findAllByUserUserIdAndStatus(int userId,Status_Order statusOrder);

  Orders findByOrderIdAndStatus(int orderId, Status_Order status);
  Orders findByOrderIdAndStatusAndIsDeletedFalse(int orderId, Status_Order status);


  Page<Orders> findAllByUserUserIdAndIsDeletedFalse(int userId,Pageable pageable);

  Page<Orders> findAllByUserUserIdAndStatusAndIsDeletedFalse(int userId,Status_Order statusOrder,Pageable pageable);

//  @Query(value = "SELECT DISTINCT oi.product_id FROM orders o JOIN order_items oi ON o.order_id = oi.order_id WHERE o.user_id = :userId", nativeQuery = true)
//  List<Integer> findAllProductIdsInUserOrders(@Param("userId") int userId);

}
