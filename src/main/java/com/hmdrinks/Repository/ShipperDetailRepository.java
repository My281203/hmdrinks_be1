package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Notification;
import com.hmdrinks.Entity.ShipperDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShipperDetailRepository extends JpaRepository<ShipperDetail, Long> {
   ShipperDetail findById(Integer id);
   ShipperDetail findByUserUserId(Integer userId);
}
