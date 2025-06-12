package com.hmdrinks.Repository;
import com.hmdrinks.Entity.ShipperAttendance;
import com.hmdrinks.Entity.ShipperCommissionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;


public interface ShipperCommissionDetailRepository extends JpaRepository<ShipperCommissionDetail, Long> {
    Optional<ShipperCommissionDetail> findByUserUserIdAndCommissionDate(Integer userId, LocalDate commissionDate);

}
