package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewAbsence;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.UpdateAbsenceReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class AbsenceRequestService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private SupportFunction supportFunction;


    public ResponseEntity<?> createRequestAbsence(CreateNewAbsence req) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = req.getStartDate().toLocalDate();
        LocalDate endDate = req.getEndDate().toLocalDate();

        if (startDate.isBefore(today)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày bắt đầu không được nhỏ hơn ngày hiện tại.");
        }

        if (endDate.isBefore(startDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }


        if (!startDate.isAfter(today)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Bạn phải gửi đơn xin nghỉ trước ít nhất 1 ngày.");
        }

        List<AbsenceRequest> existingRequests = absenceRequestRepository
                .findByUser_UserIdAndStatusIn(user.getUserId(), List.of(LeaveStatus.WAITING, LeaveStatus.APPROVED));

        boolean isOverlapping = existingRequests.stream().anyMatch(existing ->
                !existing.getEndDate().toLocalDate().isBefore(startDate) &&
                        !existing.getStartDate().toLocalDate().isAfter(endDate)
        );

        if (isOverlapping) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Đã có đơn nghỉ phép trùng ngày với đơn hiện tại.");
        }
        YearMonth requestMonth = YearMonth.from(startDate);

        long approvedDaysInMonth = existingRequests.stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED)
                .flatMap(r -> {
                    LocalDate start = r.getStartDate().toLocalDate();
                    LocalDate end = r.getEndDate().toLocalDate();
                    return start.datesUntil(end.plusDays(1));
                })
                .filter(date -> YearMonth.from(date).equals(requestMonth))
                .distinct()
                .count();

        long newRequestDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> YearMonth.from(date).equals(requestMonth))
                .count();

        if ((approvedDaysInMonth + newRequestDays) > 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Bạn đã sử dụng quá 5 ngày phép trong tháng này.");
        }

        AbsenceRequest absenceRequest = new AbsenceRequest();
        absenceRequest.setStatus(LeaveStatus.WAITING);
        absenceRequest.setUser(user);
        absenceRequest.setReason(req.getReason());
        absenceRequest.setStartDate(req.getStartDate());
        absenceRequest.setEndDate(req.getEndDate());
        absenceRequestRepository.save(absenceRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CRUDAbsenceRequestResponse(
                user.getUserId(),
                absenceRequest.getRequestId(),
                absenceRequest.getReason(),
                absenceRequest.getStatus(),
                startDate,
                endDate
        ));
    }


    public ResponseEntity<?> getOneAbsence(Integer userId, Integer requestId)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        AbsenceRequest absenceRequest = absenceRequestRepository.findByRequestId(requestId);
        if(absenceRequest == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found request");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new CRUDAbsenceRequestResponse(
                absenceRequest.getUser().getUserId(),
                absenceRequest.getRequestId(),
                absenceRequest.getReason(),
                absenceRequest.getStatus(),
                absenceRequest.getStartDate().toLocalDate(),
                absenceRequest.getEndDate().toLocalDate()
        ));
    }

    public ResponseEntity<?> getListAbsenceByUserId(Integer userId,String pageFromParam, String limitFromParam)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        Page<AbsenceRequest> absenceRequest = absenceRequestRepository.findByUserUserId(userId,pageable);
        List<AbsenceRequest> list = absenceRequestRepository.findByUserUserId(userId);
        List<CRUDAbsenceRequestResponse> responses = new ArrayList<>();
        for(AbsenceRequest absenceRequest1 : absenceRequest)
        {
            CRUDAbsenceRequestResponse response = new CRUDAbsenceRequestResponse(
                    absenceRequest1.getUser().getUserId(),
                    absenceRequest1.getRequestId(),
                    absenceRequest1.getReason(),
                    absenceRequest1.getStatus(),
                    absenceRequest1.getStartDate().toLocalDate(),
                    absenceRequest1.getEndDate().toLocalDate());

            responses.add(response);

        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new ListAllAbsenceRequestByUserIdResponse(
                userId,
                list.size(),
                responses
        ));
    }

    public ResponseEntity<?> getListAbsenceByUserIdAndStatus(Integer userId,String pageFromParam, String limitFromParam,LeaveStatus status)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        Page<AbsenceRequest> absenceRequest = absenceRequestRepository.findByUserUserIdAndStatus(userId,status,pageable);
        List<AbsenceRequest> list = absenceRequestRepository.findByUserUserIdAndStatus(userId,status);
        List<CRUDAbsenceRequestResponse> responses = new ArrayList<>();
        for(AbsenceRequest absenceRequest1 : absenceRequest)
        {
            CRUDAbsenceRequestResponse response = new CRUDAbsenceRequestResponse(
                    absenceRequest1.getUser().getUserId(),
                    absenceRequest1.getRequestId(),
                    absenceRequest1.getReason(),
                    absenceRequest1.getStatus(),
                    absenceRequest1.getStartDate().toLocalDate(),
                    absenceRequest1.getEndDate().toLocalDate());

            responses.add(response);

        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new ListAllAbsenceRequestByUserIdResponse(
                userId,
                list.size(),
                responses
        ));
    }

    public ResponseEntity<?> getListAbsenceByStatus(LeaveStatus status,String pageFromParam, String limitFromParam)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);


        Page<AbsenceRequest> absenceRequest = absenceRequestRepository.findByStatus(status,pageable);
        List<AbsenceRequest> list = absenceRequestRepository.findByStatus(status);
        List<CRUDAbsenceRequestResponse> responses = new ArrayList<>();
        for(AbsenceRequest absenceRequest1 : absenceRequest)
        {
            CRUDAbsenceRequestResponse response = new CRUDAbsenceRequestResponse(
                    absenceRequest1.getUser().getUserId(),
                    absenceRequest1.getRequestId(),
                    absenceRequest1.getReason(),
                    absenceRequest1.getStatus(),
                    absenceRequest1.getStartDate().toLocalDate(),
                    absenceRequest1.getEndDate().toLocalDate());

            responses.add(response);

        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new ListAllAbsenceRequestResponse(
                list.size(),
                responses
        ));
    }


    public ResponseEntity<?> checkTimeAbsence() {
        List<AbsenceRequest> absenceRequestList = absenceRequestRepository.findAll();
        LocalDate today = LocalDate.now();
        int count = 0;

        for (AbsenceRequest absenceRequest : absenceRequestList) {
            if (absenceRequest.getStatus() == LeaveStatus.WAITING) {
                LocalDate startDate = absenceRequest.getStartDate().toLocalDate();
                if (today.isAfter(startDate)) {
                    absenceRequest.setStatus(LeaveStatus.REJECTED);
                    absenceRequestRepository.save(absenceRequest);
                    count++;
                }
            }
        }

        return ResponseEntity.ok("Đã tự động từ chối " + count + " đơn nghỉ phép quá hạn.");
    }


    public ResponseEntity<?> updateStatusAbsence(UpdateAbsenceReq req)
    {
        AbsenceRequest absenceRequest = absenceRequestRepository.findByRequestId(req.getRequestId());
        if (absenceRequest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found request");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = absenceRequest.getStartDate().toLocalDate();

        if (!today.isBefore(startDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Chỉ được phép duyệt đơn trước ngày bắt đầu nghỉ.");
        }

        absenceRequest.setStatus(req.getStatus());
        absenceRequestRepository.save(absenceRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CRUDAbsenceRequestResponse(
                absenceRequest.getUser().getUserId(),
                absenceRequest.getRequestId(),
                absenceRequest.getReason(),
                absenceRequest.getStatus(),
                absenceRequest.getStartDate().toLocalDate(),
                absenceRequest.getEndDate().toLocalDate()
        ));
    }


    public ResponseEntity<?> getListAbsence(String pageFromParam, String limitFromParam)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);


        Page<AbsenceRequest> absenceRequest = absenceRequestRepository.findAll(pageable);
        List<AbsenceRequest> list = absenceRequestRepository.findAll();
        List<CRUDAbsenceRequestResponse> responses = new ArrayList<>();
        for(AbsenceRequest absenceRequest1 : absenceRequest)
        {
            CRUDAbsenceRequestResponse response = new CRUDAbsenceRequestResponse(
                    absenceRequest1.getUser().getUserId(),
                    absenceRequest1.getRequestId(),
                    absenceRequest1.getReason(),
                    absenceRequest1.getStatus(),
                    absenceRequest1.getStartDate().toLocalDate(),
                    absenceRequest1.getEndDate().toLocalDate());

            responses.add(response);

        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new ListAllAbsenceRequestResponse(
                list.size(),
                responses
        ));
    }

}
