package com.hmdrinks.Repository;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Entity.Shippment;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
import com.hmdrinks.Enum.Status_Shipment;
import com.hmdrinks.Response.CRUDShipmentResponse;
import com.hmdrinks.Service.ShipmentService;
import com.hmdrinks.Service.ShipperComissionDetailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;


public interface ShipmentRepository extends JpaRepository<Shippment, Integer> {
   Shippment findByShipmentIdAndIsDeletedFalse(int shipmentId);
   List<Shippment> findByStatus(Status_Shipment status);
   Shippment findByPaymentPaymentIdAndIsDeletedFalse(int paymentId);
   @Query("SELECT s FROM Shippment s JOIN s.user u WHERE u.fullName LIKE %:keyword%")
   Page<Shippment> findByUserUserNameContaining(@Param("keyword") String keyword, Pageable pageable);
   @Query("SELECT s FROM Shippment s JOIN s.user u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND s.isDeleted = false")
   Page<Shippment> searchByShipperName(@Param("keyword") String keyword, Pageable pageable);
   @Query("""
    SELECT s FROM Shippment s
    JOIN FETCH s.payment p
    JOIN FETCH p.order o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE s.status = :status
""")
   List<Shippment> findAllByStatusFetchAll(Status_Shipment status, Pageable pageable);
   @Query("""
    SELECT s FROM Shippment s
    JOIN FETCH s.payment p
    JOIN FETCH p.order o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE shipper.userId = :userId AND s.status = :status
""")
   List<Shippment> findAllByUserIdAndStatusFetchAll(@Param("userId") int userId, @Param("status") Status_Shipment status, Pageable pageable);


   @Query("SELECT s.shipmentId FROM Shippment s WHERE s.status = :status")
   Page<Integer> findShipmentIdsByStatus(@Param("status") Status_Shipment status, Pageable pageable);

   @Query("""
    SELECT new com.hmdrinks.Response.CRUDShipmentResponse(
        s.shipmentId, shipper.fullName, o.orderDate, s.dateDeleted, s.dateDelivered,
        s.dateShip, s.dateCancel, s.isDeleted, s.status, s.note,
        p.paymentId, shipper.userId, customer.fullName, customer.userId,
        o.address, customer.phoneNumber, customer.email, o.orderId
    )
    FROM Shippment s
    JOIN s.payment p
    JOIN p.order o
    JOIN o.user customer
    LEFT JOIN s.user shipper
    WHERE s.shipmentId IN :shipmentIds
""")
   List<CRUDShipmentResponse> findShipmentsByIds(@Param("shipmentIds") List<Integer> shipmentIds);


   @Query("""
    SELECT COUNT(s) FROM Shippment s
    WHERE s.user.userId = :userId AND s.status = :status
""")
   int countByUserIdAndStatus(@Param("userId") int userId, @Param("status") Status_Shipment status);

   @Query("""
    SELECT s FROM Shippment s
    JOIN FETCH s.payment p
    JOIN FETCH p.order o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE shipper.userId = :userId
""")
   List<Shippment> findAllByUserIdFetchAll(@Param("userId") int userId, Pageable pageable);

   @Query("SELECT s.shipmentId as shipmentId, shipper.fullName as shipperName, o.orderDate as orderDate, " +
           "s.dateDeleted as dateDeleted, s.dateDelivered as dateDelivered, s.dateShip as dateShip, s.dateCancel as dateCancel, " +
           "s.isDeleted as isDeleted, s.status as status, s.note as note, " +
           "p.paymentId as paymentId, shipper.userId as shipperId, customer.fullName as customerFullName, customer.userId as customerId, " +
           "CONCAT(customer.street, ', ', customer.ward, ', ', customer.district, ', ', customer.city) as customerAddress, " +
           "customer.phoneNumber as customerPhone, customer.email as customerEmail, o.orderId as orderId, s.dateCreated as shipmentDateCreated " +
           "FROM Shippment s " +
           "JOIN s.user shipper " +
           "JOIN s.payment p " +
           "JOIN p.order o " +
           "JOIN o.user customer " +
           "WHERE s.isDeleted = false AND shipper.userId = :userId" )
   Page<ShipmentService.ShipmentListProjection> findShipmentDetailsByUserId(@Param("userId") int userId, Pageable pageable);




   @Query("""
    SELECT COUNT(s) FROM Shippment s
    WHERE s.user.userId = :userId
""")
   int countByShipper(@Param("userId") int userId);



   @Query(value = "SELECT s FROM Shippment s " +
           "JOIN FETCH s.user shipper " +
           "JOIN FETCH s.payment p " +
           "JOIN FETCH p.order o " +
           "JOIN FETCH o.user customer " +
           "WHERE s.isDeleted = false",
           countQuery = "SELECT COUNT(s) FROM Shippment s WHERE s.isDeleted = false")
   Page<Shippment> findAllWithDetails(Pageable pageable);

   @Query(value = "SELECT COUNT(*) FROM shipment WHERE is_deleted = false", nativeQuery = true)
   long countAllActiveShipments();

   @Query("SELECT s.shipmentId as shipmentId, shipper.fullName as shipperName, o.orderDate as orderDate, " +
           "s.dateDeleted as dateDeleted, s.dateDelivered as dateDelivered, s.dateShip as dateShip, s.dateCancel as dateCancel, " +
           "s.isDeleted as isDeleted, s.status as status, s.note as note, " +
           "p.paymentId as paymentId, shipper.userId as shipperId, customer.fullName as customerFullName, customer.userId as customerId, " +
           "CONCAT(customer.street, ', ', customer.ward, ', ', customer.district, ', ', customer.city) as customerAddress, " +
           "customer.phoneNumber as customerPhone, customer.email as customerEmail, o.orderId as orderId, s.dateCreated as shipmentDateCreated " +
           "FROM Shippment s " +
           "JOIN s.user shipper " +
           "JOIN s.payment p " +
           "JOIN p.order o " +
           "JOIN o.user customer " +
           "WHERE s.isDeleted = false")
   Page<ShipmentService.ShipmentListProjection> findAllShipmentDetails(Pageable pageable);




//   @Query("SELECT new com.hmdrinks.Service.ShipmentService$ShipmentListDTO(" +
//           "s.shipmentId, shipper.fullName, o.orderDate, " +
//           "s.dateDeleted, s.dateDelivered, s.dateShip, s.dateCancel, " +
//           "s.isDeleted, s.status, s.note, " +
//           "p.paymentId, shipper.userId, customer.fullName, customer.userId, " +
//           "CONCAT(customer.street, ', ', customer.ward, ', ', customer.district, ', ', customer.city), " +
//           "customer.phoneNumber, customer.email, o.orderId) " +
//           "FROM Shippment s " +
//           "JOIN s.user shipper " +
//           "JOIN s.payment p " +
//           "JOIN p.order o " +
//           "JOIN o.user customer " +
//           "WHERE s.isDeleted = false")
//   Page<ShipmentService.ShipmentListDTO> findAllShipmentDetails(Pageable pageable);



   Shippment findByPaymentPaymentId(int paymentId);
   Shippment findByUserUserIdAndShipmentId(int userId, int shipmentId);
   Page<Shippment> findAll(Pageable pageable);
   List<Shippment> findAll();
   Page<Shippment> findAllByStatus(Status_Shipment statusShipment, Pageable pageable);
   List<Shippment> findAllByStatus(Status_Shipment statusShipment);
   Page<Shippment> findAllByUserUserIdAndStatus(int userId, Status_Shipment statusShipment,Pageable pageable);
   Page<Shippment> findAllByUserUserId(int userId, Pageable pageable);
   List<Shippment> findAllByUserUserId(int shipmentId);
   List<Shippment> findAllByUserUserIdAndStatus(int userId, Status_Shipment statusShipment);
   List<Shippment> findByUserUserIdAndDateShipBetween(Integer userId, LocalDateTime start, LocalDateTime end);

   List<Shippment> findAllByPayment_PaymentIdInAndIsDeletedFalse(List<Integer> paymentIds);

   List<Shippment>  findByUserUserIdAndDateShipLessThanEqual(int userId, LocalDateTime dateShip);

   @Query("""
    SELECT s FROM Shippment s
    JOIN FETCH s.payment p
    JOIN FETCH p.order o
    JOIN FETCH o.user u
    LEFT JOIN FETCH s.user shipper
    WHERE o.user.userId = :userId
    AND s.status = 'WAITING'
    AND o.isDeleted = false
""")
   List<Shippment> findWaitingShipmentsByUserId(@Param("userId") int userId);

   @Query(value = """
    SELECT 
        DATE(s.date_shipped) AS commissionDate,
        COUNT(*) AS orderCount,
        SUM(s.distance * 1500) AS dailyCommission
    FROM shipment s
    JOIN users u ON s.user_id = u.user_id
    WHERE s.user_id = :userId 
      AND s.date_shipped IS NOT NULL
      AND u.is_deleted = false
    GROUP BY DATE(s.date_shipped)
""", nativeQuery = true)
   List<ShipperComissionDetailService.ShipperCommissionStat> getCommissionStats(@Param("userId") int userId);


}
