package com.hmdrinks.Repository;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
import com.hmdrinks.Service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface PaymentRepository extends JpaRepository<Payment, Long> {
     Payment findByPaymentId(int paymentId);
     Payment findByPaymentIdAndIsDeletedFalse(int paymentId);
     Payment findByOrderOrderId(int orderId);
     Payment findByOrderOrderIdAndIsDeletedFalse(int orderId);
     long countByOrder_OrderDateBetweenAndStatus(LocalDateTime from,
                                                 LocalDateTime to,
                                                 Status_Payment status);


     Page<Payment> findAll(Pageable pageable);
     List<Payment> findAllByIsDeletedFalse();
     Page<Payment> findAllByIsDeletedFalse(Pageable pageable);

     @Query("SELECT p.paymentId as paymentId, p.amount as amount, p.dateCreated as dateCreated, " +
             "p.dateDeleted as dateDeleted, p.dateRefunded as dateRefunded, p.isDeleted as isDeleted, " +
             "p.paymentMethod as paymentMethod, p.status as status, p.order.orderId as orderId, " +
             "p.isRefund as isRefund, p.Link as link " +
             "FROM Payment p WHERE p.status = :status AND p.isDeleted = false")
     Page<PaymentService.PaymentSummary> findAllByStatusAndIsDeletedFalse(@Param("status") Status_Payment status, Pageable pageable);


     @Query("SELECT p.paymentId as paymentId, p.amount as amount, p.dateCreated as dateCreated, " +
             "p.dateDeleted as dateDeleted, p.dateRefunded as dateRefunded, p.isDeleted as isDeleted, " +
             "p.paymentMethod as paymentMethod, p.status as status, p.order.orderId as orderId, " +
             "p.isRefund as isRefund, p.Link as link " +
             "FROM Payment p " +
             "WHERE p.status = :status AND p.isDeleted = false AND p.isRefund = :isRefund")
     Page<PaymentService.PaymentSummary> findAllByStatusAndIsDeletedFalseAndIsRefund(
             @Param("status") Status_Payment status,
             @Param("isRefund") Boolean isRefund,
             Pageable pageable
     );

     //     Page<Payment> findAllByStatusAndIsDeletedFalse(Status_Payment status, Pageable pageable);
     List<Payment> findAllByStatusAndIsDeletedFalse(Status_Payment status);
     Page<Payment> findAllByPaymentMethod(Payment_Method paymentMethod, Pageable pageable);
     Page<Payment> findAllByPaymentMethodAndIsDeletedFalse(Payment_Method paymentMethod, Pageable pageable);
     List<Payment> findAllByPaymentMethodAndIsDeletedFalse(Payment_Method paymentMethod);

     Payment findByOrderIdPayment(String orderIdPayment);

     @Query("SELECT DATE(p.dateCreated), SUM(p.amount) FROM Payment p WHERE p.dateCreated >= :startDate AND p.dateCreated <= :endDate AND p.status = 'COMPLETED' GROUP BY DATE(p.dateCreated)")
     List<Object[]> findDailyRevenueByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

     @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.order.orderDate BETWEEN :start AND :end AND p.status = :status")
     Double sumAmountByOrder_OrderDateBetweenAndStatus(LocalDateTime start, LocalDateTime end, Status_Payment status);

     @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.dateCreated >= :startDate AND p.dateCreated <= :endDate AND p.status = 'COMPLETED'")
     Double findTotalRevenueByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

}
