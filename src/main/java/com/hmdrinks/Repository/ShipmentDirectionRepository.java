package com.hmdrinks.Repository;
import com.hmdrinks.Entity.ShipmentDirection;
import com.hmdrinks.Entity.Shippment;
import com.hmdrinks.Entity.ShippmentGroup;
import com.hmdrinks.Enum.Status_Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ShipmentDirectionRepository extends JpaRepository<ShipmentDirection, Integer> {
    ShipmentDirection findByShipmentShipmentId(int shipmentId);
    boolean existsByShipmentGroup(ShippmentGroup shipmentGroup);

}
