package com.hmdrinks.Repository;
import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Entity.ShipperAttendance;
import com.hmdrinks.Enum.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface ShipperAttendanceRepository extends JpaRepository<ShipperAttendance, Long> {
      boolean existsByUserUserIdAndAttendanceDate(Integer userId, LocalDate date);
      ShipperAttendance findById(Integer id);
      List<ShipperAttendance> findByUserUserIdAndAttendanceDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);

      ShipperAttendance findByUserUserIdAndAttendanceDate(Integer userId, LocalDate dateToCheck);
}
