package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.AttendanceStatus;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Status_Shipment;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewAbsence;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShipperAttendanceService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private ShipperAttendanceRepository shipperAttendanceRepository;
    @Autowired
    private ShipperDetailRepository shipperDetailRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private  ShipmentGroupRepository shipmentGroupRepository;
    @Autowired
    private SupportFunction supportFunction;


    public  ResponseEntity<?> getCurrentStatusShipper(Integer shipperId)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(shipperId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }
        ShipperDetail shipperDetail = shipperDetailRepository.findByUserUserId(shipperId);
        if (shipperDetail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found shipper");
        }
        String status = shipperDetail.getStatus();
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }

    public  ResponseEntity<?> activeStatus(Integer shipperId)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(shipperId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        List<Shippment> list = shipmentRepository.findAllByUserUserId(shipperId);
        for(Shippment shippment : list)
        {
            if(shippment.getStatus() != Status_Shipment.CANCELLED && shippment.getStatus() != Status_Shipment.SUCCESS)
            {
                if (shippment.getDateCreated().toLocalDate().isEqual(LocalDate.now())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not active available");
                }
            }
        }

        List<ShippmentGroup> list1 = shipmentGroupRepository.findAllByUserUserId(shipperId);
        for(ShippmentGroup shippment : list1)
        {
            if(shippment.getStatus() != Status_Shipment.CANCELLED && shippment.getStatus() != Status_Shipment.SUCCESS)
            {
                if (shippment.getDateCreated().toLocalDate().isEqual(LocalDate.now())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not active available");
                }
            }
        }

        ShipperDetail shipperDetail = shipperDetailRepository.findByUserUserId(shipperId);
        if(shipperDetail == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        shipperDetail.setStatus("available");
        shipperDetail.setLocation("01 Đ. Võ Văn Ngân, Linh Chiểu, Thủ Đức, Hồ Chí Minh");
        shipperDetailRepository.save(shipperDetail);
        return ResponseEntity.status(HttpStatus.OK).body("Reset success");

    }


    public ResponseEntity<?> checkIn(Integer userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }
        ShipperAttendance shipperAttendance = new ShipperAttendance();
        LocalDateTime now = LocalDateTime.now();
        LocalDate localDate = now.toLocalDate();
        LocalDateTime lateThreshold = LocalDateTime.of(localDate, LocalTime.of(8, 30));
        boolean alreadyCheckedIn = shipperAttendanceRepository.existsByUserUserIdAndAttendanceDate(userId, localDate);
        if (alreadyCheckedIn) {
            return ResponseEntity.badRequest().body("Đã điểm danh hôm nay");
        }
        ShipperAttendance attendance = new ShipperAttendance();
        attendance.setUser(user);
        attendance.setAttendanceDate(localDate);
        attendance.setCheckInTime(now);
        attendance.setCreatedAt(now);
        attendance.setIsPresent(Boolean.TRUE);

        if (now.isAfter(lateThreshold)) {
            attendance.setStatus(AttendanceStatus.LATE);
            attendance.setNote("Đi trễ sau 8h30");
        } else {
            attendance.setStatus(AttendanceStatus.ON_TIME);
            attendance.setNote("Đi đúng giờ");
        }

        shipperAttendanceRepository.save(attendance);
        CRUDShipperAttendance dto = mapToCRUD(attendance);
        return ResponseEntity.ok(dto);

    }

    public ResponseEntity<?> updateNote(String note, Integer attendanceId, Integer userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        if(Objects.equals(note, ""))
        {
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not empty note");
        }

        ShipperAttendance shipperAttendance = shipperAttendanceRepository.findById(attendanceId);
        shipperAttendance.setNote(note);
        shipperAttendance.setUpdatedAt(LocalDateTime.now());
        shipperAttendanceRepository.save(shipperAttendance);
        CRUDShipperAttendance dto = mapToCRUD(shipperAttendance);
        return ResponseEntity.ok(dto);
    }


    @Scheduled(fixedRate = 60 * 60 * 1000) // chạy lúc 01:00 mỗi ngày
    @Transactional
    public void checkDailyAttendanceStatus() {
        LocalDate dateToCheck = LocalDate.now().minusDays(1); // kiểm tra ngày hôm qua

        List<User> shippers = userRepository.findAllByRole(Role.SHIPPER);

        for (User shipper : shippers) {
            // Kiểm tra xem đã có attendance cho ngày hôm qua chưa
            ShipperAttendance existingAttendance = shipperAttendanceRepository
                    .findByUserUserIdAndAttendanceDate(shipper.getUserId(), dateToCheck);

            if (existingAttendance == null) {
                // Kiểm tra xem có đơn nghỉ phép được duyệt cho ngày hôm qua không
                boolean hasApprovedLeave = absenceRequestRepository
                        .findByUserUserIdAndStatus(shipper.getUserId(), LeaveStatus.APPROVED)
                        .stream()
                        .anyMatch(request ->
                                !request.getStartDate().toLocalDate().isAfter(dateToCheck) &&
                                        !request.getEndDate().toLocalDate().isBefore(dateToCheck)
                        );

                // Tạo mới bản ghi attendance
                ShipperAttendance attendance = new ShipperAttendance();
                attendance.setUser(shipper);
                attendance.setAttendanceDate(dateToCheck);
                attendance.setCreatedAt(LocalDateTime.now());

                if (hasApprovedLeave) {
                    attendance.setStatus(AttendanceStatus.ON_LEAVE);
                    attendance.setNote("Có đơn nghỉ phép được duyệt");
                    attendance.setIsPresent(false);
                } else {
                    attendance.setStatus(AttendanceStatus.ABSENT);
                    attendance.setNote("Vắng mặt không lý do");
                    attendance.setIsPresent(false);
                }

                shipperAttendanceRepository.save(attendance);
            }
        }
    }



    private CRUDShipperAttendance mapToCRUD(ShipperAttendance attendance) {
        return new CRUDShipperAttendance(
                attendance.getId(),
                attendance.getUser().getUserId(),
                attendance.getAttendanceDate(),
                attendance.getIsPresent(),
                attendance.getCheckInTime(),
                attendance.getNote(),
                attendance.getStatus(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }


    public ResponseEntity<?> getFullMonthlyShipperAttendance(
    Integer userId, int month, int year) {

        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

//        if (user.getRole() != Role.SHIPPER) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
//        }


        if (user.getRole() != Role.SHIPPER && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        // 2. Xác định số ngày trong tháng
        LocalDate startDate = LocalDate.of(year, month, 1);
        int lengthOfMonth = startDate.lengthOfMonth();

        LocalDate endDate = startDate.withDayOfMonth(lengthOfMonth);

        // 3. Lấy danh sách điểm danh đã có trong DB
        List<ShipperAttendance> attendanceList = shipperAttendanceRepository
                .findByUserUserIdAndAttendanceDateBetween(user.getUserId(), startDate, endDate);

        // Tạo map để tra nhanh theo ngày
        Map<LocalDate, ShipperAttendance> attendanceMap = attendanceList.stream()
                .collect(Collectors.toMap(
                        ShipperAttendance::getAttendanceDate,
                        Function.identity(),
                        (existing, replacement) -> existing // hoặc replacement, tùy bạn chọn
                ));



        List<CRUDShipperAttendance> fullMonthList = new ArrayList<>();
        for (int day = 1; day <= lengthOfMonth; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            ShipperAttendance attendance = attendanceMap.get(currentDate);

            CRUDShipperAttendance dto;
            if (attendance != null) {
                dto = mapToCRUD(attendance);
            } else {
                dto = new CRUDShipperAttendance(
                        0, // ID mặc định nếu không có
                        userId,
                        currentDate,
                        false,
                        null,
                        null,
                        AttendanceStatus.NONE,
                        null,
                        null
                );
            }

            fullMonthList.add(dto);
        }

        ListAllShipperAttendanceResponse response = ListAllShipperAttendanceResponse.builder()
                .userId(userId)
                .total(fullMonthList.size())
                .shipperAttendanceList(fullMonthList)
                .build();

        return ResponseEntity.ok(response);
    }



}
