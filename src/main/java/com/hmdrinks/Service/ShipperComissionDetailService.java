package com.hmdrinks.Service;

import com.hmdrinks.Entity.ShipperAttendance;
import com.hmdrinks.Entity.ShipperCommissionDetail;
import com.hmdrinks.Entity.Shippment;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Enum.AttendanceStatus;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Repository.AbsenceRequestRepository;
import com.hmdrinks.Repository.ShipperAttendanceRepository;
import com.hmdrinks.Repository.ShipperCommissionDetailRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Response.CRUDShipperAttendance;
import com.hmdrinks.Response.ListAllShipperAttendanceResponse;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hmdrinks.SupportFunction.SupportFunction.shipmentRepository;

@Service
public class ShipperComissionDetailService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private ShipperAttendanceRepository shipperAttendanceRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private ShipperCommissionDetailRepository shipperCommissionDetailRepository;

//    @Scheduled(cron = "0 0/30 * * * *", zone = "Asia/Ho_Chi_Minh") // chạy mỗi 30 phút
    public void updateAllShipperCommissions() {
        List<User> shipperList = userRepository.findAllByRole(Role.SHIPPER);
        for (User shipper : shipperList) {
            updateShipperCommissionFromPastToNow(shipper.getUserId());
        }
    }


    public interface ShipperCommissionStat {
        LocalDate getCommissionDate();
        Integer getOrderCount();
        BigDecimal getDailyCommission();
    }

    @Transactional
    public void updateShipperCommissionFromPastToNow(Integer userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null || user.getRole() != Role.SHIPPER) return;

        List<ShipperCommissionStat> stats = shipmentRepository.getCommissionStats(userId);
        List<ShipperCommissionDetail> toSave = new ArrayList<>();

        for (ShipperCommissionStat stat : stats) {
            LocalDate date = stat.getCommissionDate();
            int totalOrders = stat.getOrderCount();
            BigDecimal totalCommission = stat.getDailyCommission() != null ? stat.getDailyCommission() : BigDecimal.ZERO;
            double bonus = (totalOrders >= 15) ? 50000 : 0;

            // Kiểm tra có bản ghi chưa
            ShipperCommissionDetail detail = shipperCommissionDetailRepository
                    .findByUserUserIdAndCommissionDate(userId, date)
                    .orElse(new ShipperCommissionDetail());

            detail.setUser(user);
            detail.setCommissionDate(date);
            detail.setOrderCount(totalOrders);
            detail.setDailyCommission(totalCommission);
            detail.setBonus(bonus);
            detail.setNote("Auto-calculated for " + date);

            toSave.add(detail);
        }

        shipperCommissionDetailRepository.saveAll(toSave);
    }



//    @Transactional
//    public void updateShipperCommissionFromPastToNow(Integer userId) {
//        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
//        if (user == null || user.getRole() != Role.SHIPPER) return;
//
//        // Lấy toàn bộ shipment của shipper đó
//        List<Shippment> shipments = shipmentRepository.findAllByUserUserId(userId);
//
//        // Group các shipment theo ngày (LocalDate)
//        Map<LocalDate, List<Shippment>> shipmentsByDate = shipments.stream()
//                .filter(s -> s.getDateShip() != null)
//                .collect(Collectors.groupingBy(s -> s.getDateShip().toLocalDate()));
//
//        for (Map.Entry<LocalDate, List<Shippment>> entry : shipmentsByDate.entrySet()) {
//            LocalDate date = entry.getKey();
//            List<Shippment> dailyShipments = entry.getValue();
//
//            int totalOrders = 0;
//            BigDecimal totalCommission = BigDecimal.ZERO;
//            double totalBonus = 0;
//
//            for (Shippment shipment : dailyShipments) {
//                if (shipment.getDistance() != null) {
//                    totalOrders++;
//                    totalCommission = totalCommission.add(BigDecimal.valueOf(shipment.getDistance() * 1500));
//                }
//            }
//
//            if (totalOrders >= 15) totalBonus = 50000;
//
//            // Check xem đã có record hôm đó chưa
//            ShipperCommissionDetail commissionDetail = shipperCommissionDetailRepository
//                    .findByUserUserIdAndCommissionDate(userId, date)
//                    .orElse(new ShipperCommissionDetail());
//
//            commissionDetail.setUser(user);
//            commissionDetail.setCommissionDate(date);
//            commissionDetail.setDailyCommission(totalCommission);
//            commissionDetail.setOrderCount(totalOrders);
//            commissionDetail.setBonus(totalBonus);
//            commissionDetail.setNote("Auto-calculated for " + date);
//
//            shipperCommissionDetailRepository.save(commissionDetail);
//        }
//    }


}
