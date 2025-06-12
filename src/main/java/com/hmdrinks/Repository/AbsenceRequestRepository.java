package com.hmdrinks.Repository;
import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scala.Int;

import java.time.LocalDateTime;
import java.util.List;


public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
      AbsenceRequest findByRequestId(Integer absenceRequestId);
      List<AbsenceRequest> findByUserUserIdAndStatus(Integer userId, LeaveStatus status);
      Page<AbsenceRequest> findByUserUserId(Integer userId, Pageable pageable);
      Page<AbsenceRequest> findByUserUserIdAndStatus(Integer userId,LeaveStatus status, Pageable pageable);
      List<AbsenceRequest> findByUser_UserIdAndStatusIn(Integer userId, List<LeaveStatus> statuses);

      List<AbsenceRequest> findByUserUserId(Integer userId );
      Page<AbsenceRequest> findAll(Pageable pageable);
      Page<AbsenceRequest> findByStatus(LeaveStatus status, Pageable pageable);
      List<AbsenceRequest> findByStatus(LeaveStatus status);
}
