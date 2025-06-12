package com.hmdrinks.Repository;
import com.hmdrinks.Entity.GroupOrderMember;
import com.hmdrinks.Entity.GroupOrders;
import com.hmdrinks.Entity.Orders;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.Status_Order;
import com.hmdrinks.Enum.Status_Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupOrdersRepository extends JpaRepository<GroupOrders, Long> {
  GroupOrders findByGroupOrderIdAndIsDeletedFalse(Integer groupId);
  @Query("SELECT MAX(CAST(SUBSTRING(go.code, 4) AS int)) FROM GroupOrders go WHERE go.code LIKE 'GR_%'")
  Integer findMaxGroupOrderCodeNumber();

  GroupOrders findByCode(String code);
  @Query("""
           SELECT gom
           FROM GroupOrders gom
           WHERE gom.user.userId = :userId
           AND gom.status NOT IN (:completed, :canceled)
           AND (gom.isDeleted = false OR gom.isDeleted IS NULL)
           """)
  List<GroupOrders> findActiveGroupOrdersByUserId(
          @Param("userId") int userId,
          @Param("completed") StatusGroupOrder completed,
          @Param("canceled") StatusGroupOrder canceled
  );


  Page<GroupOrders> findAllByIsDeletedFalseAndStatusAndUserUserId(StatusGroupOrder statusGroupOrder,Integer userId,Pageable pageable);

  List<GroupOrders>  findAllByIsDeletedFalse();
  List<GroupOrders> findAllByStatusAndPaymentStatus(StatusGroupOrder statusOrder, Status_Payment statusPayment);
  List<GroupOrders> findAllByUserUserId(Integer userId);


  Page<GroupOrders> findAllByUserUserId(Integer userId,Pageable pageable);



  @Query("""
    SELECT DISTINCT g
    FROM GroupOrders g
    JOIN g.payment p
    WHERE g.user.userId = :userId
      AND p.status = com.hmdrinks.Enum.Status_Payment.REFUND
      AND g.isDeleted = false
""")
  Page<GroupOrders> findGroupOrdersWithRefundPaymentByUserId(
          @Param("userId") Integer userId,
          Pageable pageable
  );

  @Query("""
    SELECT DISTINCT g
    FROM GroupOrders g
    JOIN g.payment p
    WHERE p.status = 'REFUND'
      AND g.isDeleted = false
""")
  Page<GroupOrders> findGroupOrdersWithRefundPayment(
          Pageable pageable
  );

}