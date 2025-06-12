package com.hmdrinks.Service;

import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Entity.ShipperSalarySummary;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Repository.AbsenceRequestRepository;
import com.hmdrinks.Repository.ShipperSalarySummaryRequestRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Request.CreateNewAbsence;
import com.hmdrinks.Request.CreateNewShipperSalary;
import com.hmdrinks.Request.UpdateAbsenceReq;
import com.hmdrinks.Response.CRUDAbsenceRequestResponse;
import com.hmdrinks.Response.ListAllAbsenceRequestByUserIdResponse;
import com.hmdrinks.Response.ListAllAbsenceRequestResponse;
import com.hmdrinks.SupportFunction.SupportFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import scala.Int;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShipperSalarySummaryService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private ShipperSalarySummaryRequestRepository shipperSalarySummaryRequestRepository;
    @Autowired
    private SupportFunction supportFunction;


    public ResponseEntity<?> createShipperSalary(CreateNewShipperSalary req) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        if(Integer.parseInt(String.valueOf(req.getBaseSalary())) <= 0)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Value ");
        }
        ShipperSalarySummary shipperSalarySummary = new ShipperSalarySummary();


        return ResponseEntity.ok().build();

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
