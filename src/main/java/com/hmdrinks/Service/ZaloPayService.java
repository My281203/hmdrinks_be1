package com.hmdrinks.Service;

import cats.kernel.Order;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.SupportFunction.DistanceAndDuration;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ZaloPayService {
    // APP INFO
    @Value("${api.user-service.url}")
    private String userServiceUrl;

    @Value("${api.group-order.url}")
    private String groupOrderUrl;

    private static final String APP_ID = "2553";
    private static final String KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String KEY2 = "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
    private static final String ENDPOINT_CREATE = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String ENDPOINT_CHECK_STATUS = "https://sb-openapi.zalopay.vn/v2/query";
    private final PaymentRepository paymentRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserVoucherRepository userVoucherRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private  UserCointRepository userCointRepository;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private  CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private  PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private  GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private  GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private ShipmentGroupRepository shipmentGroupRepository;

    public ZaloPayService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    private static final double ALPHA = 1.0;
    private static final double BETA = 1.0;
    private static final double GAMMA = 1.0;
    private static final double DELTA = 1.0;

    private static final double[] STORE_ORIGIN = {10.850575879000075, 106.77190192800003};


    public class ShipperSelectionResult {
        private User user;
        private double distance;

        public ShipperSelectionResult(User user, double distance) {
            this.user = user;
            this.distance = distance;
        }

        public User getUser() {
            return user;
        }

        public double getDistance() {
            return distance;
        }
    }
    private static Long generateRandomOrderCode() {
        Random random = new Random();
        int randomNumber = 100000 + random.nextInt(900000);
        return Long.valueOf(randomNumber);
    }


    @Transactional
    public void assignAllPendingTasks() {
        LocalDate today = LocalDate.now();
        List<User> available = getAvailableShippers(today);
        available.sort(Comparator.comparingInt(u -> u.getShipperDetail().getTotalOrdersToday()));

        List<Shippment> shipments = shipmentRepository.findByStatus(Status_Shipment.WAITING);
        shipments.sort(Comparator.comparing(Shippment::getDateCreated));
        for (Shippment s : shipments) {
            if(s.getUser() == null) {
                assignShipment(s, available, today);
            }

        }

        List<ShippmentGroup> groups = shipmentGroupRepository.findByStatus(Status_Shipment.WAITING);
        groups.sort(Comparator.comparing(ShippmentGroup::getDateCreated));
        for (ShippmentGroup g : groups) {
            if(g.getUser() == null) {
                assignShipmentGroup(g, available, today);
            }

        }
    }

    @Transactional
    public void assignShipment(Shippment shipment, List<User> available, LocalDate today) {
        double lat = shipment.getPayment().getOrder().getLatitude();
        double lng = shipment.getPayment().getOrder().getLongitude();
        double[] dest = new double[]{lat, lng};

        ShipperSelectionResult result = selectBestShipper(shipment, null, available, dest, today);
        if (result != null) {
            allocateShipment(shipment, result.getUser(),result.getDistance());

        }
    }


    @Transactional
    public void assignShipmentGroup(ShippmentGroup group, List<User> available, LocalDate today) {
        double lat = group.getPayment().getGroupOrder().getLatitude();
        double lng = group.getPayment().getGroupOrder().getLongitude();
        double[] dest = new double[]{lat, lng};


        ShipperSelectionResult result = selectBestShipper(null, group, available, dest, today);
        if (result != null) {
            allocateGroup(group, result.getUser(),result.getDistance());

        }
//        User selected = selectBestShipper(null, group, available, dest, today);
//        if (selected != null) allocateGroup(group, selected);
    }

    public class ShipperCost {
        private final User user;
        private final double cost;
        private final double distance;

        public ShipperCost(User user, double cost, double distance) {
            this.user = user;
            this.cost = cost;
            this.distance = distance;
        }

        public User getUser() {
            return user;
        }

        public double getCost() {
            return cost;
        }

        public double getDistance() {
            return distance;
        }
    }




    private ShipperSelectionResult selectBestShipper(Shippment shipment, ShippmentGroup group, List<User> avail, double[] dest, LocalDate today) {
        for (User u : avail) {
            if (groupable(u, shipment, group, today)) {
                double distance = calculateCostInfo(u, dest).getDistance(); // lấy distance từ hàm chung
                return new ShipperSelectionResult(u, distance);
            }
        }

        return avail.stream()
                .map(u -> calculateCostInfo(u, dest))
                .min(Comparator.comparingDouble(ShipperCost::getCost))
                .map(sc -> new ShipperSelectionResult(sc.getUser(), sc.getDistance()))
                .orElse(null);
    }



    private ShipperCost calculateCostInfo(User u, double[] dest) {
        double[] origin = getShipperOrigin(u);
        DistanceAndDuration dd = supportFunction.getShortestDistance(origin, dest);
        double tts = parseDurationToMinutes(dd.getDuration());
        int count = u.getShippments().size() + u.getShippmentGroups().size();
        double tot = calculateTotalTravelTime(u);
        double cost = ALPHA * tts + BETA * count + GAMMA * tot + DELTA * dd.getDistance();
        return new ShipperCost(u, cost, dd.getDistance());
    }
    private double calculateCostScore(User u, double[] dest) {
        double[] origin = getShipperOrigin(u);
        DistanceAndDuration dd = supportFunction.getShortestDistance(origin, dest);
        double tts = parseDurationToMinutes(dd.getDuration());
        int count = u.getShippments().size() + u.getShippmentGroups().size();
        double tot = calculateTotalTravelTime(u);
        return ALPHA * tts + BETA * count + GAMMA * tot + DELTA * dd.getDistance();
    }

    private double[] getShipperOrigin(User shipper) {
//        List<LocalDateTime> deliveryTimes = new ArrayList<>();
//        List<double[]> locations = new ArrayList<>();
//
//        for (Shippment s : shipper.getShippments()) {
//            if(s.getStatus() == Status_Shipment.WAITING && s.getUser() != null)
//            {
//                if (s.getDateDelivered() != null) {
//                    deliveryTimes.add(s.getDateDelivered());
//                    double lat = s.getPayment().getOrder().getLatitude();
//                    double lng = s.getPayment().getOrder().getLongitude();
//                    double[] coord = new double[]{lat, lng};
//                    locations.add(coord);
//                }
//            }
//
//        }
//
//        for (ShippmentGroup g : shipper.getShippmentGroups()) {
//            if(g.getStatus() == Status_Shipment.WAITING && g.getUser() != null)
//            {
//                if (g.getDateDelivered() != null) {
//                    deliveryTimes.add(g.getDateDelivered());
//                    double lat = g.getPayment().getGroupOrder().getLatitude();
//                    double lng = g.getPayment().getGroupOrder().getLongitude();
//                    double[] coord = new double[]{lat, lng};
//                    locations.add(coord);
//                }
//            }
//
//        }

//        if (deliveryTimes.isEmpty())
        return STORE_ORIGIN;
//        int latestIndex = IntStream.range(0, deliveryTimes.size())
//                .boxed().max(Comparator.comparing(deliveryTimes::get)).orElse(0);
//        return locations.get(latestIndex);
    }


    private boolean groupable(User u, Shippment s, ShippmentGroup g, LocalDate today) {
        List<double[]> allDest = new ArrayList<>();

        for (Shippment sh : u.getShippments()) {
            if(sh.getStatus() == Status_Shipment.WAITING && sh.getUser() == null)
            {
                if (sh.getDateShip() != null && sh.getDateShip().toLocalDate().equals(today)) {
                    double lat = sh.getPayment().getOrder().getLatitude();
                    double lng = sh.getPayment().getOrder().getLongitude();
                    double[] coord = new double[]{lat, lng};
                    allDest.add(coord);
                }
            }

        }

        for (ShippmentGroup gr : u.getShippmentGroups()) {
            if(gr.getStatus() == Status_Shipment.WAITING && gr.getUser() == null)
            {
                if (gr.getDateShip() != null && gr.getDateShip().toLocalDate().equals(today)) {
                    double lat = gr.getPayment().getGroupOrder().getLatitude();
                    double lng = gr.getPayment().getGroupOrder().getLongitude();
                    double[] coord = new double[]{lat, lng};
                    allDest.add(coord);
                }
            }

        }

        double lat, lng;

        if (s != null) {
            Orders order = s.getPayment().getOrder();
            lat = order.getLatitude();
            lng = order.getLongitude();
        } else {
            GroupOrders groupOrder = g.getPayment().getGroupOrder();
            lat = groupOrder.getLatitude();
            lng = groupOrder.getLongitude();
        }

        double[] newDest = new double[]{lat, lng};


        for (double[] dest : allDest) {
            if (supportFunction.getShortestDistance(dest, newDest).getDistance() <= 1.0) {
                return true;
            }
        }

        return false;
    }


    private List<User> getAvailableShippers(LocalDate today) {
        List<User> all = userRepository.findAllByRole(Role.SHIPPER);
        List<User> list = new ArrayList<>();
        for (User u : all) {
            ShipperDetail d = u.getShipperDetail();
            if (d.getOnLeave() || isOnAbsence(u, today)) continue;
            if (d.getStatus().equals("busy")) continue;
            if (d.getStatus().equals("busy") && (!canGroupAny(u, today) || isNotReturned(d))) continue;
            list.add(u);
        }
        return list;
    }

    private boolean isOnAbsence(User u, LocalDate today) {
        return absenceRequestRepository
                .findByUserUserIdAndStatus(u.getUserId(), LeaveStatus.APPROVED)
                .stream().anyMatch(r -> !r.getStartDate().toLocalDate().isAfter(today)
                        && !r.getEndDate().toLocalDate().isBefore(today));
    }

    private boolean isNotReturned(ShipperDetail d) {
        return d.getExpectedReturnTime() != null && d.getExpectedReturnTime().isAfter(LocalDateTime.now());
    }

    private boolean canGroupAny(User u, LocalDate today) {
        List<Object> tasks = new ArrayList<>();
        tasks.addAll(u.getShippments());
        tasks.addAll(u.getShippmentGroups());
        return canGroupList(tasks, today, null);
    }

    private boolean canGroupList(List<Object> tasks, LocalDate today, Object newTask) {
        Object last = null;
        LocalDateTime lastCreated = null;

        for (Object task : tasks) {
            LocalDateTime dateShip = null;
            LocalDateTime dateCreated = null;
            Status_Shipment status = null;

            if (task instanceof Shippment) {
                Shippment s = (Shippment) task;
                dateShip = s.getDateShip();
                dateCreated = s.getDateCreated();
                status = s.getStatus();
            } else if (task instanceof ShippmentGroup) {
                ShippmentGroup g = (ShippmentGroup) task;
                dateShip = g.getDateShip();
                dateCreated = g.getDateCreated();
                status = g.getStatus();
            }

            if (dateShip != null && dateShip.toLocalDate().equals(today) && status == Status_Shipment.WAITING) {
                if (last == null || dateCreated.isAfter(lastCreated)) {
                    last = task;
                    lastCreated = dateCreated;
                }
            }
        }

        if (last == null || lastCreated == null) return false;
        long mins = Duration.between(lastCreated, LocalDateTime.now()).toMinutes();

        // So sánh user nếu có newTask
        if (newTask != null && mins <= 5) {
            Integer lastUserId = getUserIdFromTask(last);
            Integer newUserId = getUserIdFromTask(newTask);
            if (Objects.equals(lastUserId, newUserId)) return true;
        }

        double[] a = getCoordinatesFromTask(last);
        double[] b = (newTask != null) ? getCoordinatesFromTask(newTask) : a;
        return supportFunction.getShortestDistance(a, b).getDistance() <= 1.0;
    }

    private double[] getCoordinatesFromTask(Object task) {
        double lat = 0;
        double lng = 0;

        if (task instanceof Shippment) {
            Orders order = ((Shippment) task).getPayment().getOrder();
            lat = order.getLatitude();
            lng = order.getLongitude();
        } else if (task instanceof ShippmentGroup) {
            GroupOrders groupOrder = ((ShippmentGroup) task).getPayment().getGroupOrder();
            lat = groupOrder.getLatitude();
            lng = groupOrder.getLongitude();
        }

        return new double[]{lat, lng};
    }


    private Integer getUserIdFromTask(Object task) {
        if (task instanceof Shippment) {
            return ((Shippment) task).getPayment().getOrder().getUser().getUserId();
        } else if (task instanceof ShippmentGroup) {
            return ((ShippmentGroup) task).getPayment().getGroupOrder().getUser().getUserId();
        }
        return null;
    }


    private double calculateTotalTravelTime(User u) {
        double sum = 0;

        // Tính tổng thời gian cho các shipment thông thường chưa giao (status WAITING hoặc SHIPPING)
        sum += u.getShippments().stream()
                .filter(s -> s.getStatus() == Status_Shipment.WAITING) // Lọc đơn chưa giao (WAITING, SHIPPING)
                .mapToDouble(s -> travelMinutes(u, s.getPayment().getOrder().getAddress())) // Tính thời gian vận chuyển cho từng đơn
                .sum();

        // Tính tổng thời gian cho các shipment group chưa giao (status WAITING hoặc SHIPPING)
        for (ShippmentGroup g : u.getShippmentGroups()) {
            // Chỉ tính shipment group chưa giao, không liên quan đến "Shippments" bên trong
            if (g.getStatus() == Status_Shipment.WAITING) { // Lọc group chưa giao (WAITING, SHIPPING)
                sum += travelMinutes(u, g.getPayment().getGroupOrder().getAddress()); // Tính thời gian cho group shipment chưa giao
            }
        }

        return sum;
    }



    private int travelMinutes(User u, String addr) {
        double[] from = getShipperOrigin(u);
        double[] to = supportFunction.getCoordinates(supportFunction.getLocation(addr));
        return parseDurationToMinutes(supportFunction.getShortestDistance(from, to).getDuration());
    }


    public static int parseDurationToMinutes(String duration) {
        int hours = 0, mins = 0;
        if (duration == null || duration.isBlank()) return 25;
        if (duration.contains("giờ")) {
            String[] parts = duration.split("giờ", 2);
            try { hours = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
            if (parts.length > 1 && parts[1].contains("phút")) {
                try { mins = Integer.parseInt(parts[1].replace("phút", "").trim()); } catch (Exception ignored) {}
            }
        } else if (duration.contains("phút")) {
            try { mins = Integer.parseInt(duration.replace("phút", "").trim()); } catch (Exception ignored) {}
        }
        mins += 5;
        return hours * 60 + mins;
    }

    private void allocateShipment(Shippment s, User u, double distance) {
        LocalDateTime now = LocalDateTime.now();
        int mins = travelMinutes(u, s.getPayment().getOrder().getAddress());
        s.setUser(u);
        s.setDateShip(now);
        s.setDateDelivered(now.plusMinutes(mins));
        s.setDistance(distance);
        shipmentRepository.save(s);

        u.getShippments().add(s);
        u.getShipperDetail().setStatus("busy");
        u.getShipperDetail().setTotalOrdersToday(u.getShipperDetail().getTotalOrdersToday() + 1);
        double lat = s.getPayment().getOrder().getLatitude();
        double lng = s.getPayment().getOrder().getLongitude();
        double[] coord = new double[]{lat, lng};
        supportFunction.getDirections(STORE_ORIGIN, coord, s.getShipmentId());
        userRepository.save(u);
        notificationService.sendNotification(u.getUserId(), s.getShipmentId(), "Bạn có đơn mới");
    }

    private void allocateGroup(ShippmentGroup g, User u,Double distance) {
        LocalDateTime now = LocalDateTime.now();
        String groupAddress = g.getPayment().getGroupOrder().getAddress();
        int mins = travelMinutes(u,g.getPayment().getGroupOrder().getAddress());
        g.setUser(u);
        g.setDateShip(now);
        g.setDateDelivered(now.plusMinutes(mins));
        g.setDistance(distance);
        shipmentGroupRepository.save(g);

        double lat = g.getPayment().getGroupOrder().getLatitude();
        double lng = g.getPayment().getGroupOrder().getLongitude();
        double[] coord = new double[]{lat, lng};
        supportFunction.getDirectionsGroup(STORE_ORIGIN, coord, g.getShipmentId());
        u.getShippmentGroups().add(g);
        u.getShipperDetail().setStatus("busy");
        u.getShipperDetail().setTotalOrdersToday(u.getShipperDetail().getTotalOrdersToday() + 1);
        userRepository.save(u);
        notificationService.sendNotification(u.getUserId(), g.getPayment().getGroupOrder().getGroupOrderId(), "Bạn có nhóm đơn mới");
    }







    public static LocalDateTime addDurationToCurrentTime(String duration, LocalDateTime currentTime) {
        int hours = 0;
        int minutes = 0;

        LocalDateTime now = LocalDateTime.now();
        if (currentTime.isBefore(now)) {
            return now.plusMinutes(20);
        }

        if (duration.contains("giờ")) {
            String[] parts = duration.split("giờ");
            hours = Integer.parseInt(parts[0].trim());
            if (parts.length > 1 && parts[1].contains("phút")) {
                minutes = Integer.parseInt(parts[1].replace("phút", "").trim()) + 25;
            }
        } else if (duration.contains("phút")) {
            minutes = Integer.parseInt(duration.replace("phút", "").trim()) + 25;
        }

        if (hours == 0 && minutes == 0) {
            minutes += 25;
        }

        if (minutes >= 60) {
            hours += minutes / 60;
            minutes = minutes % 60;
        }

        return currentTime.plusHours(hours).plusMinutes(minutes);
    }

    public boolean isTimeConflict(LocalDateTime startNew, LocalDateTime endNew, List<LocalDateTime[]> existingTimeRanges) {
        for (LocalDateTime[] range : existingTimeRanges) {
            if (!(endNew.isBefore(range[0]) || startNew.isAfter(range[1]))) {
                return true;
            }
        }
        return false;
    }



    @Transactional
    public boolean assignShipments(int orderId) {
        List<Shippment> pendingShipments = shipmentRepository.findByStatus(Status_Shipment.WAITING)
                .stream()
                .sorted(Comparator.comparing(Shippment::getDateCreated))
                .collect(Collectors.toList());

        Orders orders = orderRepository.findByOrderId(orderId);
        String placeId = supportFunction.getLocation(orders.getAddress());
        double[] destination = supportFunction.getCoordinates(placeId);
        double[] origin = {10.850575879000075, 106.77190192800003};
        double[] lastOrigin = new double[0];

        List<User> allShippers = userRepository.findAllByRole(Role.SHIPPER);
        LocalDate today = LocalDate.now();

        List<User> availableShippers = allShippers.stream()
                .filter(shipper -> absenceRequestRepository.findByUserUserIdAndStatus(shipper.getUserId(), LeaveStatus.APPROVED)
                        .stream()
                        .noneMatch(req -> !req.getStartDate().toLocalDate().isAfter(today)
                                && !req.getEndDate().toLocalDate().isBefore(today)))
                .collect(Collectors.toList());

        System.out.println("Tổng số shipment đang chờ: " + pendingShipments.size());
        System.out.println("Số shipper sẵn sàng: " + availableShippers.size());

        for (Shippment shipment : pendingShipments) {
            System.out.println("\n=== Đang xử lý shipment: " + shipment.getShipmentId() + " ===");

            User selectedShipper = null;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime estimatedDeliveryTime = now;
            double distanceResult = 0;

            for (User shipper : availableShippers) {
                System.out.println("-> Đang kiểm tra shipper: " + shipper.getUserId());

                List<LocalDateTime[]> busyTimeRanges = new ArrayList<>();

                // Busy từ đơn lẻ
                for (Shippment s : shipper.getShippments()) {
                    if (s.getDateShip() != null && s.getDateDelivered() != null && s.getStatus() != Status_Shipment.CANCELLED) {
                        busyTimeRanges.add(new LocalDateTime[]{s.getDateShip(), s.getDateDelivered()});
                    }
                }

                // Busy từ group
                for (ShippmentGroup sg : shipper.getShippmentGroups()) {
                    if (sg.getDateShip() != null && sg.getDateDelivered() != null && sg.getStatus() != Status_Shipment.CANCELLED) {
                        busyTimeRanges.add(new LocalDateTime[]{sg.getDateShip(), sg.getDateDelivered()});
                    }
                }

                double[] lastDestination = origin;

                // Shipment đã giao hôm nay
                List<Shippment> todayShipments = shipper.getShippments().stream()
                        .filter(s -> s.getDateShip() != null &&
                                s.getDateShip().toLocalDate().isEqual(today) &&
                                s.getStatus() != Status_Shipment.CANCELLED)
                        .collect(Collectors.toList());

                if (!todayShipments.isEmpty()) {
                    Shippment lastShipment = todayShipments.get(todayShipments.size() - 1);
                    lastDestination = supportFunction.getCoordinates(
                            supportFunction.getLocation(lastShipment.getPayment().getOrder().getAddress()));

                    DistanceAndDuration distance = supportFunction.getShortestDistance(lastDestination, destination);
                    System.out.println("  - Khoảng cách (có đơn hôm nay): " + distance.getDistance());

                    if (distance.getDistance() > 10) {
                        System.out.println("  - Bỏ qua do khoảng cách > 10km");
                        continue;
                    }

                    String duration = distance.getDuration();
                    LocalDateTime baseTime;
                    if (lastShipment.getDateDelivered() != null) {
                        baseTime = lastShipment.getDateDelivered();
                    } else if (lastShipment.getDateShip() != null) {
                        baseTime = lastShipment.getDateShip().plusMinutes(20); // giả sử 20 phút giao hàng
                    } else {
                        baseTime = now;
                    }

                    estimatedDeliveryTime = addDurationToCurrentTime(duration, baseTime);
                    lastOrigin = lastDestination;
                    distanceResult = distance.getDistance();
                } else {
                    DistanceAndDuration distance = supportFunction.getShortestDistance(origin, destination);
                    System.out.println("  - Khoảng cách (từ kho): " + distance.getDistance());

                    if (distance.getDistance() > 20) {
                        System.out.println("  - Bỏ qua do khoảng cách > 20km từ kho");
                        continue;
                    }

                    String duration = distance.getDuration();
                    estimatedDeliveryTime = addDurationToCurrentTime(duration, now);
                    lastOrigin = origin;
                    distanceResult = distance.getDistance();
                }

                LocalDateTime estimatedStart = estimatedDeliveryTime.minusMinutes(10);
                LocalDateTime estimatedEnd = estimatedDeliveryTime;
                boolean conflict = isTimeConflict(estimatedStart, estimatedEnd, busyTimeRanges);
                System.out.println("  - Có trùng thời gian không: " + conflict);

                if (conflict) continue;

                selectedShipper = shipper;
                System.out.println("✅ Chọn shipper: " + selectedShipper.getUserId());
                break;
            }

            if (selectedShipper == null) {
                System.out.println("⚠️ Không tìm được shipper cho đơn hàng: " + shipment.getShipmentId());
                continue;
            }

            shipment.setUser(selectedShipper);
            shipment.setStatus(Status_Shipment.WAITING);
            shipment.setDateDelivered(estimatedDeliveryTime);
            shipment.setDateShip(estimatedDeliveryTime.minusMinutes(10));
            shipment.setDistance(distanceResult);
            shipmentRepository.save(shipment);

            selectedShipper.getShippments().add(shipment);
            supportFunction.getDirections(lastOrigin, destination, shipment.getShipmentId());

            try {
                notificationService.sendNotification(
                        selectedShipper.getUserId(),
                        shipment.getShipmentId(),
                        "Bạn có đơn hàng mới cần giao"
                );
            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo: " + e.getMessage());
            }
        }

        return true;
    }


    @Transactional
    public boolean assignShipmentGroups(int groupOrderId) {
        List<ShippmentGroup> pendingGroups = shipmentGroupRepository.findByStatus(Status_Shipment.WAITING)
                .stream()
                .sorted(Comparator.comparing(ShippmentGroup::getDateCreated))
                .collect(Collectors.toList());

        GroupOrders groupOrders = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        String placeId = supportFunction.getLocation(groupOrders.getAddress());
        double[] destination = supportFunction.getCoordinates(placeId);
        double[] origin = {10.850575879000075, 106.77190192800003};  // Kho hàng
        double[] lastOrigin = new double[0];

        List<User> allShippers = userRepository.findAllByRole(Role.SHIPPER);
        LocalDate today = LocalDate.now();

        List<User> availableShippers = allShippers.stream()
                .filter(shipper -> absenceRequestRepository.findByUserUserIdAndStatus(shipper.getUserId(), LeaveStatus.APPROVED)
                        .stream()
                        .noneMatch(req -> !req.getStartDate().toLocalDate().isAfter(today) && !req.getEndDate().toLocalDate().isBefore(today)))
                .collect(Collectors.toList());

        System.out.println("Tổng số group đang chờ: " + pendingGroups.size());
        System.out.println("Số shipper sẵn sàng: " + availableShippers.size());

        for (ShippmentGroup group : pendingGroups) {
            System.out.println("\n=== Đang xử lý group: " + group.getShipmentId() + " ===");
            User selectedShipper = null;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime estimatedDeliveryTime = now;
            double distance_result = 0;

            for (User shipper : availableShippers) {
                System.out.println("-> Đang kiểm tra shipper: " + shipper.getUserId());

                List<LocalDateTime[]> busyTimeRanges = new ArrayList<>();

                // Kiểm tra thời gian bận của các đơn hàng trước của shipper
                for (Shippment s : shipper.getShippments()) {
                    if (s.getDateShip() != null && s.getDateDelivered() != null && s.getStatus() != Status_Shipment.CANCELLED) {
                        busyTimeRanges.add(new LocalDateTime[]{s.getDateShip(), s.getDateDelivered()});
                    }
                }

                // Kiểm tra thời gian bận từ các nhóm đơn hàng
                for (ShippmentGroup sg : shipper.getShippmentGroups()) {
                    if (sg.getDateShip() != null && sg.getDateDelivered() != null && sg.getStatus() != Status_Shipment.CANCELLED) {
                        busyTimeRanges.add(new LocalDateTime[]{sg.getDateShip(), sg.getDateDelivered()});
                    }
                }

                double[] lastDestination = origin;
                LocalDateTime lastDeliveredTime = null;

                // Tìm đơn hàng giao gần nhất
                for (Shippment s : shipper.getShippments()) {
                    if (s.getDateDelivered() != null &&
                            (lastDeliveredTime == null || s.getDateDelivered().isAfter(lastDeliveredTime))) {
                        lastDeliveredTime = s.getDateDelivered();
                        lastDestination = supportFunction.getCoordinates(
                                supportFunction.getLocation(s.getPayment().getOrder().getAddress()));
                    }
                }

                // Tìm nhóm đơn hàng giao gần nhất
                for (ShippmentGroup sg : shipper.getShippmentGroups()) {
                    if (sg.getDateDelivered() != null &&
                            (lastDeliveredTime == null || sg.getDateDelivered().isAfter(lastDeliveredTime))) {
                        lastDeliveredTime = sg.getDateDelivered();
                        lastDestination = supportFunction.getCoordinates(
                                supportFunction.getLocation(sg.getPayment().getGroupOrder().getAddress()));
                    }
                }

                DistanceAndDuration distance;
                LocalDateTime baseTime;

                // Xác định baseTime
                if (lastDeliveredTime != null) {
                    baseTime = lastDeliveredTime;
                } else if (lastDeliveredTime == null && lastDeliveredTime != null) {
                    baseTime = lastDeliveredTime;
                } else {
                    baseTime = now;
                }

                distance = supportFunction.getShortestDistance(lastDestination, destination);
                System.out.println("  - Khoảng cách (từ điểm giao gần nhất): " + distance.getDistance());
                if (distance.getDistance() > 10) {
                    System.out.println("  - Bỏ qua do khoảng cách > 10km");
                    continue;
                }
                estimatedDeliveryTime = addDurationToCurrentTime(distance.getDuration(), baseTime);
                lastOrigin = lastDestination;
                distance_result = distance.getDistance();

                LocalDateTime estimatedStart = estimatedDeliveryTime.minusMinutes(10);
                LocalDateTime estimatedEnd = estimatedDeliveryTime;
                boolean conflict = isTimeConflict(estimatedStart, estimatedEnd, busyTimeRanges);
                System.out.println("  - Có trùng thời gian không: " + conflict);
                if (conflict) {
                    continue;
                }

                selectedShipper = shipper;
                System.out.println("✅ Chọn shipper: " + selectedShipper.getUserId());
                break;
            }

            if (selectedShipper == null) {
                System.out.println("⚠️ Không tìm được shipper cho group: " + group.getShipmentId());
                continue;
            }

            group.setUser(selectedShipper);
            group.setStatus(Status_Shipment.WAITING);
            group.setDateDelivered(estimatedDeliveryTime);
            group.setDateShip(estimatedDeliveryTime.minusMinutes(10));
            group.setDistance(distance_result);
            shipmentGroupRepository.save(group);

            selectedShipper.getShippmentGroups().add(group);
            supportFunction.getDirectionsGroup(lastOrigin, destination, group.getShipmentId());

            try {
                notificationService.sendNotification(selectedShipper.getUserId(), group.getShipmentId(), "Bạn có nhóm đơn hàng mới cần giao");
            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo: " + e.getMessage());
            }
        }

        return true;
    }

    public Map<String, Object> createPayment(Long total,String  type) throws Exception {
        String callback_url = "";
        String ridirect_url = "";
        if (Objects.equals(type, "WEB"))
        {
            callback_url = userServiceUrl + "/callback/android/zalo";
            ridirect_url = userServiceUrl + "/redirect/web/zalo";

        }
        else
        {
            callback_url = userServiceUrl + "/callback/android/zalo";
            ridirect_url = userServiceUrl + "/redirect/android/zalo";
        }

        String transId = String.valueOf(new Random().nextInt(1000000));
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", APP_ID);
        order.put("app_trans_id", getCurrentTimeString("yyMMdd") + "_" + transId);
        order.put("app_user", "user123");
        order.put("app_time", System.currentTimeMillis());
        order.put("amount", total);
        order.put("description", "HMDrinks - Payment for the order #" + transId);
        order.put("bank_code", "");
        order.put("callback_url", callback_url);
        order.put("embed_data", "{\"redirecturl\":\"" + ridirect_url + "\"}");

//        order.put("callback_url", "https://8bc2-2001-ee0-5005-b8f0-4d7e-4499-cfaf-faa6.ngrok-free.app/payment-online-status/");
//        order.put("embed_data", "{\"redirecturl\":\"http://localhost:5173/payment-online-status\"}");
        order.put("item", "[]");

        String data = APP_ID + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|" +
                order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|" + order.get("item");

        // Tạo chữ ký `mac`
        String mac = generateHmacSHA256(data, KEY1);
        order.put("mac", mac);
        Map<String, Object> result = new HashMap<>();

        try {

            JSONObject response = sendRequest(ENDPOINT_CREATE, order);

            String payUrl = response.optString("order_url");
            result.put("order_url", payUrl);
            result.put("app_trans_id", (String) order.get("app_trans_id"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Map<String, Object> createPaymentGroup(Long total,String  type) throws Exception {
        String callback_url = "";
        String ridirect_url = "";
        if (Objects.equals(type, "WEB"))
        {

            callback_url = userServiceUrl + "/callback/android/group/zalo";
            ridirect_url = userServiceUrl + "/redirect/web/zalo";
//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/zalo";
//            ridirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/web/zalo";
        }
        else
        {
            callback_url = userServiceUrl + "/callback/android/group/zalo";
            ridirect_url = userServiceUrl + "/redirect/android/group/zalo";
//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/zalo";
//            ridirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/android/group/zalo";

        }


        String transId = String.valueOf(new Random().nextInt(1000000));
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", APP_ID);
        order.put("app_trans_id", getCurrentTimeString("yyMMdd") + "_" + transId);
        order.put("app_user", "user123");
        order.put("app_time", System.currentTimeMillis());
        order.put("amount", total);
        order.put("description", "HMDrinks - Payment for the order #" + transId);
        order.put("bank_code", "");
        order.put("callback_url", callback_url);
        order.put("embed_data", "{\"redirecturl\":\"" + ridirect_url + "\"}");

//        order.put("callback_url", "https://8bc2-2001-ee0-5005-b8f0-4d7e-4499-cfaf-faa6.ngrok-free.app/payment-online-status/");
//        order.put("embed_data", "{\"redirecturl\":\"http://localhost:5173/payment-online-status\"}");
        order.put("item", "[]");

        String data = APP_ID + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|" +
                order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|" + order.get("item");

        // Tạo chữ ký `mac`
        String mac = generateHmacSHA256(data, KEY1);
        order.put("mac", mac);
        Map<String, Object> result = new HashMap<>();

        try {

            JSONObject response = sendRequest(ENDPOINT_CREATE, order);

            String payUrl = response.optString("order_url");
            result.put("order_url", payUrl);
            result.put("app_trans_id", (String) order.get("app_trans_id"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getCurrentTimeString(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }

    private static String generateHmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static JSONObject sendRequest(String endpoint, Map<String, Object> order) throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(endpoint);
        List<NameValuePair> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : order.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
        }

        post.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = client.execute(post);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return new JSONObject(result.toString());
    }

    public static String hmacSHA256(String key, String data) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKey);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static boolean checkStatusOrder(String appTransId) {
        try {
            // Tạo dữ liệu chữ ký MAC
            String data = APP_ID + "|" + appTransId + "|" + KEY1;
            String mac = hmacSHA256(KEY1, data);
            String postData = "app_id=" + APP_ID + "&app_trans_id=" + appTransId + "&mac=" + mac;

            // Thiết lập kết nối HTTP
            URL url = new URL(ENDPOINT_CHECK_STATUS);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);


            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return response.contains("\"return_code\":1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Transactional
    public ResponseEntity<?> handleCallBack(String appTransId) {
        Payment payment = paymentRepository.findByOrderIdPayment(appTransId);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        boolean isSuccess = checkStatusOrder(appTransId);
        System.out.println(isSuccess);
        if (isSuccess != false) {
            if (payment.getStatus() != Status_Payment.FAILED || payment.getStatus() != Status_Payment.COMPLETED) {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentRepository.save(payment);
                Shippment shippment = new Shippment();
                shippment.setPayment(payment);
                shippment.setIsDeleted(false);
                shippment.setDateCreated(LocalDateTime.now());
                shippment.setDateDelivered(LocalDateTime.now());
                shippment.setStatus(Status_Shipment.WAITING);
                shipmentRepository.save(shippment);

                // - sp
                Orders orders = payment.getOrder();
                Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
                cart.setStatus(Status_Cart.COMPLETED);





//                boolean status_assign = assignShipments(orders.getOrderId());
                assignAllPendingTasks();
                String note = "";

                Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {
                    note = "Hiện không thể giao hàng";
                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentRepository.save(shippment);
                }
//                if(!status_assign)
//                {
//                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
//                    shipmentRepository.save(shippment);
//                    note = "Hiện không thể giao hàng";
//                }

                response.put("status", 1);
                response.put("note",note);
            } else {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
                Orders order = payment.getOrder();
                order.setDateCanceled(LocalDateTime.now());
                order.setStatus(Status_Order.CANCELLED);
                Cart cart = order.getOrderItem().getCart();
                cart.setStatus(Status_Cart.COMPLETED);
                cartRepository.save(cart);
                Voucher voucher = order.getVoucher();
                if(voucher != null) {
                    UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), voucher.getVoucherId());
                    userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                    userVoucherRepository.save(userVoucher);
                }
                UserCoin userCoin = userCointRepository.findByUserUserId(order.getUser().getUserId());
                if(userCoin != null)
                {
                    float point_coint = order.getPointCoinUse() + userCoin.getPointCoin();
                    userCoin.setPointCoin(point_coint);
                    userCointRepository.save(userCoin);
                }
                List<CartItem> cartItems = cart.getCartItems();
                for(CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                }
                response.put("status", 0);
            }

        }
        else
        {
            payment.setStatus(Status_Payment.FAILED);
            paymentRepository.save(payment);
            Orders order = payment.getOrder();
            order.setDateCanceled(LocalDateTime.now());
            order.setStatus(Status_Order.CANCELLED);
            Cart cart = order.getOrderItem().getCart();
            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);
            Voucher voucher = order.getVoucher();
            if(voucher != null) {
                UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), voucher.getVoucherId());
                userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                userVoucherRepository.save(userVoucher);
            }
            UserCoin userCoin = userCointRepository.findByUserUserId(order.getUser().getUserId());
            if(userCoin != null)
            {
                float point_coint = order.getPointCoinUse() + userCoin.getPointCoin();
                userCoin.setPointCoin(point_coint);
                userCointRepository.save(userCoin);
            }
            List<CartItem> cartItems = cart.getCartItems();
            for(CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                productVariantsRepository.save(productVariants);
//                cartItem.setIsDeleted(true);
//                cartItem.setDateDeleted(LocalDateTime.now());
            }
            response.put("status", 0);
        }
        return ResponseEntity.ok().body(response);
    }

    @Transactional
    public ResponseEntity<?> handleCallBackGroup(String appTransId) {
        PaymentGroup payment = paymentGroupRepository.findByOrderIdPayment(appTransId);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        boolean isSuccess = checkStatusOrder(appTransId);
        System.out.println(isSuccess);
        if (isSuccess != false) {
            if (payment.getStatus() != Status_Payment.FAILED || payment.getStatus() != Status_Payment.COMPLETED) {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentGroupRepository.save(payment);
                GroupOrders groupOrders_update = payment.getGroupOrder();
                groupOrders_update.setStatus(StatusGroupOrder.COMPLETED);
                groupOrdersRepository.save(groupOrders_update);
                ShippmentGroup shippment = new ShippmentGroup();
                shippment.setPayment(payment);
                shippment.setIsDeleted(false);
                shippment.setDateCreated(LocalDateTime.now());
                shippment.setDateDelivered(LocalDateTime.now());
                shippment.setStatus(Status_Shipment.WAITING);
                shipmentGroupRepository.save(shippment);

                GroupOrders groupOrder = payment.getGroupOrder();
                List<GroupOrderMember> groupOrderMemberList1 = groupOrder.getGroupOrderMembers();

//                boolean status_assign = assignShipmentGroups(groupOrder.getGroupOrderId());
                String note = "";
                assignAllPendingTasks();
                ShippmentGroup shippment1 = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {
                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentGroupRepository.save(shippment);
                    note = "Hiện không thể giao hàng";
                }

                response.put("status", 1);
                response.put("note",note);
            } else {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);

                response.put("status", 0);
            }

        }
        else
        {
            payment.setStatus(Status_Payment.FAILED);
            paymentGroupRepository.save(payment);

            response.put("status", 0);
        }
        return ResponseEntity.ok().body(response);
    }

}
