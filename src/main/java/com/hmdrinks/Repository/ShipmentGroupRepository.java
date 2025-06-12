package com.hmdrinks.Repository;
import com.hmdrinks.Entity.Shippment;
import com.hmdrinks.Entity.ShippmentGroup;
import com.hmdrinks.Enum.Status_Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


public interface ShipmentGroupRepository extends JpaRepository<ShippmentGroup, Integer> {


    ShippmentGroup findByShipmentIdAndIsDeletedFalse(int shipmentId);
    ShippmentGroup findByUserUserIdAndShipmentId(int userId, int shipmentId);


    @Query("SELECT s FROM Shippment s JOIN s.user u WHERE u.fullName LIKE %:keyword%")
    Page<ShippmentGroup> findByUserUserNameContaining(@Param("keyword") String keyword, Pageable pageable);
    @Query("SELECT s FROM Shippment s JOIN s.user u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND s.isDeleted = false")
    Page<ShippmentGroup> searchByShipperName(@Param("keyword") String keyword, Pageable pageable);
    @Query("""
    SELECT s FROM ShippmentGroup s
    JOIN FETCH s.payment p
    JOIN FETCH p.groupOrder o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE s.status = :status
""")
    List<ShippmentGroup> findAllByStatusFetchAll(Status_Shipment status, Pageable pageable);
    @Query("""
    SELECT s FROM ShippmentGroup s
    JOIN FETCH s.payment p
    JOIN FETCH p.groupOrder o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE shipper.userId = :userId AND s.status = :status
""")
    List<ShippmentGroup> findAllByUserIdAndStatusFetchAll(@Param("userId") int userId, @Param("status") Status_Shipment status, Pageable pageable);

    @Query("""
    SELECT COUNT(s) FROM ShippmentGroup s
    WHERE s.user.userId = :userId AND s.status = :status
""")
    int countByUserIdAndStatus(@Param("userId") int userId, @Param("status") Status_Shipment status);

    @Query("""
    SELECT s FROM ShippmentGroup s
    JOIN FETCH s.payment p
    JOIN FETCH p.groupOrder o
    JOIN FETCH o.user customer
    LEFT JOIN FETCH s.user shipper
    WHERE shipper.userId = :userId
""")
    List<ShippmentGroup> findAllByUserIdFetchAll(@Param("userId") int userId, Pageable pageable);

    @Query("""
    SELECT COUNT(s) FROM ShippmentGroup s
    WHERE s.user.userId = :userId
""")
    int countByShipper(@Param("userId") int userId);


    @Query("SELECT s FROM ShippmentGroup s " +
            "JOIN FETCH s.user shipper " +
            "JOIN FETCH s.payment p " +
            "JOIN FETCH p.groupOrder o " +
            "JOIN FETCH o.user customer " +
            "WHERE s.isDeleted = false")
    Page<ShippmentGroup> findAllWithDetails(Pageable pageable);



    Page<ShippmentGroup> findAll(Pageable pageable);
    List<ShippmentGroup> findAll();
    Page<ShippmentGroup> findAllByStatus(Status_Shipment statusShipment, Pageable pageable);
    List<ShippmentGroup> findAllByStatus(Status_Shipment statusShipment);
    Page<ShippmentGroup> findAllByUserUserIdAndStatus(int userId, Status_Shipment statusShipment,Pageable pageable);
    Page<ShippmentGroup> findAllByUserUserId(int userId, Pageable pageable);
    List<ShippmentGroup> findAllByUserUserId(int shipmentId);
    List<ShippmentGroup> findAllByUserUserIdAndStatus(int userId, Status_Shipment statusShipment);
    List<ShippmentGroup> findByUserUserIdAndDateShipBetween(Integer userId, LocalDateTime start, LocalDateTime end);

//    List<ShippmentGroup> findAllByPaymentGroup_PaymentIdInAndIsDeletedFalse(List<Integer> paymentIds);

    List<ShippmentGroup>  findByUserUserIdAndDateShipLessThanEqual(int userId, LocalDateTime dateShip);

    @Query("""
    SELECT s FROM ShippmentGroup s
    JOIN FETCH s.payment p
    JOIN FETCH p.groupOrder o
    JOIN FETCH o.user u
    LEFT JOIN FETCH s.user shipper
    WHERE o.user.userId = :userId
    AND s.status = 'WAITING'
    AND o.isDeleted = false
""")
    List<ShippmentGroup> findWaitingShipmentsByUserId(@Param("userId") int userId);

    List<ShippmentGroup> findByStatus(Status_Shipment statusShipment);
}
