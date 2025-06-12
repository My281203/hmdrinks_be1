package com.hmdrinks.Service;

import com.hmdrinks.Enum.Status_Order;
import com.hmdrinks.Enum.Status_Payment;
import com.hmdrinks.Repository.OrderRepository;
import com.hmdrinks.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;

    public ResponseEntity<Double> reportRevenueByDay(LocalDate date) {
        Double revenue = paymentRepository.findTotalRevenueByDate(date.atStartOfDay(), date.atTime(23, 59, 59));
        return ResponseEntity.ok(revenue != null ? revenue : 0.0);
    }

    public ResponseEntity<Map<String, Object>> reportRevenueByMonth(int year, Month month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, month, start.toLocalDate().lengthOfMonth(), 23, 59, 59);

        Map<LocalDate, Double> dailyRevenueMap = getDailyRevenueMap(start, end);
        double totalRevenue = dailyRevenueMap.values().stream().mapToDouble(Double::doubleValue).sum(); // Tính tổng doanh thu

        Map<String, Object> response = new HashMap<>();
        response.put("dailyRevenue", dailyRevenueMap);
        response.put("totalRevenue", totalRevenue);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> reportRevenueByQuarter(int year, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        Map<Month, Double> monthlyRevenueMap = new HashMap<>();

        for (int month = startMonth; month < startMonth + 3; month++) {
            Month currentMonth = Month.of(month);
            double revenue = (Double) reportRevenueByMonth(year, currentMonth).getBody().get("totalRevenue");
            monthlyRevenueMap.put(currentMonth, revenue);
        }

        double totalQuarterRevenue = monthlyRevenueMap.values().stream().mapToDouble(Double::doubleValue).sum();


        Map<String, Object> response = new HashMap<>();
        response.put("monthlyRevenue", monthlyRevenueMap);
        response.put("totalQuarterRevenue", totalQuarterRevenue);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> reportRevenueByYear(int year) {
        Map<Month, Double> monthlyRevenueMap = new HashMap<>();
        double totalYearRevenue = 0.0;

        for (Month month : Month.values()) {
            double revenue = (Double) reportRevenueByMonth(year, month).getBody().get("totalRevenue");
            monthlyRevenueMap.put(month, revenue);
            totalYearRevenue += revenue;
        }

        // Tạo bản đồ trả về
        Map<String, Object> response = new HashMap<>();
        response.put("monthlyRevenue", monthlyRevenueMap);
        response.put("totalYearRevenue", totalYearRevenue);

        return ResponseEntity.ok(response);
    }

    private Map<LocalDate, Double> getDailyRevenueMap(LocalDateTime start, LocalDateTime end) {
        List<Object[]> dailyRevenueData = paymentRepository.findDailyRevenueByDate(start, end);
        Map<LocalDate, Double> dailyRevenueMap = new HashMap<>();

        for (Object[] result : dailyRevenueData) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            Double revenue = result.length > 1 ? (Double) result[1] : 0.0;
            dailyRevenueMap.put(date, revenue != null ? revenue : 0.0);
        }
        return dailyRevenueMap;
    }

    public MonthlyOrderStats getMonthlyOrderStats(int year, int month) {
        RevenueAndCountPair success = getSuccessfulOrdersCount(year, month);
        CountPair cancelled = getCancelledOrdersCount(year, month);
        return new MonthlyOrderStats(success, cancelled);
    }

    public RevenueAndCountPair getSuccessfulOrdersCount(int year, int month) {
        LocalDateTime startThis = getStartOfMonth(year, month);
        LocalDateTime endThis = getEndOfMonth(year, month);
        LocalDateTime startLast = startThis.minusMonths(1);
        LocalDateTime endLast = startThis.minusSeconds(1);

        long successThisCount = paymentRepository.countByOrder_OrderDateBetweenAndStatus(startThis, endThis, Status_Payment.COMPLETED);
        long successLastCount = paymentRepository.countByOrder_OrderDateBetweenAndStatus(startLast, endLast, Status_Payment.COMPLETED);

        Double revenueThis = paymentRepository.sumAmountByOrder_OrderDateBetweenAndStatus(startThis, endThis, Status_Payment.COMPLETED);
        Double revenueLast = paymentRepository.sumAmountByOrder_OrderDateBetweenAndStatus(startLast, endLast, Status_Payment.COMPLETED);

        return new RevenueAndCountPair(
                new CountPair(successThisCount, successLastCount),
                new RevenuePair(revenueThis != null ? revenueThis : 0.0, revenueLast != null ? revenueLast : 0.0)
        );
    }

    public CountPair getCancelledOrdersCount(int year, int month) {
        LocalDateTime startThis = getStartOfMonth(year, month);
        LocalDateTime endThis = getEndOfMonth(year, month);
        LocalDateTime startLast = startThis.minusMonths(1);
        LocalDateTime endLast = startThis.minusSeconds(1);

        long cancelThis = orderRepository.countByOrderDateBetweenAndStatus(startThis, endThis, Status_Order.CANCELLED);
        long cancelLast = orderRepository.countByOrderDateBetweenAndStatus(startLast, endLast, Status_Order.CANCELLED);

        return new CountPair(cancelThis, cancelLast);
    }

    private LocalDateTime getStartOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay().minusSeconds(1);
    }

    public static class MonthlyOrderStats {
        private final RevenueAndCountPair success;
        private final CountPair cancelled;

        public MonthlyOrderStats(RevenueAndCountPair success, CountPair cancelled) {
            this.success = success;
            this.cancelled = cancelled;
        }

        public RevenueAndCountPair getSuccess() {
            return success;
        }

        public CountPair getCancelled() {
            return cancelled;
        }
    }

    public static class CountPair {
        private final long thisMonth;
        private final long lastMonth;

        public CountPair(long thisMonth, long lastMonth) {
            this.thisMonth = thisMonth;
            this.lastMonth = lastMonth;
        }

        public long getThisMonth() {
            return thisMonth;
        }

        public long getLastMonth() {
            return lastMonth;
        }
    }

    public static class RevenuePair {
        private final double thisMonth;
        private final double lastMonth;

        public RevenuePair(double thisMonth, double lastMonth) {
            this.thisMonth = thisMonth;
            this.lastMonth = lastMonth;
        }

        public double getThisMonth() {
            return thisMonth;
        }

        public double getLastMonth() {
            return lastMonth;
        }
    }

    public static class RevenueAndCountPair {
        private final CountPair count;
        private final RevenuePair revenue;

        public RevenueAndCountPair(CountPair count, RevenuePair revenue) {
            this.count = count;
            this.revenue = revenue;
        }

        public CountPair getCount() {
            return count;
        }

        public RevenuePair getRevenue() {
            return revenue;
        }
    }




}
