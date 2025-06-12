package com.hmdrinks.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.Response.CRUDPaymentGroupResponse;
import com.hmdrinks.SupportFunction.DistanceAndDuration;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentGroupService {

    @Value("${api.user-service.url}")
    private String userServiceUrl;

    @Value("${api.group-order.url}")
    private String groupOrderUrl;
    //payos
    private static final String clientId = "eba81bf7-4dbf-4ec9-a7c2-d0e6e3cb1a72";
    private static final String apiKey = "e62918ef-78fb-4e98-b285-08d85f66c246";
    private static final String checksumKey = "67fce528f58ccebc972c2814853f993ea2d4d7e0c13336d81eb3d84df946d6c1";
    private static  final String webhookUrl_Web = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/web/payOs";
    private static  final String webhookUrl = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/payOs";

    //momo
    private final String accessKey = "F8BBA842ECF85";
    private final String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String partnerCode = "MOMO";
    private final String redirectUrl = "https://f0ec-116-108-42-64.ngrok-free.app/intermediary-page";
    private final String ipnUrl = "https://rightly-poetic-amoeba.ngrok-free.app/api/payment/callback";
    private final String requestType = "payWithMethod";
    private final String requestType1 = "onDelivery";
    private final boolean autoCapture = true;
    private final int orderExpireTime = 15;
    private final String lang = "vi";
    private String orderInfo = "Payment Order";


    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private  PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private  ShipmentGroupRepository shipmentGroupRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserVoucherRepository userVoucherRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private VNPayService vnPayService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private  UserCointRepository userCointRepository;
    @Autowired
    private  AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private  GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private CartItemGroupRepository cartItemGroupRepository;

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

    private static final double ALPHA = 1.0;
    private static final double BETA = 1.0;
    private static final double GAMMA = 1.0;
    private static final double DELTA = 1.0;

    private static final double[] STORE_ORIGIN = {10.850575879000075, 106.77190192800003};
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
    public boolean assignShipments(int groupOrderId) {
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






    public static double calculateFee(double distance) {
        if (distance < 1) {
            return 5000.0;
        } else if (distance >= 1 && distance < 5) {
            return 15000.0;
        } else if (distance >= 5 && distance < 10) {
            return 25000.0;
        } else if (distance >= 10 && distance < 15) {
            return 35000.0;
        } else if (distance >= 15 && distance <= 20) {
            return 50000.0;
        } else {
            return 0.0;
        }
    }
    
    @Transactional
    public ResponseEntity<?> createPaymentMomo(int orderGroupId,String type, Integer leaderId,Language language) {
        try {

            String callback_url = "";
            String redirect_url = "";
            if (Objects.equals(type, "WEB"))
            {
                callback_url = userServiceUrl + "/callback/android/group/momo";
                redirect_url = userServiceUrl + "/redirect/web/momo";
//                callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/momo";
//                redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/web/momo/redirect";
            }
            else
            {
                callback_url = userServiceUrl + "/callback/android/group/momo";
                redirect_url = userServiceUrl + "/redirect/android/group/momo";
//                callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/momo";
//                redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/android/group/momo";
            }

            GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(orderGroupId, leaderId);
            if (leader.getIsLeader() == Boolean.FALSE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
            }
            List<PaymentGroup> payment1 = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(orderGroupId);
            for(PaymentGroup payment2: payment1)
            {
                if (payment2 != null) {
                    if (payment2.getPaymentMethod() == Payment_Method.CASH && payment2.getStatus() == Status_Payment.PENDING) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                    }
                    if (payment2.getStatus() == Status_Payment.COMPLETED || payment2.getStatus() == Status_Payment.PENDING) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                    }
                }
            }
            GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(orderGroupId);
            if (groupOrder == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not found");
            }
            if(groupOrder.getStatus() != StatusGroupOrder.CHECKOUT)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not confirm");
            }
            LocalTime localTime = LocalTime.now();
            LocalTime groupTime = groupOrder.getDatePaymentTime().plusMinutes(20);
            if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
            {
                if (!localTime.isBefore(groupTime)) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("The group action time has expired and can no longer be performed.");
                }
            }


            System.out.println(groupOrder.getStatus());
            if (groupOrder.getStatus() != StatusGroupOrder.CHECKOUT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
            }

            String orderId = partnerCode + "-" + UUID.randomUUID();
            String requestId = partnerCode + "-" + UUID.randomUUID();


            Double Subtotal = 0.0;


            List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(orderGroupId);
            for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
                CartGroup cartGroup = groupOrderMember.getCartGroup();
                CRUDCartGroupResponse crudCartGroupResponse = null;

                if (cartGroup != null) {
                    List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                    List<CartItemGroup> cartItems = cartGroup.getCartItems();
                    if (cartItems != null) {
                        for (CartItemGroup cartItem : cartItems) {

                            Subtotal += cartItem.getTotalPrice();

                        }
                    }
             }

            }
            int discountPercent = 0;
            long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                    .filter(member -> !member.getIsDeleted())
                    .count();
            if (activeMemberCount >= 8) {
                discountPercent = 10;
            } else if (activeMemberCount >= 5) {
                discountPercent = 6;
            } else if (activeMemberCount >= 3) {
                discountPercent = 4;
            } else if (activeMemberCount >= 2) {
                discountPercent = 2;
            }
            Double deliveryFee = 0.0;
            String address = groupOrder.getAddress();
            if(address.isEmpty())
            {
                deliveryFee = 0.0;
            }
            String place_id = supportFunction.getLocation(address);
            double[] destinations= supportFunction.getCoordinates(place_id);
            double[] origins = {10.850575879000075,106.77190192800003};
            // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
            DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
            double distance = distanceAndDuration.getDistance();
            if(distance > 20){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
            }
            deliveryFee = calculateFee(distance);
            double discountAmount = Subtotal * discountPercent / 100.0;
            Double TotalPrice = Subtotal + deliveryFee - discountAmount;


            Long totalAmountLong = TotalPrice.longValue();
            String amount = totalAmountLong.toString();

            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey, amount, callback_url, orderId, orderInfo, partnerCode, redirect_url, requestId, requestType
            );

            String signature = hmacSHA256(secretKey, rawSignature);
            JSONObject userInfo = new JSONObject();
            userInfo.put("phoneNumber", groupOrder.getUser().getPhoneNumber());
            userInfo.put("email", groupOrder.getUser().getEmail());
            userInfo.put("name", groupOrder.getUser().getFullName());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("partnerName", "TEST");
            requestBody.put("storeId", "MomoTestStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirect_url);
            requestBody.put("ipnUrl", callback_url);
            requestBody.put("lang", lang);
            requestBody.put("requestType", requestType);
            requestBody.put("autoCapture", autoCapture);
            requestBody.put("extraData", "");
            requestBody.put("userInfo", userInfo);
            requestBody.put("signature", signature);
            requestBody.put("orderExpireTime", orderExpireTime);
            List<GroupOrderMember> listGroupOrderMember = groupOrder.getGroupOrderMembers();
            List<Map<String, Object>> items = new ArrayList<>();
            for (GroupOrderMember groupOrderMember : listGroupOrderMember) {
                CartGroup cartGroup = groupOrderMember.getCartGroup();
                if(cartGroup != null)
                {
                    List<CartItemGroup> cartItems = cartGroup.getCartItems();

                    for (CartItemGroup cartItem : cartItems) {
                        Map<String, Object> item = new HashMap<>();
                        String listProImg = cartItem.getProductVariants().getProduct().getListProImg();
                        String imageUrl = "";
                        if (listProImg != null && !listProImg.isEmpty()) {
                            String[] images = listProImg.split(", ");
                            imageUrl = images.length > 0 ? images[0] : "";
                        }

                        String firstImage = imageUrl.split(",")[0];
                        int colonIndex = firstImage.indexOf(":");
                        String imageUrl1 = (colonIndex != -1) ? firstImage.substring(colonIndex + 1).trim() : "";
                        Long totalPrice = Math.round(cartItem.getTotalPrice());
                        item.put("imageUrl", imageUrl1);
                        item.put("name", cartItem.getProductVariants().getProduct().getProName() + "- Size " + cartItem.getProductVariants().getSize());
                        item.put("unit", cartItem.getProductVariants().getSize());
                        item.put("quantity", cartItem.getQuantity());
                        item.put("price", totalPrice);
                        item.put("category", "beverage");
                        item.put("manufacturer", "HMDrinks");
                        items.add(item);
                    }
                }


            }

            Map<String, Object> itemFee = new HashMap<>();
            itemFee.put("name","Phí giao hàng");
            itemFee.put("price",Math.round(deliveryFee));
            itemFee.put("quantity", -1);
            itemFee.put("imageUrl","https://cdn.vectorstock.com/i/1000x1000/52/44/delivery-vector-30925244.webp");
            items.add(itemFee);


            requestBody.put("items", items);

            URL url = new URL("https://test-payment.momo.vn/v2/gateway/api/create");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            ObjectMapper mapper = new ObjectMapper();
            conn.getOutputStream().write(mapper.writeValueAsBytes(requestBody));

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            Map<String, Object> responseBody = mapper.readValue(response.toString(), HashMap.class);
            int statusCode = (Integer) responseBody.get("resultCode");
            String shortLink = (String) responseBody.get("payUrl");
            PaymentGroup payment = new PaymentGroup();
            if (statusCode == 0) {
                payment.setPaymentMethod(Payment_Method.CREDIT);
                payment.setStatus(Status_Payment.PENDING);
                payment.setGroupOrder(groupOrder);
                payment.setAmount(TotalPrice);
                payment.setDateCreated(LocalDateTime.now());
                payment.setOrderIdPayment(orderId);
                payment.setIsDeleted(false);
                payment.setIsRefund(false);
                payment.setLink(shortLink);
                paymentGroupRepository.save(payment);
            }
            return new ResponseEntity<>(new CreatePaymentResponse(
                    payment.getPaymentId(),
                    payment.getAmount(),
                    payment.getDateCreated(),
                    payment.getDateDeleted(),
                    payment.getDateRefunded(),
                    payment.getIsDeleted(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getGroupOrder().getGroupOrderId(),
                    shortLink,
                    ""
            ), HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = Map.of("statusCode", 500, "message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ResponseEntity<?> createPaymentATM(int orderGroupId,String type, Integer leaderId,Language language) {
        String webhook = "";

        if (Objects.equals(type, "WEB"))
        {
            webhook = userServiceUrl +  "/callback/web/payOs";
        }
        else
        {
            webhook = userServiceUrl + "/callback/android/group/payOs";
        }

        try {
            GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(orderGroupId);
            if (groupOrder == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not found");
            }
            LocalTime localTime = LocalTime.now();
            LocalTime groupTime = groupOrder.getDatePaymentTime().plusMinutes(20);
            if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
            {
                if (!localTime.isBefore(groupTime)) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("The group action time has expired and can no longer be performed.");
                }
            }
            GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(orderGroupId, leaderId);
            if (leader.getIsLeader() == Boolean.FALSE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
            }
            List<PaymentGroup> payment1 = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(orderGroupId);
            for(PaymentGroup payment2: payment1)
            {
                if (payment2 != null) {
                    if (payment2.getPaymentMethod() == Payment_Method.CASH && payment2.getStatus() == Status_Payment.PENDING) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                    }
                    if (payment2.getStatus() == Status_Payment.COMPLETED || payment2.getStatus() == Status_Payment.PENDING) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                    }
                }
            }



            if (groupOrder.getStatus() != StatusGroupOrder.CHECKOUT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
            }

            String orderId = partnerCode + "-" + UUID.randomUUID();
            String requestId = partnerCode + "-" + UUID.randomUUID();


            Double Subtotal = 0.0;


            List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(orderGroupId);
            for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
                CartGroup cartGroup = groupOrderMember.getCartGroup();
                CRUDCartGroupResponse crudCartGroupResponse = null;

                if (cartGroup != null) {
                    List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                    List<CartItemGroup> cartItems = cartGroup.getCartItems();
                    if (cartItems != null) {
                        for (CartItemGroup cartItem : cartItems) {

                            Subtotal += cartItem.getTotalPrice();

                        }
                    }
                }

            }
            int discountPercent = 0;
            long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                    .filter(member -> !member.getIsDeleted())
                    .count();
            if (activeMemberCount >= 8) {
                discountPercent = 10;
            } else if (activeMemberCount >= 5) {
                discountPercent = 6;
            } else if (activeMemberCount >= 3) {
                discountPercent = 4;
            } else if (activeMemberCount >= 2) {
                discountPercent = 2;
            }
            Double deliveryFee = 0.0;
            String address = groupOrder.getAddress();
            if(address.isEmpty())
            {
                deliveryFee = 0.0;
            }
            String place_id = supportFunction.getLocation(address);
            double[] destinations= supportFunction.getCoordinates(place_id);
            double[] origins = {10.850575879000075,106.77190192800003};
            // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
            DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
            double distance = distanceAndDuration.getDistance();
            if(distance > 20){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
            }
            deliveryFee = calculateFee(distance);
            double discountAmount = Subtotal * discountPercent / 100.0;
            Double TotalPrice = Subtotal + deliveryFee - discountAmount;

            int totalAmountLong = (int) TotalPrice.longValue();


            List<ItemData> items = new ArrayList<>();
            List<GroupOrderMember> groupOrderMemberList1 = groupOrder.getGroupOrderMembers();
            for(GroupOrderMember groupOrderMember: groupOrderMemberList1)
            {
                CartGroup cartGroup = groupOrderMember.getCartGroup();
                if(cartGroup != null) {
                    List<CartItemGroup> cartItems = cartGroup.getCartItems();
                    for (CartItemGroup cartItem : cartItems) {
                        ProductVariants productVariants = cartItem.getProductVariants();
                        ItemData itemData = ItemData.builder()
                                .name(productVariants.getProduct().getProName() + "-" + productVariants.getSize())
                                .quantity(cartItem.getQuantity())
                                .price((int) productVariants.getPrice())
                                .build();
                        items.add(itemData);
                    }
                }

            }


            PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
            Long orderCode = generateRandomOrderCode();
            ItemData itemData = ItemData.builder()
                    .name("Phí giao hàng")
                    .quantity(1)
                    .price((int) Double.parseDouble(String.valueOf(deliveryFee)))
                    .build();
            items.add(itemData);

            int discount = (int) Double.parseDouble(String.valueOf(discountAmount));
            ItemData itemData1 = ItemData.builder()
                    .name("Giảm giá")
                    .quantity(1)
                    .price(discount)
                    .build();
            items.add(itemData1);
            User user = groupOrder.getUser();
            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(totalAmountLong)
                    .description("Thanh toán đơn hàng")
                    .returnUrl(webhook)
                    .cancelUrl(webhook)
                    .buyerAddress(groupOrder.getAddress())
                    .buyerEmail(user.getEmail())
                    .buyerName(user.getPhoneNumber())
                    .buyerName(user.getFullName())
                    .expiredAt((long) (System.currentTimeMillis() / 1000 + 15 * 60))
                    .items(items).build();

            CheckoutResponseData result = payOS.createPaymentLink(paymentData);
            String link = result.getCheckoutUrl();
            PaymentGroup payment = new PaymentGroup();
            if (result.getStatus().equals("PENDING")) {
                payment.setPaymentMethod(Payment_Method.CREDIT);
                payment.setStatus(Status_Payment.PENDING);
                payment.setGroupOrder(groupOrder);
                payment.setAmount(TotalPrice);
                payment.setDateCreated(LocalDateTime.now());
                payment.setOrderIdPayment("PayOS" + orderCode);
                payment.setIsDeleted(false);
                payment.setIsRefund(false);
                payment.setLink(link);
                paymentGroupRepository.save(payment);
            }
            return new ResponseEntity<>(new CreatePaymentGroupResponse(
                    payment.getPaymentId(),
                    payment.getAmount(),
                    payment.getDateCreated(),
                    payment.getDateDeleted(),
                    payment.getDateRefunded(),
                    payment.getIsDeleted(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getGroupOrder().getGroupOrderId(),
                    link,
                    ""
            ), HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = Map.of("statusCode", 500, "message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static String generateUniqueNumericString(int length) {
        String digits = "123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        while (result.length() < length) {
            char c = digits.charAt(random.nextInt(digits.length()));
            if (result.indexOf(String.valueOf(c)) == -1) {
                result.append(c);
            }
        }
        return result.toString();
    }


    @Transactional
    public ResponseEntity<?> createVNPay(CreatePaymentVNPayGroupReq req)
    {
        String callback_url = "";
        String redirect_url = "";
        if (Objects.equals(req.getType(), "WEB"))
        {
            callback_url = userServiceUrl + "/callback/android/group/vnpay_ipn";
            redirect_url = userServiceUrl + "/redirect/web/vnpay";
//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/vnpay_ipn";
//            redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/web/vnpay";
        }
        else
        {
            callback_url = userServiceUrl + "/callback/android/group/vnpay_ipn";
            redirect_url = userServiceUrl + "/redirect/android/group/vnpay";

//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/group/vnpay_ipn";
//            redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/android/group/vnpay";
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime().plusMinutes(20);
        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("The group action time has expired and can no longer be performed.");
            }
        }
        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(req.getGroupOrderId(), req.getLeaderUserId());
        if (leader.getIsLeader() == Boolean.FALSE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }
        List<PaymentGroup> payment1 = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        for(PaymentGroup payment2: payment1)
        {
            if (payment2 != null) {
                if (payment2.getPaymentMethod() == Payment_Method.CASH && payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                }
                if (payment2.getStatus() == Status_Payment.COMPLETED || payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                }
            }
        }




        if (groupOrder.getStatus() != StatusGroupOrder.CHECKOUT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }


        String requestId = partnerCode + "-" + UUID.randomUUID();


        Double Subtotal = 0.0;


        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;

            if (cartGroup != null) {
                List<CartItemGroup> cartItems = cartGroup.getCartItems();
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        Subtotal += cartItem.getTotalPrice();

                    }
                }
            }

        }
        int discountPercent = 0;
        long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
        if (activeMemberCount >= 8) {
            discountPercent = 10;
        } else if (activeMemberCount >= 5) {
            discountPercent = 6;
        } else if (activeMemberCount >= 3) {
            discountPercent = 4;
        } else if (activeMemberCount >= 2) {
            discountPercent = 2;
        }
        Double deliveryFee = 0.0;
        String address = groupOrder.getAddress();
        if(address.isEmpty())
        {
            deliveryFee = 0.0;
        }
        String place_id = supportFunction.getLocation(address);
        double[] destinations= supportFunction.getCoordinates(place_id);
        double[] origins = {10.850575879000075,106.77190192800003};
        DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
        double distance = distanceAndDuration.getDistance();
        if(distance > 20){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
        }
        deliveryFee = calculateFee(distance);
        double discountAmount = Subtotal * discountPercent / 100.0;
        Double TotalPrice = Subtotal + deliveryFee - discountAmount;

        Long totalAmountLong = TotalPrice.longValue();
        String orderIdCode = partnerCode + "-" + UUID.randomUUID();
        PaymentGroup payment = new PaymentGroup();
        payment.setPaymentMethod(Payment_Method.CREDIT);
        payment.setStatus(Status_Payment.PENDING);
        payment.setGroupOrder(groupOrder);
        payment.setAmount(TotalPrice);
        payment.setDateCreated(LocalDateTime.now());
        payment.setOrderIdPayment(orderIdCode);
        payment.setIsDeleted(false);
        payment.setIsRefund(false);
        User user = groupOrder.getUser();
        paymentGroupRepository.save(payment);
        String order_id = generateUniqueNumericString(5);
        var initPaymentRequest = InitPaymentRequest.builder()
                .userId(Long.valueOf(String.valueOf(user.getUserId())))
                .amount(totalAmountLong)
                .txnRef(order_id)
                .requestId(orderIdCode)
                .ipAddress(req.getIpAddress())
                .build();
        payment.setOrderIdPayment(order_id);
        paymentGroupRepository.save(payment);
        var initPaymentResponse = vnPayService.init(initPaymentRequest,callback_url,redirect_url);
        payment.setLink(initPaymentResponse.getVnpUrl());
        paymentGroupRepository.save(payment);
        return new ResponseEntity<>(new CreatePaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                initPaymentResponse.getVnpUrl(),
                ""
        ), HttpStatus.OK);
    }
    @Autowired
    private ZaloPayService zaloPayService;

    @Transactional
    public ResponseEntity<?> createZaloPay(CreatePaymentGroupReq req) throws Exception {

        String webhook = "";
        if (Objects.equals(req.getType(), "WEB"))
        {
            webhook = webhookUrl_Web;
        }
        else
        {
            webhook = webhookUrl;
        }
        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(req.getGroupOrderId(),req.getLeaderUserId());
        if (leader.getIsLeader() == Boolean.FALSE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }
        List<PaymentGroup> payment1 = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        for(PaymentGroup payment2: payment1)
        {
            if (payment2 != null) {
                if (payment2.getPaymentMethod() == Payment_Method.CASH && payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                }
                if (payment2.getStatus() == Status_Payment.COMPLETED || payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                }
            }
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime().plusMinutes(20);
        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("The group action time has expired and can no longer be performed.");
            }
        }

        if (groupOrder.getStatus() != StatusGroupOrder.CHECKOUT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }




        Double Subtotal = 0.0;


        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(req.getGroupOrderId());
        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;

            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartGroup.getCartItems();
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {

                        Subtotal += cartItem.getTotalPrice();

                    }
                }
            }

        }
        int discountPercent = 0;
        long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
        if (activeMemberCount >= 8) {
            discountPercent = 10;
        } else if (activeMemberCount >= 5) {
            discountPercent = 6;
        } else if (activeMemberCount >= 3) {
            discountPercent = 4;
        } else if (activeMemberCount >= 2) {
            discountPercent = 2;
        }
        Double deliveryFee = 0.0;
        String address = groupOrder.getAddress();
        if(address.isEmpty())
        {
            deliveryFee = 0.0;
        }
        String place_id = supportFunction.getLocation(address);
        double[] destinations= supportFunction.getCoordinates(place_id);
        double[] origins = {10.850575879000075,106.77190192800003};
        // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
        DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
        double distance = distanceAndDuration.getDistance();
        if(distance > 20){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
        }
        deliveryFee = calculateFee(distance);
        double discountAmount = Subtotal * discountPercent / 100.0;
        Double TotalPrice = Subtotal + deliveryFee - discountAmount;

        Long totalAmountLong = TotalPrice.longValue();
        String orderId = partnerCode + "-" + UUID.randomUUID();
        PaymentGroup payment = new PaymentGroup();
        payment.setPaymentMethod(Payment_Method.CREDIT);
        payment.setStatus(Status_Payment.PENDING);
        payment.setGroupOrder(groupOrder);
        payment.setAmount(TotalPrice);
        payment.setDateCreated(LocalDateTime.now());
        payment.setOrderIdPayment(orderId);
        payment.setIsDeleted(false);
        payment.setIsRefund(false);

        paymentGroupRepository.save(payment);
        Map<String, Object> response = zaloPayService.createPaymentGroup(totalAmountLong,req.getType());
        String orderUrl = (String) response.get("order_url");
        String appTransId = (String) response.get("app_trans_id");
        payment.setLink(orderUrl);
        payment.setOrderIdPayment(appTransId);
        paymentGroupRepository.save(payment);
        return new ResponseEntity<>(new CreatePaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                orderUrl,
                ""
        ), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> callBack(String resultCode, String orderId) {
        PaymentGroup payment = paymentGroupRepository.findByOrderIdPayment(orderId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Not found payment"));
        }
        if (payment.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Payment is deleted"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", payment.getPaymentId());

        if ("0".equals(resultCode)) {
            payment.setStatus(Status_Payment.COMPLETED);
            paymentGroupRepository.save(payment);
            GroupOrders groupOrders_update = payment.getGroupOrder();
            groupOrders_update.setStatus(StatusGroupOrder.COMPLETED);
            groupOrdersRepository.save(groupOrders_update);


            GroupOrders groupOrders = payment.getGroupOrder();
            List<GroupOrderMember> groupOrderMember = groupOrders.getGroupOrderMembers();

            ShippmentGroup shippment = new ShippmentGroup();
            shippment.setPayment(payment);
            shippment.setIsDeleted(false);
            shippment.setDateCreated(LocalDateTime.now());
            shippment.setDateDelivered(LocalDateTime.now());
            shippment.setStatus(Status_Shipment.WAITING);
            shipmentGroupRepository.save(shippment);
            assignAllPendingTasks();
            ShippmentGroup shippment1 = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
            if(shippment1.getUser() == null)
            {
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                shipmentGroupRepository.save(shippment);
            }

            response.put("status", HttpStatus.OK.value());
            response.put("message", "Payment completed successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            payment.setStatus(Status_Payment.FAILED);
            paymentGroupRepository.save(payment);
            response.put("status", HttpStatus.BAD_REQUEST.value());
            response.put("message", "Payment failed");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }


    @Transactional
    public ResponseEntity<?> getInformationPayOs(int paymentId) throws Exception {
        PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
        PaymentGroup payment = paymentGroupRepository.findByPaymentId(paymentId);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }
        String orderCode = payment.getOrderIdPayment();
        String extractedCode = orderCode.replace("PayOS", "");
        PaymentLinkData paymentLinkData = payOS.getPaymentLinkInformation(Long.valueOf(extractedCode));
        String status = paymentLinkData.getStatus();
        switch (status) {
            case "EXPIRED", "CANCELLED" -> {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);

            }
            case "PAID" -> {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentGroupRepository.save(payment);
            }
            default -> {
                payment.setStatus(Status_Payment.PENDING);
                paymentGroupRepository.save(payment);
            }
        }
        CRUDPaymentGroupResponse crudPaymentResponse = new CRUDPaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        );
        return ResponseEntity.status(HttpStatus.OK).body(crudPaymentResponse);
    }


    public ResponseEntity<?> checkStatusPayment(int paymentId) {
        PaymentGroup payment = paymentGroupRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if (payment == null) {
            return new ResponseEntity<>("Not found payment", HttpStatus.NOT_FOUND);
        }
        if(payment.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment is deleted");
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        ));
    }


    @Transactional
    public ResponseEntity<?> createPaymentCash(Integer orderGroupId, Integer leaderId, Language language) {
        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(orderGroupId, leaderId);
        if (leader.getIsLeader() == Boolean.FALSE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }
        List<PaymentGroup> payment1 = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(orderGroupId);
        for(PaymentGroup payment2: payment1)
        {
            if (payment2 != null) {
                if (payment2.getPaymentMethod() == Payment_Method.CASH && payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                }
                if (payment2.getStatus() == Status_Payment.COMPLETED || payment2.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                }
            }
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(orderGroupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("GroupOrder not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime().plusMinutes(20);
        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("The group action time has expired and can no longer be performed.");
            }
        }


        if (groupOrder.getStatus() != StatusGroupOrder.CHECKOUT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }

        Double Subtotal = 0.0;
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(orderGroupId);
        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;

            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartGroup.getCartItems();
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {

                        Subtotal += cartItem.getTotalPrice();

                    }
                }
            }

        }
        int discountPercent = 0;
        long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
        if (activeMemberCount >= 8) {
            discountPercent = 10;
        } else if (activeMemberCount >= 5) {
            discountPercent = 6;
        } else if (activeMemberCount >= 3) {
            discountPercent = 4;
        } else if (activeMemberCount >= 2) {
            discountPercent = 2;
        }
        Double deliveryFee = 0.0;
        String address = groupOrder.getAddress();
        if(address.isEmpty())
        {
            deliveryFee = 0.0;
        }
        String place_id = supportFunction.getLocation(address);
        double[] destinations= supportFunction.getCoordinates(place_id);
        double[] origins = {10.850575879000075,106.77190192800003};
        // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
        DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
        double distance = distanceAndDuration.getDistance();
        if(distance > 20){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
        }
        deliveryFee = calculateFee(distance);
        double discountAmount = Subtotal * discountPercent / 100.0;
        Double TotalPrice = Subtotal + deliveryFee - discountAmount;

        PaymentGroup payment12 = new PaymentGroup();
        payment12.setAmount(TotalPrice);
        payment12.setPaymentMethod(Payment_Method.CASH);
        payment12.setStatus(Status_Payment.PENDING);
        payment12.setDateCreated(LocalDateTime.now());
        payment12.setLink("");
        payment12.setIsDeleted(false);
        payment12.setGroupOrder(groupOrder);
        payment12.setIsRefund(false);
        paymentGroupRepository.save(payment12);

        ShippmentGroup shippment = new ShippmentGroup();
        shippment.setPayment(payment12);
        shippment.setIsDeleted(false);
        shippment.setDateCreated(LocalDateTime.now());
        shippment.setDateDelivered(LocalDateTime.now());
        shippment.setStatus(Status_Shipment.WAITING);

        shipmentGroupRepository.save(shippment);

        List<GroupOrderMember> groupOrderMemberList1 = groupOrder.getGroupOrderMembers();



        assignAllPendingTasks();
        ShippmentGroup shippment1 = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
        if(shippment1.getUser() == null)
            {
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                shipmentGroupRepository.save(shippment);
            }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentGroupResponse(
                payment12.getPaymentId(),
                payment12.getAmount(),
                payment12.getDateCreated(),
                payment12.getDateDeleted(),
                payment12.getDateRefunded(),
                payment12.getIsDeleted(),
                payment12.getPaymentMethod(),
                payment12.getStatus(),
                payment12.getGroupOrder().getGroupOrderId(),
                payment12.getIsRefund(),
                payment12.getLink()

        ));
    }

//    public ResponseEntity<?> getAllPayment(String pageFromParam, String limitFromParam) {
//        // Parse page and limit parameters
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Integer.parseInt(limitFromParam);
//        if (limit >= 100) limit = 100;
//
//        Pageable pageable = PageRequest.of(page - 1, limit);
//
//        // Get all payments with pagination
//        Page<Payment> payments = paymentRepository.findAllByIsDeletedFalse(pageable);
//
//        // Prepare responses list
//        List<CRUDPaymentResponse> responses = new ArrayList<>(payments.getContent().size());
//
//        // Get all orders once and map them for faster access
//        Set<Integer> orderIds = payments.getContent().stream()
//                .map(payment -> payment.getOrder().getOrderId())
//                .collect(Collectors.toSet());
//        Map<Integer, Orders> orderMap = orderRepository.findAllById(orderIds).stream()
//                .collect(Collectors.toMap(Orders::getOrderId, Function.identity()));
//
//        // Loop through payments and map to DTOs
//        for (Payment payment : payments) {
//            Orders order = orderMap.get(payment.getOrder().getOrderId()); // Get order from preloaded map
//
//            CRUDPaymentResponse response = new CRUDPaymentResponse(
//                    payment.getPaymentId(),
//                    payment.getAmount(),
//                    payment.getDateCreated(),
//                    payment.getDateDeleted(),
//                    payment.getDateRefunded(),
//                    payment.getIsDeleted(),
//                    payment.getPaymentMethod(),
//                    payment.getStatus(),
//                    order != null ? order.getOrderId() : null, // Avoid potential NPE
//                    payment.getIsRefund(),
//                    payment.getLink()
//            );
//            responses.add(response);
//        }
//
//        // Return paginated response
//        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
//                page,
//                payments.getTotalPages(),
//                limit,
//                (int) payments.getTotalElements(),
//                responses
//        ));
//    }
//
//
//    public ResponseEntity<?> getAllPaymentStatus(String pageFromParam, String limitFromParam, Status_Payment statusPayment) {
//        // Parse page and limit parameters
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Integer.parseInt(limitFromParam);
//        if (limit >= 100) limit = 100;
//
//        Pageable pageable = PageRequest.of(page - 1, limit);
//
//        // Get payments with the specified status and not deleted, using pagination
//        Page<Payment> payments = paymentRepository.findAllByStatusAndIsDeletedFalse(statusPayment, pageable);
//
//        // Preload all orders for the payments in a single query
//        Set<Integer> orderIds = payments.getContent().stream()
//                .map(payment -> payment.getOrder().getOrderId())
//                .collect(Collectors.toSet());
//        Map<Integer, Orders> orderMap = orderRepository.findAllById(orderIds).stream()
//                .collect(Collectors.toMap(Orders::getOrderId, Function.identity()));
//
//        // Prepare the responses list
//        List<CRUDPaymentResponse> responses = new ArrayList<>(payments.getContent().size());
//
//        // Loop through payments and map to DTOs
//        for (Payment payment : payments) {
//            Orders order = orderMap.get(payment.getOrder().getOrderId()); // Retrieve order from preloaded map
//
//            CRUDPaymentResponse response = new CRUDPaymentResponse(
//                    payment.getPaymentId(),
//                    payment.getAmount(),
//                    payment.getDateCreated(),
//                    payment.getDateDeleted(),
//                    payment.getDateRefunded(),
//                    payment.getIsDeleted(),
//                    payment.getPaymentMethod(),
//                    payment.getStatus(),
//                    order != null ? order.getOrderId() : null, // Avoid potential NPE
//                    payment.getIsRefund(),
//                    payment.getLink()
//            );
//            responses.add(response);
//        }
//
//        // Return paginated response
//        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
//                page,
//                payments.getTotalPages(),
//                limit,
//                (int) payments.getTotalElements(),  // Use total elements for accurate total count
//                responses
//        ));
//    }
//
//    public ResponseEntity<?> getAllPaymentMethod(String pageFromParam, String limitFromParam, Payment_Method paymentMethod) {
//        // Parse page and limit parameters
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Integer.parseInt(limitFromParam);
//        if (limit >= 100) limit = 100;
//
//        Pageable pageable = PageRequest.of(page - 1, limit);
//
//        // Get payments with the specified payment method and not deleted, using pagination
//        Page<Payment> payments = paymentRepository.findAllByPaymentMethodAndIsDeletedFalse(paymentMethod, pageable);
//
//        // Preload all orders for the payments in a single query
//        Set<Integer> orderIds = payments.getContent().stream()
//                .map(payment -> payment.getOrder().getOrderId())
//                .collect(Collectors.toSet());
//        Map<Integer, Orders> orderMap = orderRepository.findAllById(orderIds).stream()
//                .collect(Collectors.toMap(Orders::getOrderId, Function.identity()));
//
//        // Prepare the responses list
//        List<CRUDPaymentResponse> responses = new ArrayList<>(payments.getContent().size());
//
//        // Loop through payments and map to DTOs
//        for (Payment payment : payments) {
//            Orders order = orderMap.get(payment.getOrder().getOrderId()); // Retrieve order from preloaded map
//
//            CRUDPaymentResponse response = new CRUDPaymentResponse(
//                    payment.getPaymentId(),
//                    payment.getAmount(),
//                    payment.getDateCreated(),
//                    payment.getDateDeleted(),
//                    payment.getDateRefunded(),
//                    payment.getIsDeleted(),
//                    payment.getPaymentMethod(),
//                    payment.getStatus(),
//                    order != null ? order.getOrderId() : null, // Avoid potential NPE
//                    payment.getIsRefund(),
//                    payment.getLink()
//            );
//            responses.add(response);
//        }
//
//        // Return paginated response
//        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
//                page,
//                payments.getTotalPages(),
//                limit,
//                (int) payments.getTotalElements(),  // Use total elements for accurate total count
//                responses
//        ));
//    }


    public ResponseEntity<?> getOnePayment(int paymentId){
        PaymentGroup payment = paymentGroupRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        ));
    }

    @Transactional
    public ResponseEntity<?> handleCallBackPayOS(int orderCode) throws Exception {
        PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
        List<PaymentGroup> payments = paymentGroupRepository.findAll();
        String orderCode1="";
        for (PaymentGroup payment : payments) {
            String equal = "PayOS" + orderCode;
            System.out.println("PayOS" + orderCode);
            if(equal.equals(payment.getOrderIdPayment()))
            {
                orderCode1 = payment.getOrderIdPayment();
            }
        }

        if(orderCode1.equals(""))
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found orderCode");
        }
        PaymentGroup payment = paymentGroupRepository.findByOrderIdPayment(orderCode1);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }
        GroupOrders groupOrder = payment.getGroupOrder();
        String extractedCode = orderCode1.replace("PayOS", "");
        PaymentLinkData paymentLinkData = payOS.getPaymentLinkInformation(Long.valueOf(extractedCode));
        String status = paymentLinkData.getStatus();
        switch (status) {
            case "EXPIRED", "CANCELLED" -> {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);

            }
            case "PAID" -> {
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

                List<GroupOrderMember> groupOrderMemberList1 = groupOrder.getGroupOrderMembers();

                assignAllPendingTasks();
                ShippmentGroup shippment1 = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {
                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentGroupRepository.save(shippment);
                }

            }
            default -> {
                payment.setStatus(Status_Payment.PENDING);
                paymentGroupRepository.save(payment);
            }
        }
        CRUDPaymentGroupResponse crudPaymentResponse = new CRUDPaymentGroupResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGroupOrder().getGroupOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        );
        return ResponseEntity.status(HttpStatus.OK).body(crudPaymentResponse);
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hmacData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    @Transactional
//    public  ResponseEntity<?> listAllPaymentRefund(String pageFromParam, String limitFromParam){
//       int page = Integer.parseInt(pageFromParam);
//       int limit = Integer.parseInt(limitFromParam);
//       if (limit >= 100) limit = 100;
//       Pageable pageable = PageRequest.of(page - 1, limit);
//       Page<Payment> payments = paymentRepository.findAllByStatusAndIsDeletedFalse(Status_Payment.REFUND, pageable);
//       List<Payment> payments1 = paymentRepository.findAllByStatusAndIsDeletedFalse(Status_Payment.REFUND);
//       List<CRUDPaymentResponse> responses = new ArrayList<>();
//       for (Payment payment : payments) {
//           responses.add(
//                   new CRUDPaymentResponse(
//                           payment.getPaymentId(),
//                           payment.getAmount(),
//                           payment.getDateCreated(),
//                           payment.getDateDeleted(),
//                           payment.getDateRefunded(),
//                           payment.getIsDeleted(),
//                           payment.getPaymentMethod(),
//                           payment.getStatus(),
//                           payment.getOrder().getOrderId(),
//                           payment.getIsRefund(),
//                           payment.getLink()
//                   )
//           );
//       }
//       return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
//               page,
//               payments.getTotalPages(),
//               limit,
//               payments1.size(),
//               responses
//       ));
//   }
//
//    @Transactional
//    public ResponseEntity<?> activateRefund(int paymentId)
//    {
//       Payment payment = paymentRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
//       if(payment == null)
//       {
//           return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
//       }
//       Shippment shippment = payment.getShipment();
//
//       if(shippment.getStatus() == Status_Shipment.WAITING ||shippment.getStatus() == Status_Shipment.SHIPPING)
//       {
//           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not refund with shipment");
//       }
//       if(payment.getPaymentMethod() == Payment_Method.CASH)
//        {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not refund with payment method cash");
//        }
//       if(payment.getIsRefund())
//       {
//           return ResponseEntity.status(HttpStatus.CONFLICT).body("Refund exists");
//       }
//       payment.setIsRefund(true);
//       payment.setDateRefunded(LocalDateTime.now());
//       paymentRepository.save(payment);
//       return ResponseEntity.status(HttpStatus.OK).body("Refund activated");
//   }


    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void checkTimePaymentGroupLocal(){
        List<PaymentGroup> paymentList = paymentGroupRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CREDIT);
        List<PaymentGroup> paymentList1 = paymentGroupRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CASH);
        for(PaymentGroup paymentGroup: paymentList1)
        {
            if(paymentGroup.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            ShippmentGroup shippmentGroup = paymentGroup.getShipment();
            if(shippmentGroup == null)
            {
                paymentGroup.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(paymentGroup);
                if(paymentGroup.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
                {
                    GroupOrders groupOrders = paymentGroup.getGroupOrder();
                    groupOrders.setStatus(StatusGroupOrder.CANCELED);
                    groupOrdersRepository.save(groupOrders);

                }
            }
            else
            {
                ShippmentGroup shippmentGroup1 = paymentGroup.getShipment();
                Duration timeoutDuration = Duration.ofMinutes(30);
                if (shippmentGroup1.getDateDelivered().plus(timeoutDuration).isBefore(LocalDateTime.now())) {
                    paymentGroup.setStatus(Status_Payment.FAILED);
                    paymentGroupRepository.save(paymentGroup);
                    if(paymentGroup.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
                    {
                        GroupOrders groupOrders = paymentGroup.getGroupOrder();
                        groupOrders.setStatus(StatusGroupOrder.CANCELED);
                        groupOrdersRepository.save(groupOrders);

                    }

                }
            }
        }
        for(PaymentGroup payment : paymentList)
        {

            if(payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            String orderIdPayment = payment.getOrderIdPayment();
            LocalDateTime paymentCreatedTime = payment.getDateCreated();
            Duration timeoutDuration = Duration.ofMinutes(30);

            if (orderIdPayment != null && orderIdPayment.toUpperCase().contains("MOMO")) {
                timeoutDuration = Duration.ofMinutes(100);
            }

            if (paymentCreatedTime.plus(timeoutDuration).isBefore(LocalDateTime.now())) {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);
            }

            if(payment.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
            {
                GroupOrders groupOrders = payment.getGroupOrder();
                groupOrders.setStatus(StatusGroupOrder.CANCELED);
                groupOrdersRepository.save(groupOrders);

            }
        }




    }

    @Transactional
    public ResponseEntity<?> checkTimePaymentGroup(){
        List<PaymentGroup> paymentList = paymentGroupRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CREDIT);
        List<PaymentGroup> paymentList1 = paymentGroupRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CASH);
        for(PaymentGroup paymentGroup: paymentList1)
        {
            if(paymentGroup.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            ShippmentGroup shippmentGroup = paymentGroup.getShipment();
            if(shippmentGroup == null)
            {
                paymentGroup.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(paymentGroup);
                if(paymentGroup.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
                {
                    GroupOrders groupOrders = paymentGroup.getGroupOrder();
                    groupOrders.setStatus(StatusGroupOrder.CANCELED);
                    groupOrdersRepository.save(groupOrders);

                }
            }
            else
            {
                ShippmentGroup shippmentGroup1 = paymentGroup.getShipment();
                Duration timeoutDuration = Duration.ofMinutes(30);
                if (shippmentGroup1.getDateDelivered().plus(timeoutDuration).isBefore(LocalDateTime.now())) {
                    paymentGroup.setStatus(Status_Payment.FAILED);
                    paymentGroupRepository.save(paymentGroup);
                    if(paymentGroup.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
                    {
                        GroupOrders groupOrders = paymentGroup.getGroupOrder();
                        groupOrders.setStatus(StatusGroupOrder.CANCELED);
                        groupOrdersRepository.save(groupOrders);




                    }

                }
            }
        }
        for(PaymentGroup payment : paymentList)
        {

            if(payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            String orderIdPayment = payment.getOrderIdPayment();
            LocalDateTime paymentCreatedTime = payment.getDateCreated();
            Duration timeoutDuration = Duration.ofMinutes(30);

            if (orderIdPayment != null && orderIdPayment.toUpperCase().contains("MOMO")) {
                timeoutDuration = Duration.ofMinutes(100);
            }

            if (paymentCreatedTime.plus(timeoutDuration).isBefore(LocalDateTime.now())) {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);
            }

            if(payment.getGroupOrder().getTypeTime() == Status_Type_Time_Group.TIME)
            {
                GroupOrders groupOrders = payment.getGroupOrder();
                groupOrders.setStatus(StatusGroupOrder.CANCELED);
                groupOrdersRepository.save(groupOrders);

            }
        }

           return  ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> listAllGroupPaymentRefund(String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "dateCreated"));
        Page<PaymentGroup> paymentGroups = paymentGroupRepository.findAllByStatusAndIsDeletedFalse(Status_Payment.REFUND, pageable);
        List<PaymentGroup> allRefunds = paymentGroupRepository.findAllByStatusAndIsDeletedFalse(Status_Payment.REFUND);

        List<CRUDPaymentGroupResponse> responses = paymentGroups.stream()
                .map(payment -> new CRUDPaymentGroupResponse(
                        payment.getPaymentId(),
                        payment.getAmount(),
                        payment.getDateCreated(),
                        payment.getDateDeleted(),
                        payment.getDateRefunded(),
                        payment.getIsDeleted(),
                        payment.getPaymentMethod(),
                        payment.getStatus(),
                        payment.getGroupOrder().getGroupOrderId(),
                        payment.getIsRefund(),
                        payment.getLink()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(
                new ListAllPaymentGroupResponse(
                        page,
                        paymentGroups.getTotalPages(),
                        limit,
                        allRefunds.size(),
                        responses
                )
        );
    }


    public static class NormalizedOrder {
        private String orderId;
        private String type;

        public NormalizedOrder(String orderId, String type) {
            this.orderId = orderId;
            this.type = type;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getType() {
            return type;
        }
    }

    public NormalizedOrder normalizeOrderIdAndDetectType(String rawOrderId) {
        if (rawOrderId == null || rawOrderId.isEmpty()) return new NormalizedOrder("", "vnpay");

        // Trường hợp 1: ZaloPay dạng "241121_591407 32795"
        if (rawOrderId.matches("^\\d{6}_\\d+$")) {
            return new NormalizedOrder(rawOrderId, "zalopay");
        }

        // Trường hợp 2: MOMO
        if (rawOrderId.startsWith("MOMO-")) {
            String[] parts = rawOrderId.split("MOMO-");
            if (parts.length > 1) {
                return new NormalizedOrder(parts[1], "momo");
            }
        }

        // Trường hợp 3: PayOS bỏ
        if (rawOrderId.startsWith("PayOS")) {
            return new NormalizedOrder("", "payos");
        }


        return new NormalizedOrder(rawOrderId, "vnpay");
    }

    @Transactional
    public ResponseEntity<?> activateRefundGroup(int paymentId, String token) {
        PaymentGroup payment = paymentGroupRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment group");
        }

        ShippmentGroup shipment = payment.getShipment();
        if (shipment != null &&
                (shipment.getStatus() == Status_Shipment.WAITING || shipment.getStatus() == Status_Shipment.SHIPPING)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot refund: shipment in transit or waiting");
        }

        if (payment.getPaymentMethod() == Payment_Method.CASH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot refund for cash payment method");
        }

        if (Boolean.TRUE.equals(payment.getIsRefund())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Refund already activated");
        }

        NormalizedOrder normalized = normalizeOrderIdAndDetectType(payment.getOrderIdPayment());
        String orderId = normalized.getOrderId();
        String type = normalized.getType();

        if (orderId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order ID not valid for refund");
        }

        // ✅ Chỉ hỗ trợ các loại ví sau
        List<String> supportedTypes = Arrays.asList("zalopay", "momo", "vnpay");
        if (!supportedTypes.contains(type)) {
            payment.setIsRefund(true);
            paymentGroupRepository.save(payment);
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Success " + type);
        }

        String url = userServiceUrl + "/refund-payment/refund";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        System.out.println(token);
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("code", orderId);
        requestBody.put("type", type);
        System.out.println(requestBody);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                payment.setIsRefund(true);
                paymentGroupRepository.save(payment);
                return ResponseEntity.ok("Refund success: " + response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Refund failed: " + response.getBody());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refund error: " + e.getMessage());
        }


    }



}
