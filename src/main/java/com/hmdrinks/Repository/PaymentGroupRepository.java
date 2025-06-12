package com.hmdrinks.Repository;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Entity.PaymentGroup;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface PaymentGroupRepository extends JpaRepository<PaymentGroup, Long> {
    List<PaymentGroup> findByGroupOrder_GroupOrderIdAndIsDeletedFalse(int groupOrderId);
    PaymentGroup findByGroupOrder_GroupOrderIdAndStatusAndIsDeletedFalse(int groupOrderId,Status_Payment statusPayment);
    List<PaymentGroup> findByGroupOrder_GroupOrderIdAndIsDeletedFalseAndStatus(int groupOrderId,Status_Payment statusPayment);
    PaymentGroup findByOrderIdPayment(String orderIdPayment);


    PaymentGroup findByPaymentId(int paymentId);

    PaymentGroup findByPaymentIdAndIsDeletedFalse(int paymentId);

    List<PaymentGroup> findAllByPaymentMethodAndIsDeletedFalse(Payment_Method paymentMethod);

    Page<PaymentGroup> findAllByStatusAndIsDeletedFalse(Status_Payment statusPayment, Pageable pageable);
    List<PaymentGroup> findAllByStatusAndIsDeletedFalse(Status_Payment statusPayment);
}
