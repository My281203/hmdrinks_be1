package com.hmdrinks.Service;
import java.math.BigDecimal;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreatePaymentReq;
import com.hmdrinks.Request.CreatePaymentVNPayReq;
import com.hmdrinks.Request.InitPaymentRequest;
import com.hmdrinks.Response.CRUDPaymentResponse;
import com.hmdrinks.Response.CreatePaymentResponse;
import com.hmdrinks.Response.ListAllPaymentResponse;
import com.hmdrinks.SupportFunction.DistanceAndDuration;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.annotation.PostConstruct;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Value("${api.user-service.url}")
    private String userServiceUrl;

    @Value("${api.group-order.url}")
    private String groupOrderUrl;

    private final WebhookConfig webhookConfig;

    @Autowired
    public PaymentService(WebhookConfig webhookConfig) {
        this.webhookConfig = webhookConfig;
    }

    private static String webhookUrl_Web1;
    private static String webhookUrl_Android1;

    @PostConstruct
    public void init() {
        webhookUrl_Web1 = userServiceUrl + "/callback/web/payOs";
        webhookUrl_Android1 = userServiceUrl + "/callback/android/payOs";
    }
    //payos
    private static final String clientId = "eba81bf7-4dbf-4ec9-a7c2-d0e6e3cb1a72";
    private static final String apiKey = "e62918ef-78fb-4e98-b285-08d85f66c246";
    private static final String checksumKey = "67fce528f58ccebc972c2814853f993ea2d4d7e0c13336d81eb3d84df946d6c1";

//    private static  final String webhookUrl_Web = webhookUrl_Web1;
//    private static  final String webhookUrl = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/payOs";

    //momo
    private final String accessKey = "F8BBA842ECF85";
    private final String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String partnerCode = "MOMO";
    private final String redirectUrl = "https://f0ec-116-108-42-64.ngrok-free.app/intermediary-page";
//    private final String ipnUrl = "https://rightly-poetic-amoeba.ngrok-free.app/api/payment/callback";
    private final String requestType = "payWithMethod";
    private final String requestType1 = "onDelivery";
    private final boolean autoCapture = true;
    private final int orderExpireTime = 15;
    private final String lang = "vi";
    private String orderInfo = "Payment Order";

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
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
    private PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private ShipmentGroupRepository shipmentGroupRepository;
    @Autowired
    private GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private  CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private  GroupOrderMembersRepository groupOrderMembersRepository;


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




    private PaymentService.ShipperSelectionResult selectBestShipper(Shippment shipment, ShippmentGroup group, List<User> avail, double[] dest, LocalDate today) {
        for (User u : avail) {
            if (groupable(u, shipment, group, today)) {
                double distance = calculateCostInfo(u, dest).getDistance(); // lấy distance từ hàm chung
                return new PaymentService.ShipperSelectionResult(u, distance);
            }
        }

        return avail.stream()
                .map(u -> calculateCostInfo(u, dest))
                .min(Comparator.comparingDouble(ShipperCost::getCost))
                .map(sc -> new PaymentService.ShipperSelectionResult(sc.getUser(), sc.getDistance()))
                .orElse(null);
    }



    private ShipperCost calculateCostInfo(User u, double[] dest) {
        double[] origin = getShipperOrigin(u);
        DistanceAndDuration dd = supportFunction.getShortestDistance(origin, dest);
        double tts = parseDurationToMinutes(dd.getDuration());
        int count = u.getShippments().size() + u.getShippmentGroups().size();
        double tot = calculateTotalTravelTime(u);
        double cost = ALPHA * tts + BETA * count + GAMMA * tot + DELTA * dd.getDistance();
        return new PaymentService.ShipperCost(u, cost, dd.getDistance());
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

//    public static LocalDateTime addDurationToCurrentTime(String duration, LocalDateTime currentTime) {
//        int hours = 0;
//        int minutes = 0;
//
//
//        // Lấy thời gian hiện tại
//        LocalDateTime now = LocalDateTime.now();
//
//        if (currentTime.isBefore(now)) {
//            return now.plusMinutes(20);
//        }
//
//        if (duration.contains("giờ")) {
//            String[] parts = duration.split("giờ");
//            hours = Integer.parseInt(parts[0].trim()); // Lấy số giờ
//            if (parts.length > 1 && parts[1].contains("phút")) {
//                minutes = Integer.parseInt(parts[1].replace("phút", "").trim()) + 25;
//            }
//        } else if (duration.contains("phút")) {
//            minutes = Integer.parseInt(duration.replace("phút", "").trim()) + 25;
//        }
//
//        // Nếu không có giờ hoặc phút, thêm mặc định 25 phút
//        if (hours == 0 && minutes == 0) {
//            minutes += 25;
//        }
//
//        // Xử lý trường hợp số phút lớn hơn 60
//        if (minutes >= 60) {
//            hours += minutes / 60;
//            minutes = minutes % 60;
//        }
//
//        return currentTime.plusHours(hours).plusMinutes(minutes);
//    }
//
//
//    @Transactional
//    public boolean assignShipments(int orderId) {
//        List<Shippment> pendingShipments = shipmentRepository.findByStatus(Status_Shipment.WAITING)
//                .stream()
//                .sorted(Comparator.comparing(Shippment::getDateCreated))
//                .collect(Collectors.toList());
//
//        Orders orders = orderRepository.findByOrderId(orderId);
//        String placeId = supportFunction.getLocation(orders.getAddress());
//        double[] destination = supportFunction.getCoordinates(placeId);
//        double[] lastOrigin = new double[0];
//        double[] origin = {10.850575879000075, 106.77190192800003}; // Số 1-3 Võ Văn Ngân, Linh Chiểu, Thủ Đức, Tp HCM
//
//        List<User> allShippers = userRepository.findAllByRole(Role.SHIPPER);
//        LocalDate today = LocalDate.now();
//
//        List<User> shippers = allShippers.stream()
//                .filter(shipper -> {
//                    List<AbsenceRequest> approvedRequests = absenceRequestRepository
//                            .findByUserUserIdAndStatus(shipper.getUserId(), LeaveStatus.APPROVED);
//
//                    // Nếu không có đơn nghỉ nào nằm trong hôm nay → shipper có thể làm việc
//                    return approvedRequests.stream().noneMatch(request ->
//                            !request.getStartDate().toLocalDate().isAfter(today) &&   // startDate <= today
//                                    !request.getEndDate().toLocalDate().isBefore(today)      // endDate >= today
//                    );
//                })
//                .collect(Collectors.toList());
//
//
//        double distance_result = 0;
//        for (Shippment shipment : pendingShipments) {
//            User selectedShipper = null;
//            LocalDateTime currentTime = LocalDateTime.now();
//            LocalDateTime now = currentTime;
//
//            for (User shipper : shippers.stream()
//                    .sorted(Comparator.comparingInt(shipper -> shipper.getShippments().size())) // Sắp xếp shipper theo số đơn
//                    .collect(Collectors.toList())) {
//
//
//                List<Shippment> recentShipments = shipper.getShippments().stream()
//                        .filter(s -> s.getDateDelivered() != null &&
//                                Duration.between(s.getDateDelivered(), now).toHours() < 1)
//                        .collect(Collectors.toList());
//
//                if (recentShipments.size() >= 5) {
//                    continue;
//                }
//
//                double[] lastDestination = origin;
//
//                if (!recentShipments.isEmpty()) {
//
//                    Shippment lastShipment = recentShipments.get(recentShipments.size() - 1);
//                    lastDestination = supportFunction.getCoordinates(
//                            supportFunction.getLocation(lastShipment.getPayment().getOrder().getAddress()));
//
//                    DistanceAndDuration distance = supportFunction.getShortestDistance(lastDestination, destination);
//                    if (distance.getDistance() > 5) {
//                        continue;
//                    }
//
//                    DistanceAndDuration lastToCurrent = supportFunction.getShortestDistance(lastDestination, destination);
//                    String duration = lastToCurrent.getDuration();
//                    currentTime = addDurationToCurrentTime(duration, lastShipment.getDateDelivered());
//                    lastOrigin = lastDestination;
//                    distance_result = distance.getDistance();
//
//                } else {
//                    DistanceAndDuration originToDestination = supportFunction.getShortestDistance(origin, destination);
//                    if (originToDestination.getDistance() > 20) {
//                        continue;
//                    }
//
//                    String duration = originToDestination.getDuration();
//                    currentTime = addDurationToCurrentTime(duration, currentTime);
//                    lastOrigin = origin;
//                    distance_result = originToDestination.getDistance();
//                }
//
//                selectedShipper = shipper;
//                break;
//            }
//
//            if (selectedShipper == null) {
//                currentTime = null;
//                return false;
//            }
//            shipment.setUser(selectedShipper);
//            shipment.setStatus(Status_Shipment.SHIPPING);
//            shipment.setDateDelivered(currentTime);
//            shipment.setDistance(distance_result);
//            shipmentRepository.save(shipment);
//            selectedShipper.getShippments().add(shipment);
//            supportFunction.getDirections(lastOrigin,destination,shipment.getShipmentId());
//            // Gửi thông báo đến shipper
//            try {
//                String message = "Bạn có đơn hàng mới cần giao";
//                Integer userId = shipment.getUser().getUserId();
//
//                Integer shipmentId = shipment.getShipmentId();
//                notificationService.sendNotification(userId, shipmentId, message);
//            } catch (Exception e) {
//                System.err.println("Failed to send notification to shipper. Error: " + e.getMessage());
//            }
//        }
//        return true;
//    }

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
            if(shipment.getUser() != null)
            {
                continue;
            }
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
                        baseTime = lastShipment.getDateShip().plusMinutes(20);
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
    public ResponseEntity<?> createPaymentMomo(int orderId1,String type) {
        try {

            String callback_url = "";
            String redirect_url = "";
            if (Objects.equals(type, "WEB"))
            {
                callback_url = userServiceUrl + "/callback/android/momo";
                redirect_url = userServiceUrl + "/redirect/web/momo";
//                callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/";
//                redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/";
//                redirect_url = "https://1a2f-14-187-80-86.ngrok-free.app/intermediary-page";
            }
            else
            {
                System.out.println(type);
                callback_url = userServiceUrl + "/callback/android/momo";
                redirect_url = userServiceUrl + "/redirect/android/momo";
//                callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/momo";
//                redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/android/momo";
            }
            Payment payment1 = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId1);
            if (payment1 != null) {
                if (payment1.getPaymentMethod() == Payment_Method.CASH && payment1.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                }
                if (payment1.getStatus() == Status_Payment.COMPLETED || payment1.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                }
            }
            Orders orders = orderRepository.findByOrderIdAndStatusAndIsDeletedFalse(orderId1, Status_Order.CONFIRMED);
            if (orders == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
            }
            Orders orders1 = orderRepository.findByOrderId(orderId1);
            if (orders1.getStatus() == Status_Order.CANCELLED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is cancelled");
            }
            String orderId = partnerCode + "-" + UUID.randomUUID();
            String requestId = partnerCode + "-" + UUID.randomUUID();
            Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId1);
            User user = userRepository.findByUserIdAndIsDeletedFalse(order.getUser().getUserId());
            if(user == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            Double totalAmount = order.getTotalPrice() - order.getDiscountPrice() -order.getPointCoinUse() + order.getDeliveryFee();
            if(totalAmount <= 0.0)
            {
                    Payment payment = new Payment();
                    payment.setPaymentMethod(Payment_Method.CREDIT);
                    payment.setStatus(Status_Payment.COMPLETED);
                    payment.setOrder(order);
                    payment.setAmount(0.0);
                    payment.setDateCreated(LocalDateTime.now());
                    payment.setOrderIdPayment(orderId);
                    payment.setIsDeleted(false);
                    payment.setIsRefund(false);
                    paymentRepository.save(payment);

                Shippment shippment = new Shippment();
                shippment.setPayment(payment);
                shippment.setIsDeleted(false);
                shippment.setDateCreated(LocalDateTime.now());
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(25));
                shippment.setStatus(Status_Shipment.WAITING);
                shipmentRepository.save(shippment);

                Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
                List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

                for (CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    if (productVariants.getStock() >= cartItem.getQuantity()) {
                        productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
                        productVariantsRepository.save(productVariants);
                        if(productVariants.getStock() == 0)
                        {
                            List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
                            for (CartItem cartItemList1 : cartItemList) {
                                Cart cart1 = cartItemList1.getCart();
                                if(cart1.getStatus() == Status_Cart.NEW)
                                {
                                    cartItemList1.setQuantity(0);
                                    cartItemList1.setNote("Hiện đang hết hàng");
                                    cartItemList1.setTotalPrice(0.0);
                                    cartItemRepository.save(cartItemList1);
                                }
                                List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
                                double total = 0.0;
                                int total_quantity = 0;
                                for (CartItem cartItemList3 : cartItemList2) {
                                    total +=  cartItemList3.getTotalPrice();
                                    total_quantity += cartItemList3.getQuantity();
                                }
                                cart1.setTotalPrice(total);
                                cart1.setTotalProduct(total_quantity);
                                cartRepository.save(cart1);
                            }}
                    }
                }
                assignAllPendingTasks();
                String note = "";
                Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {
                    note = "Hiện không thể giao hàng";
                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentRepository.save(shippment);
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
                        payment.getOrder().getOrderId(),
                        "",
                        note
                ), HttpStatus.OK);

            }
            Long totalAmountLong = totalAmount.longValue();
            String amount = totalAmountLong.toString();
            if (order.getDiscountPrice() > 0) {
                orderInfo = "Giảm giá: " + order.getDiscountPrice() + " VND";
            }
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey, amount, callback_url, orderId, orderInfo, partnerCode, redirect_url, requestId, requestType
            );

            String signature = hmacSHA256(secretKey, rawSignature);
            JSONObject userInfo = new JSONObject();
            userInfo.put("phoneNumber", user.getPhoneNumber());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getFullName());

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
            List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(order.getOrderItem().getCart().getCartId());
            List<Map<String, Object>> items = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
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
            Map<String, Object> itemFee = new HashMap<>();
            itemFee.put("name","Phí giao hàng");
            itemFee.put("price",Math.round(order.getDeliveryFee()));
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
            Payment payment = new Payment();
            if (statusCode == 0) {
                payment.setPaymentMethod(Payment_Method.CREDIT);
                payment.setStatus(Status_Payment.PENDING);
                payment.setOrder(order);
                payment.setAmount(totalAmount);
                payment.setDateCreated(LocalDateTime.now());
                payment.setOrderIdPayment(orderId);
                payment.setIsDeleted(false);
                payment.setIsRefund(false);
                payment.setLink(shortLink);
                paymentRepository.save(payment);
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
                    payment.getOrder().getOrderId(),
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
    public ResponseEntity<?> createPaymentATM(int orderId1, String type) {
        String webhook = "";
        if (Objects.equals(type, "WEB"))
        {
            webhook = userServiceUrl +  "/callback/web/payOs";
        }
        else
        {
            webhook = userServiceUrl + "/callback/android/payOs";
        }

        try {
            Payment payment1 = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId1);
            if (payment1 != null) {
                if (payment1.getPaymentMethod() == Payment_Method.CASH && payment1.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
                }
                if (payment1.getStatus() == Status_Payment.COMPLETED || payment1.getStatus() == Status_Payment.PENDING) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
                }
            }
            Orders orders = orderRepository.findByOrderIdAndStatusAndIsDeletedFalse(orderId1, Status_Order.CONFIRMED);
            if (orders == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
            }
            Orders orders1 = orderRepository.findByOrderId(orderId1);
            if (orders1.getStatus() == Status_Order.CANCELLED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is cancelled");
            }


            Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId1);
            User user = userRepository.findByUserIdAndIsDeletedFalse(order.getUser().getUserId());
            if(user == null)
            {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
            }
            Double totalAmount = order.getTotalPrice() - order.getDiscountPrice() - order.getPointCoinUse() + order.getDeliveryFee();
            if(totalAmount <= 0.0)
            {
                Payment payment = new Payment();
                payment.setPaymentMethod(Payment_Method.CREDIT);
                payment.setStatus(Status_Payment.COMPLETED);
                payment.setOrder(order);
                payment.setAmount(0.0);
                payment.setDateCreated(LocalDateTime.now());
                payment.setOrderIdPayment("None");
                payment.setIsDeleted(false);
                payment.setIsRefund(false);
                paymentRepository.save(payment);

                Shippment shippment = new Shippment();
                shippment.setPayment(payment);
                shippment.setIsDeleted(false);
                shippment.setDateCreated(LocalDateTime.now());
                shippment.setDateDelivered(LocalDateTime.now());
                shippment.setStatus(Status_Shipment.WAITING);
                shipmentRepository.save(shippment);

                Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
                List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

                for (CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    if (productVariants.getStock() >= cartItem.getQuantity()) {
                        productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
                        productVariantsRepository.save(productVariants);
                        if(productVariants.getStock() == 0)
                        {
                            List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
                            for (CartItem cartItemList1 : cartItemList) {
                                Cart cart1 = cartItemList1.getCart();
                                if(cart1.getStatus() == Status_Cart.NEW)
                                {
                                    cartItemList1.setQuantity(0);
                                    cartItemList1.setNote("Hiện đang hết hàng");
                                    cartItemList1.setTotalPrice(0.0);
                                    cartItemRepository.save(cartItemList1);
                                }
                                List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
                                double total = 0.0;
                                int total_quantity = 0;
                                for (CartItem cartItemList3 : cartItemList2) {
                                    total +=  cartItemList3.getTotalPrice();
                                    total_quantity += cartItemList3.getQuantity();
                                }
                                cart1.setTotalPrice(total);
                                cart1.setTotalProduct(total_quantity);
                                cartRepository.save(cart1);
                            }}
                    }
                }
                assignAllPendingTasks();
                String note = "";

                Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {
                    note = "Hiện không thể giao hàng";
                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentRepository.save(shippment);
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
                        payment.getOrder().getOrderId(),
                        "",
                        note
                ), HttpStatus.OK);

            }
            int totalAmountLong = (int) totalAmount.longValue();
            OrderItem orderItem = order.getOrderItem();
            List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(orderItem.getCart().getCartId());
            List<ItemData> items = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                ItemData itemData = ItemData.builder()
                        .name(productVariants.getProduct().getProName() + "-" + productVariants.getSize())
                        .quantity(cartItem.getQuantity())
                        .price((int) productVariants.getPrice())
                        .build();
                items.add(itemData);
            }
            int deliveryFee = (int) order.getDeliveryFee();
            PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
            Long orderCode = generateRandomOrderCode();
            ItemData itemData = ItemData.builder()
                    .name("Phí giao hàng")
                    .quantity(1)
                    .price(deliveryFee)
                    .build();
            items.add(itemData);

            int discount = (int) order.getDiscountPrice();
            ItemData itemData1 = ItemData.builder()
                    .name("Giảm giá")
                    .quantity(1)
                    .price(discount)
                    .build();
            items.add(itemData1);

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(totalAmountLong)
                    .description("Thanh toán đơn hàng")
                    .returnUrl(webhook)
                    .cancelUrl(webhook)
                    .buyerAddress(order.getAddress())
                    .buyerEmail(user.getEmail())
                    .buyerName(user.getPhoneNumber())
                    .buyerName(user.getFullName())
                    .expiredAt((long) (System.currentTimeMillis() / 1000 + 15 * 60))
                    .items(items).build();

            CheckoutResponseData result = payOS.createPaymentLink(paymentData);
            String link = result.getCheckoutUrl();
            Payment payment = new Payment();
            if (result.getStatus().equals("PENDING")) {
                payment.setPaymentMethod(Payment_Method.CREDIT);
                payment.setStatus(Status_Payment.PENDING);
                payment.setOrder(order);
                payment.setAmount(totalAmount);
                payment.setDateCreated(LocalDateTime.now());
                payment.setOrderIdPayment("PayOS" + orderCode);
                payment.setIsDeleted(false);
                payment.setIsRefund(false);
                payment.setLink(link);
                paymentRepository.save(payment);
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
                    payment.getOrder().getOrderId(),
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
    public ResponseEntity<?> createVNPay(CreatePaymentVNPayReq req)
    {
        String callback_url = "";
        String redirect_url = "";
        if (Objects.equals(req.getType(), "WEB"))
        {
            callback_url = userServiceUrl + "/callback/android/vnpay_ipn";
            redirect_url = userServiceUrl + "/redirect/web/vnpay";
//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/vnpay_ipn";
//            redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/web/vnpay";
        }
        else
        {

            callback_url = userServiceUrl + "/callback/android/vnpay_ipn";
            redirect_url = userServiceUrl + "/redirect/android/vnpay";
//            callback_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/callback/android/vnpay_ipn";
//            redirect_url = "https://living-accurately-mackerel.ngrok-free.app/api/v1/redirect/android/vnpay";
        }
        int orderId1 = req.getOrderId();
        Payment payment1 = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId1);

        if (payment1 != null) {
            if (payment1.getPaymentMethod() == Payment_Method.CASH && payment1.getStatus() == Status_Payment.PENDING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment cash already create");
            }
            if (payment1.getStatus() == Status_Payment.COMPLETED || payment1.getStatus() == Status_Payment.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
            }
        }
        Orders orders = orderRepository.findByOrderIdAndStatusAndIsDeletedFalse(orderId1, Status_Order.CONFIRMED);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }
        Orders orders1 = orderRepository.findByOrderId(orderId1);
        if (orders1.getStatus() == Status_Order.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is cancelled");
        }
        Orders order = orderRepository.findByOrderId(orderId1);
        User user = userRepository.findByUserId(order.getUser().getUserId());
        Double totalAmount = order.getTotalPrice() - order.getDiscountPrice() -order.getPointCoinUse() + order.getDeliveryFee();
        if(totalAmount <= 0.0)
        {
            Payment payment = new Payment();
            payment.setPaymentMethod(Payment_Method.CREDIT);
            payment.setStatus(Status_Payment.COMPLETED);
            payment.setOrder(order);
            payment.setAmount(0.0);
            payment.setDateCreated(LocalDateTime.now());
            payment.setOrderIdPayment("None");
            payment.setIsDeleted(false);
            payment.setIsRefund(false);
            paymentRepository.save(payment);

            Shippment shippment = new Shippment();
            shippment.setPayment(payment);
            shippment.setIsDeleted(false);
            shippment.setDateCreated(LocalDateTime.now());
            shippment.setDateDelivered(LocalDateTime.now());
            shippment.setStatus(Status_Shipment.WAITING);
            shipmentRepository.save(shippment);

            Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
            List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

            for (CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                if (productVariants.getStock() >= cartItem.getQuantity()) {
                    productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                    if(productVariants.getStock() == 0)
                    {
                        List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
                        for (CartItem cartItemList1 : cartItemList) {
                            Cart cart1 = cartItemList1.getCart();
                            if(cart1.getStatus() == Status_Cart.NEW)
                            {
                                cartItemList1.setQuantity(0);
                                cartItemList1.setNote("Hiện đang hết hàng");
                                cartItemList1.setTotalPrice(0.0);
                                cartItemRepository.save(cartItemList1);
                            }
                            List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
                            double total = 0.0;
                            int total_quantity = 0;
                            for (CartItem cartItemList3 : cartItemList2) {
                                total +=  cartItemList3.getTotalPrice();
                                total_quantity += cartItemList3.getQuantity();
                            }
                            cart1.setTotalPrice(total);
                            cart1.setTotalProduct(total_quantity);
                            cartRepository.save(cart1);
                        }}
                }
            }
            assignAllPendingTasks();
            String note = "";

            Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
            if(shippment1.getUser() == null)
            {
                note = "Hiện không thể giao hàng";
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                shipmentRepository.save(shippment);
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
                    payment.getOrder().getOrderId(),
                    "",
                    note
            ), HttpStatus.OK);

        }
        Long totalAmountLong = totalAmount.longValue();
        String orderId = partnerCode + "-" + UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentMethod(Payment_Method.CREDIT);
        payment.setStatus(Status_Payment.PENDING);
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setDateCreated(LocalDateTime.now());
        payment.setOrderIdPayment(orderId);
        payment.setIsDeleted(false);
        payment.setIsRefund(false);
        paymentRepository.save(payment);
        String order_id = generateUniqueNumericString(5);
        var initPaymentRequest = InitPaymentRequest.builder()
                .userId(Long.valueOf(String.valueOf(user.getUserId())))
                .amount(totalAmountLong)
                .txnRef(order_id)
                .requestId(orderId)
                .ipAddress(req.getIpAddress())
                .build();
        payment.setOrderIdPayment(order_id);
        paymentRepository.save(payment);
        var initPaymentResponse = vnPayService.init(initPaymentRequest,callback_url,redirect_url);
        payment.setLink(initPaymentResponse.getVnpUrl());
        paymentRepository.save(payment);
        return new ResponseEntity<>(new CreatePaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
                initPaymentResponse.getVnpUrl(),
                ""
        ), HttpStatus.OK);
    }
    @Autowired
    private ZaloPayService zaloPayService;

    @Transactional
    public ResponseEntity<?> createZaloPay(CreatePaymentReq req) throws Exception {
        int orderId1 = req.getOrderId();
        String webhook = "";
        if (Objects.equals(req.getType(), "WEB"))
        {
            webhook = userServiceUrl +  "/callback/web/payOs";
        }
        else
        {
            webhook = userServiceUrl + "/callback/android/payOs";
        }
        Payment payment1 = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId1);
        if (payment1 != null) {
            if (payment1.getPaymentMethod() == Payment_Method.CASH && payment1.getStatus() == Status_Payment.PENDING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type cash");
            }
            if (payment1.getStatus() == Status_Payment.COMPLETED || payment1.getStatus() == Status_Payment.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment already exists");
            }
        }
        Orders orders = orderRepository.findByOrderIdAndStatusAndIsDeletedFalse(orderId1, Status_Order.CONFIRMED);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }
        Orders orders1 = orderRepository.findByOrderId(orderId1);
        if (orders1.getStatus() == Status_Order.WAITING || orders1.getStatus() == Status_Order.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order cancelled");
        }
        Orders order = orderRepository.findByOrderId(orderId1);
        Double totalAmount = order.getTotalPrice() - order.getDiscountPrice() -order.getPointCoinUse()+ order.getDeliveryFee();
        if(totalAmount <= 0.0)
        {
            Payment payment = new Payment();
            payment.setPaymentMethod(Payment_Method.CREDIT);
            payment.setStatus(Status_Payment.COMPLETED);
            payment.setOrder(order);
            payment.setAmount(0.0);
            payment.setDateCreated(LocalDateTime.now());
            payment.setOrderIdPayment("None");
            payment.setIsDeleted(false);
            payment.setIsRefund(false);
            paymentRepository.save(payment);

            Shippment shippment = new Shippment();
            shippment.setPayment(payment);
            shippment.setIsDeleted(false);
            shippment.setDateCreated(LocalDateTime.now());
            shippment.setDateDelivered(LocalDateTime.now());
            shippment.setStatus(Status_Shipment.WAITING);
            shipmentRepository.save(shippment);

            Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
            List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

            for (CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                if (productVariants.getStock() >= cartItem.getQuantity()) {
                    productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                    if(productVariants.getStock() == 0)
                    {
                        List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
                        for (CartItem cartItemList1 : cartItemList) {
                            Cart cart1 = cartItemList1.getCart();
                            if(cart1.getStatus() == Status_Cart.NEW)
                            {
                                cartItemList1.setQuantity(0);
                                cartItemList1.setNote("Hiện đang hết hàng");
                                cartItemList1.setTotalPrice(0.0);
                                cartItemRepository.save(cartItemList1);
                            }
                            List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
                            double total = 0.0;
                            int total_quantity = 0;
                            for (CartItem cartItemList3 : cartItemList2) {
                                total +=  cartItemList3.getTotalPrice();
                                total_quantity += cartItemList3.getQuantity();
                            }
                            cart1.setTotalPrice(total);
                            cart1.setTotalProduct(total_quantity);
                            cartRepository.save(cart1);
                        }}
                }
            }
            assignAllPendingTasks();
            String note = "";

            Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
            if(shippment1.getUser() == null)
            {
                note = "Hiện không thể giao hàng";
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                shipmentRepository.save(shippment);
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
                    payment.getOrder().getOrderId(),
                    "",
                    note
            ), HttpStatus.OK);

        }
        Long totalAmountLong = totalAmount.longValue();
        String orderId = partnerCode + "-" + UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentMethod(Payment_Method.CREDIT);
        payment.setStatus(Status_Payment.PENDING);
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setDateCreated(LocalDateTime.now());
        payment.setOrderIdPayment(orderId);
        payment.setIsDeleted(false);
        payment.setIsRefund(false);

        paymentRepository.save(payment);
        Map<String, Object> response = zaloPayService.createPayment(totalAmountLong,req.getType());
        String orderUrl = (String) response.get("order_url");
        String appTransId = (String) response.get("app_trans_id");
        payment.setLink(orderUrl);
        payment.setOrderIdPayment(appTransId);
        paymentRepository.save(payment);
        return new ResponseEntity<>(new CreatePaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
                orderUrl,
                ""
        ), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> callBack(String resultCode, String orderId) {
        Payment payment = paymentRepository.findByOrderIdPayment(orderId);


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
            paymentRepository.save(payment);

            Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
            Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);

            Shippment shippment = new Shippment();
            shippment.setPayment(payment);
            shippment.setIsDeleted(false);
            shippment.setDateCreated(LocalDateTime.now());
            shippment.setDateDelivered(LocalDateTime.now());
            shippment.setStatus(Status_Shipment.WAITING);

            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);
            shipmentRepository.save(shippment);
            assignAllPendingTasks();

            Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
            if(shippment1.getUser() == null)
            {
                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                shipmentRepository.save(shippment);
            }
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Payment completed successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            payment.setStatus(Status_Payment.FAILED);
            paymentRepository.save(payment);
            Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
            orders.setStatus(Status_Order.CANCELLED);
            orders.setDateCanceled(LocalDateTime.now());
            Cart cart = orders.getOrderItem().getCart();
            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);
            orderRepository.save(orders);
            Voucher voucher = orders.getVoucher();
            if(voucher != null) {
                UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(orders.getUser().getUserId(), voucher.getVoucherId());
                userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                userVoucherRepository.save(userVoucher);
            }
            UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
            if(userCoin != null)
            {
                float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                userCoin.setPointCoin(point_coint);
                userCointRepository.save(userCoin);
            }
            List<CartItem> cartItems = cart.getCartItems();
            for(CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                productVariantsRepository.save(productVariants);
            }

//            OrderItem orderItem1 = orders.getOrderItem();
//            if (orderItem1 != null) {
//                orderItemRepository.delete(orderItem1);
//                Cart cart = cartRepository.findByCartId(orderItem1.getCart().getCartId());
//                cart.setStatus(Status_Cart.NEW);
//                cartRepository.save(cart);
//            }

            response.put("status", HttpStatus.BAD_REQUEST.value());
            response.put("message", "Payment failed");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public ResponseEntity<?> getInformationPayOs(int paymentId) throws Exception {
        PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
        Payment payment = paymentRepository.findByPaymentId(paymentId);
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
                paymentRepository.save(payment);
                UserCoin userCoin = userCointRepository.findByUserUserId(payment.getOrder().getUser().getUserId());
                if(userCoin != null)
                {
                    float point_coint = payment.getOrder().getPointCoinUse() + userCoin.getPointCoin();
                    userCoin.setPointCoin(point_coint);
                    userCointRepository.save(userCoin);
                }
            }
            case "PAID" -> {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentRepository.save(payment);
            }
            default -> {
                payment.setStatus(Status_Payment.PENDING);
                paymentRepository.save(payment);
            }
        }
        CRUDPaymentResponse crudPaymentResponse = new CRUDPaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        );
        return ResponseEntity.status(HttpStatus.OK).body(crudPaymentResponse);
    }


    public ResponseEntity<?> checkStatusPayment(int paymentId) {
        Payment payment = paymentRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if (payment == null) {
            return new ResponseEntity<>("Not found payment", HttpStatus.NOT_FOUND);
        }
        if(payment.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment is deleted");
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        ));
    }

//    @Transactional
//    public ResponseEntity<?> createPaymentCash(int orderId) {
//        Payment payment = paymentRepository.findByOrderOrderId(orderId);
//        if (payment != null) {
//            if(payment.getPaymentMethod() == Payment_Method.CASH)
//            {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment already exists");
//            }
//            if (payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.PENDING) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment create with type credit");
//            }
//            if (payment.getStatus() == Status_Payment.COMPLETED) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment already completed");
//            }
//        }
//        Orders orders = orderRepository.findByOrderIdAndStatus(orderId, Status_Order.CONFIRMED);
//        if (orders == null) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
//        }
//        Orders orders1 = orderRepository.findByOrderId(orderId);
//        if (orders1.getStatus() == Status_Order.CANCELLED) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is cancelled");
//        }
//        Orders order = orderRepository.findByOrderId(orderId);
//
//        User user = userRepository.findByUserId(order.getUser().getUserId());
//        if(user.getIsDeleted())
//        {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is deleted");
//        }
//        Double totalAmount = order.getTotalPrice() - order.getDiscountPrice() -order.getPointCoinUse() + order.getDeliveryFee();
//        if(totalAmount <= 0.0)
//        {
//            Payment payments = new Payment();
//            payments.setPaymentMethod(Payment_Method.CASH);
//            payments.setStatus(Status_Payment.COMPLETED);
//            payments.setOrder(order);
//            payments.setAmount(0.0);
//            payments.setDateCreated(LocalDateTime.now());
//            payments.setOrderIdPayment("None");
//            payments.setIsDeleted(false);
//            payments.setIsRefund(false);
//            paymentRepository.save(payments);
//
//            Shippment shippment = new Shippment();
//            shippment.setPayment(payments);
//            shippment.setIsDeleted(false);
//            shippment.setDateCreated(LocalDateTime.now());
//            shippment.setDateDelivered(LocalDateTime.now());
//            shippment.setStatus(Status_Shipment.WAITING);
//            shipmentRepository.save(shippment);
//
//            Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
//            cart.setStatus(Status_Cart.COMPLETED);
//            cartRepository.save(cart);
//            assignAllPendingTasks();
//            String note = "";
//            Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
//            if(shippment1.getUser() == null)
//            {
//                note = "Hiện không thể giao hàng";
//                shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
//                shipmentRepository.save(shippment);
//            }
//
//
//            return new ResponseEntity<>(new CreatePaymentResponse(
//                    payments.getPaymentId(),
//                    payments.getAmount(),
//                    payments.getDateCreated(),
//                    payments.getDateDeleted(),
//                    payment.getDateRefunded(),
//                    payments.getIsDeleted(),
//                    payments.getPaymentMethod(),
//                    payments.getStatus(),
//                    payments.getOrder().getOrderId(),
//                    "",
//                    note
//            ), HttpStatus.OK);
//
//        }
//        Payment payment1 = new Payment();
//        payment1.setAmount(totalAmount);
//        payment1.setPaymentMethod(Payment_Method.CASH);
//        payment1.setStatus(Status_Payment.PENDING);
//        payment1.setDateCreated(LocalDateTime.now());
//        payment1.setLink("");
//        payment1.setIsDeleted(false);
//        payment1.setOrder(order);
//        payment1.setIsRefund(false);
//        paymentRepository.save(payment1);
//
//        Shippment shippment = new Shippment();
//        shippment.setPayment(payment1);
//        shippment.setIsDeleted(false);
//        shippment.setDateCreated(LocalDateTime.now());
//        shippment.setDateDelivered(LocalDateTime.now());
//        shippment.setStatus(Status_Shipment.WAITING);
//
//        shipmentRepository.save(shippment);
//        Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
//        cart.setStatus(Status_Cart.COMPLETED);
//        cartRepository.save(cart);
//        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
//
//        for (CartItem cartItem : cartItems) {
//            ProductVariants productVariants = cartItem.getProductVariants();
//            if (productVariants.getStock() >= cartItem.getQuantity()) {
//                productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
//                productVariantsRepository.save(productVariants);
//                if(productVariants.getStock() == 0)
//                {
//                    List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
//                    for (CartItem cartItemList1 : cartItemList) {
//                        Cart cart1 = cartItemList1.getCart();
//                        if(cart1.getStatus() == Status_Cart.NEW)
//                        {
//                            cartItemList1.setQuantity(0);
//                            cartItemList1.setNote("Hiện đang hết hàng");
//                            cartItemList1.setTotalPrice(0.0);
//                            cartItemRepository.save(cartItemList1);
//                        }
//                        List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
//                        double total = 0.0;
//                        int total_quantity = 0;
//                        for (CartItem cartItemList3 : cartItemList2) {
//                            total +=  cartItemList3.getTotalPrice();
//                            total_quantity += cartItemList3.getQuantity();
//                        }
//                        cart1.setTotalPrice(total);
//                        cart1.setTotalProduct(total_quantity);
//                        cartRepository.save(cart1);
//                    }}
//            }
//        }
//        assignAllPendingTasks();
//        Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
//        if(shippment1.getUser() == null)
//        {
//            shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
//            shipmentRepository.save(shippment);
//        }
////        if(!status)
////        {
////            shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
////            shipmentRepository.save(shippment);
////        }
//        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentResponse(
//                payment1.getPaymentId(),
//                payment1.getAmount(),
//                payment1.getDateCreated(),
//                payment1.getDateDeleted(),
//                payment1.getDateRefunded(),
//                payment1.getIsDeleted(),
//                payment1.getPaymentMethod(),
//                payment1.getStatus(),
//                payment1.getOrder().getOrderId(),
//                payment1.getIsRefund(),
//                payment1.getLink()
//
//        ));
//    }

    private void processStockAndCartItems(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
        if (cartItems.isEmpty()) return;

        // Chuẩn bị map variantId -> CartItem
        Map<Integer, List<CartItem>> itemsByVariantId = cartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getProductVariants().getVarId()));

        // Tập hợp variant cần cập nhật
        List<ProductVariants> variantsToUpdate = new ArrayList<>();
        Set<Integer> outOfStockVariantIds = new HashSet<>();

        for (Map.Entry<Integer, List<CartItem>> entry : itemsByVariantId.entrySet()) {
            ProductVariants variant = entry.getValue().get(0).getProductVariants(); // 1 cart item đại diện
            int totalQuantity = entry.getValue().stream().mapToInt(CartItem::getQuantity).sum();

            if (variant.getStock() >= totalQuantity) {
                variant.setStock(variant.getStock() - totalQuantity);
                variantsToUpdate.add(variant);

                if (variant.getStock() == 0) {
                    outOfStockVariantIds.add(variant.getVarId());
                }
            }
        }

        productVariantsRepository.saveAll(variantsToUpdate);

        if (!outOfStockVariantIds.isEmpty()) {
            // Lấy tất cả cart items liên quan đến variant đã hết hàng
            List<CartItem> affectedCartItems = cartItemRepository.findByProductVariants_VarIdIn(new ArrayList<>(outOfStockVariantIds));

            // Gom nhóm theo cartId
            Map<Integer, List<CartItem>> cartItemsGroupedByCart = affectedCartItems.stream()
                    .collect(Collectors.groupingBy(ci -> ci.getCart().getCartId()));

            List<CartItem> itemsToUpdate = new ArrayList<>();
            List<Cart> cartsToUpdate = new ArrayList<>();

            for (Map.Entry<Integer, List<CartItem>> entry : cartItemsGroupedByCart.entrySet()) {
                Integer cartId = entry.getKey();
                Cart affectedCart = entry.getValue().get(0).getCart();

                if (affectedCart.getStatus() == Status_Cart.NEW) {
                    for (CartItem item : entry.getValue()) {
                        item.setQuantity(0);
                        item.setNote("Hiện đang hết hàng");
                        item.setTotalPrice(0.0);
                        itemsToUpdate.add(item);
                    }

                    // Lấy lại tất cả cartItem active trong cart này để tính lại tổng
                    List<CartItem> remainingItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartId);
                    double totalPrice = remainingItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
                    int totalProduct = remainingItems.stream().mapToInt(CartItem::getQuantity).sum();

                    affectedCart.setTotalPrice(totalPrice);
                    affectedCart.setTotalProduct(totalProduct);
                    cartsToUpdate.add(affectedCart);
                }
            }

            cartItemRepository.saveAll(itemsToUpdate);
            cartRepository.saveAll(cartsToUpdate);
        }
    }



//    private void processStockAndCartItems(Cart cart) {
//        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
//        List<ProductVariants> updatedVariants = new ArrayList<>();
//        Map<Long, List<CartItem>> outOfStockItemsByVariant = new HashMap<>();
//
//        for (CartItem cartItem : cartItems) {
//            ProductVariants pv = cartItem.getProductVariants();
//            if (pv.getStock() >= cartItem.getQuantity()) {
//                pv.setStock(pv.getStock() - cartItem.getQuantity());
//                updatedVariants.add(pv);
//                if (pv.getStock() == 0) {
//                    outOfStockItemsByVariant.computeIfAbsent((long) pv.getVarId(), k -> new ArrayList<>()).add(cartItem);
//                }
//            }
//        }
//
//        productVariantsRepository.saveAll(updatedVariants);
//
//        for (Map.Entry<Long, List<CartItem>> entry : outOfStockItemsByVariant.entrySet()) {
//            Long varId = entry.getKey();
//            List<CartItem> relatedCartItems = cartItemRepository.findByProductVariants_VarId(Math.toIntExact(varId));
//            List<CartItem> toUpdate = new ArrayList<>();
//            List<Cart> cartsToUpdate = new ArrayList<>();
//
//            for (CartItem ci : relatedCartItems) {
//                Cart relatedCart = ci.getCart();
//                if (relatedCart.getStatus() == Status_Cart.NEW) {
//                    ci.setQuantity(0);
//                    ci.setNote("Hiện đang hết hàng");
//                    ci.setTotalPrice(0.0);
//                    toUpdate.add(ci);
//
//                    List<CartItem> remainingItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(relatedCart.getCartId());
//                    double total = 0.0;
//                    int quantity = 0;
//                    for (CartItem rem : remainingItems) {
//                        total += rem.getTotalPrice();
//                        quantity += rem.getQuantity();
//                    }
//
//                    relatedCart.setTotalPrice(total);
//                    relatedCart.setTotalProduct(quantity);
//                    cartsToUpdate.add(relatedCart);
//                }
//            }
//
//            cartItemRepository.saveAll(toUpdate);
//            cartRepository.saveAll(cartsToUpdate);
//        }
//    }


    @Transactional
    public ResponseEntity<?> createPaymentCash(int orderId) {
        // Load full order with user and cart using custom query
        Orders order = orderRepository.fetchFullOrderWithUserAndCart(orderId);
        if (order == null || order.getStatus() != Status_Order.CONFIRMED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not confirmed");
        }
        if (order.getStatus() == Status_Order.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is cancelled");
        }

        User user = order.getUser();
        if (user.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is deleted");
        }

        Payment existingPayment = paymentRepository.findByOrderOrderId(orderId);
        if (existingPayment != null) {
            if (existingPayment.getPaymentMethod() == Payment_Method.CASH) {
                return ResponseEntity.badRequest().body("Payment already exists");
            }
            if (existingPayment.getPaymentMethod() == Payment_Method.CREDIT &&
                    existingPayment.getStatus() == Status_Payment.PENDING) {
                return ResponseEntity.badRequest().body("Payment create with type credit");
            }
            if (existingPayment.getStatus() == Status_Payment.COMPLETED) {
                return ResponseEntity.badRequest().body("Payment already completed");
            }
        }

        double totalAmount = order.getTotalPrice() - order.getDiscountPrice()
                - order.getPointCoinUse() + order.getDeliveryFee();

        Payment payment = new Payment();
        payment.setPaymentMethod(Payment_Method.CASH);
        payment.setDateCreated(LocalDateTime.now());
        payment.setOrder(order);
        payment.setIsDeleted(false);
        payment.setIsRefund(false);

        if (totalAmount <= 0.0) {
            payment.setStatus(Status_Payment.COMPLETED);
            payment.setAmount(0.0);
            payment.setOrderIdPayment("None");
        } else {
            payment.setStatus(Status_Payment.PENDING);
            payment.setAmount(totalAmount);
            payment.setLink("");
        }

        paymentRepository.save(payment);

        Shippment shipment = new Shippment();
        shipment.setPayment(payment);
        shipment.setIsDeleted(false);
        shipment.setDateCreated(LocalDateTime.now());
        shipment.setDateDelivered(LocalDateTime.now());
        shipment.setStatus(Status_Shipment.WAITING);
        shipmentRepository.save(shipment);

        // Update cart status
        Cart cart = order.getOrderItem().getCart();
        cart.setStatus(Status_Cart.COMPLETED);
        cartRepository.save(cart);

        // Process product variant stock and cartItem
        if (totalAmount > 0) {
            processStockAndCartItems(cart);
        }

        assignAllPendingTasks();

        Shippment persistedShipment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipment.getShipmentId());
        String note = "";
        if (persistedShipment.getUser() == null) {
            persistedShipment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
            shipmentRepository.save(persistedShipment);
            note = "Hiện không thể giao hàng";
        }

        if (totalAmount <= 0.0) {
            return ResponseEntity.ok(new CreatePaymentResponse(
                    payment.getPaymentId(), payment.getAmount(), payment.getDateCreated(),
                    payment.getDateDeleted(), payment.getDateRefunded(), payment.getIsDeleted(),
                    payment.getPaymentMethod(), payment.getStatus(), order.getOrderId(),
                    "", note
            ));
        } else {
            return ResponseEntity.ok(new CRUDPaymentResponse(
                    payment.getPaymentId(), payment.getAmount(), payment.getDateCreated(),
                    payment.getDateDeleted(), payment.getDateRefunded(), payment.getIsDeleted(),
                    payment.getPaymentMethod(), payment.getStatus(), order.getOrderId(),
                    payment.getIsRefund(), payment.getLink()
            ));
        }
    }


    public ResponseEntity<?> getAllPayment(String pageFromParam, String limitFromParam) {
        // Parse page and limit parameters
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);

        // Get all payments with pagination
        Page<Payment> payments = paymentRepository.findAllByIsDeletedFalse(pageable);

        // Prepare responses list
        List<CRUDPaymentResponse> responses = new ArrayList<>(payments.getContent().size());

        // Get all orders once and map them for faster access
        Set<Integer> orderIds = payments.getContent().stream()
                .map(payment -> payment.getOrder().getOrderId())
                .collect(Collectors.toSet());
        Map<Integer, Orders> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Orders::getOrderId, Function.identity()));

        // Loop through payments and map to DTOs
        for (Payment payment : payments) {
            Orders order = orderMap.get(payment.getOrder().getOrderId()); // Get order from preloaded map

            CRUDPaymentResponse response = new CRUDPaymentResponse(
                    payment.getPaymentId(),
                    payment.getAmount(),
                    payment.getDateCreated(),
                    payment.getDateDeleted(),
                    payment.getDateRefunded(),
                    payment.getIsDeleted(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    order != null ? order.getOrderId() : null, // Avoid potential NPE
                    payment.getIsRefund(),
                    payment.getLink()
            );
            responses.add(response);
        }

        // Return paginated response
        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
                page,
                payments.getTotalPages(),
                limit,
                (int) payments.getTotalElements(),
                responses
        ));
    }


    @Transactional()
    public ResponseEntity<?> getAllPaymentStatus(String pageFromParam, String limitFromParam, Status_Payment statusPayment) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit > 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<PaymentService.PaymentSummary> paymentSummaries = paymentRepository.findAllByStatusAndIsDeletedFalse(statusPayment, pageable);

        List<CRUDPaymentResponse> responses = paymentSummaries.stream()
                .map(p -> new CRUDPaymentResponse(
                        p.getPaymentId(),
                        p.getAmount(),
                        p.getDateCreated(),
                        p.getDateDeleted(),
                        p.getDateRefunded(),
                        p.getIsDeleted(),
                        p.getPaymentMethod(),
                        p.getStatus(),
                        p.getOrderId(),
                        p.getIsRefund(),
                        p.getLink()
                ))
                .toList();

        return ResponseEntity.ok(new ListAllPaymentResponse(
                page,
                paymentSummaries.getTotalPages(),
                limit,
                (int) paymentSummaries.getTotalElements(),
                responses
        ));
    }


    public ResponseEntity<?> getAllPaymentMethod(String pageFromParam, String limitFromParam, Payment_Method paymentMethod) {
        // Parse page and limit parameters
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);

        // Get payments with the specified payment method and not deleted, using pagination
        Page<Payment> payments = paymentRepository.findAllByPaymentMethodAndIsDeletedFalse(paymentMethod, pageable);

        // Preload all orders for the payments in a single query
        Set<Integer> orderIds = payments.getContent().stream()
                .map(payment -> payment.getOrder().getOrderId())
                .collect(Collectors.toSet());
        Map<Integer, Orders> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Orders::getOrderId, Function.identity()));

        // Prepare the responses list
        List<CRUDPaymentResponse> responses = new ArrayList<>(payments.getContent().size());

        // Loop through payments and map to DTOs
        for (Payment payment : payments) {
            Orders order = orderMap.get(payment.getOrder().getOrderId()); // Retrieve order from preloaded map

            CRUDPaymentResponse response = new CRUDPaymentResponse(
                    payment.getPaymentId(),
                    payment.getAmount(),
                    payment.getDateCreated(),
                    payment.getDateDeleted(),
                    payment.getDateRefunded(),
                    payment.getIsDeleted(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    order != null ? order.getOrderId() : null, // Avoid potential NPE
                    payment.getIsRefund(),
                    payment.getLink()
            );
            responses.add(response);
        }

        // Return paginated response
        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPaymentResponse(
                page,
                payments.getTotalPages(),
                limit,
                (int) payments.getTotalElements(),  // Use total elements for accurate total count
                responses
        ));
    }


    public ResponseEntity<?> getOnePayment(int paymentId){
        Payment payment = paymentRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
                payment.getIsRefund(),
                payment.getLink()
        ));
    }

    @Transactional
    public ResponseEntity<?> handleCallBackPayOS(int orderCode) throws Exception {
        PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
        List<Payment> payments = paymentRepository.findAll();
        String orderCode1="";
        for (Payment payment : payments) {
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
        Payment payment = paymentRepository.findByOrderIdPayment(orderCode1);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }
        String extractedCode = orderCode1.replace("PayOS", "");
        PaymentLinkData paymentLinkData = payOS.getPaymentLinkInformation(Long.valueOf(extractedCode));
        String status = paymentLinkData.getStatus();
        switch (status) {
            case "EXPIRED", "CANCELLED" -> {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
                Orders order = payment.getOrder();
                order.setDateCanceled(LocalDateTime.now());
                order.setStatus(Status_Order.CANCELLED);
                Voucher voucher = order.getVoucher();
                if(voucher != null) {
                    UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), voucher.getVoucherId());
                    userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                    userVoucherRepository.save(userVoucher);
                }
                List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
                for(CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                }
            }
            case "PAID" -> {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentRepository.save(payment);
                Shippment shippment = new Shippment();
                shippment.setPayment(payment);
                shippment.setIsDeleted(false);
                shippment.setDateCreated(LocalDateTime.now());
                shippment.setDateDelivered(LocalDateTime.now());
                shippment.setStatus(Status_Shipment.WAITING);


                shipmentRepository.save(shippment);

                Orders orders = payment.getOrder();
                Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
                cart.setStatus(Status_Cart.COMPLETED);
                cartRepository.save(cart);


                assignAllPendingTasks();
                Shippment shippment1 = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shippment.getShipmentId());
                if(shippment1.getUser() == null)
                {

                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
                    shipmentRepository.save(shippment);
                }
//                if(!status1)
//                {
//                    shippment.setDateDelivered(LocalDateTime.now().plusMinutes(30));
//                    shipmentRepository.save(shippment);
//                }
            }
            default -> {
                payment.setStatus(Status_Payment.PENDING);
                paymentRepository.save(payment);
            }
        }
        CRUDPaymentResponse crudPaymentResponse = new CRUDPaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getDateCreated(),
                payment.getDateDeleted(),
                payment.getDateRefunded(),
                payment.getIsDeleted(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getOrder().getOrderId(),
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
//    public  ResponseEntity<?> listAllPaymentGroupRefund(String pageFromParam, String limitFromParam){
//       int page = Integer.parseInt(pageFromParam);
//       int limit = Integer.parseInt(limitFromParam);
//       if (limit >= 100) limit = 100;
//
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

    public interface PaymentSummary {
        Integer getPaymentId();
        Double getAmount();
        LocalDateTime getDateCreated();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateRefunded();
        Boolean getIsDeleted();
        Payment_Method getPaymentMethod();
        Status_Payment getStatus();
        Integer getOrderId();  // Order id
        Boolean getIsRefund();
        String getLink();
    }


    @Transactional()
    public ResponseEntity<?> listAllPaymentRefund(String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<PaymentSummary> paymentsPage = paymentRepository.findAllByStatusAndIsDeletedFalse(Status_Payment.REFUND, pageable);

        List<CRUDPaymentResponse> responses = paymentsPage.stream()
                .map(p -> new CRUDPaymentResponse(
                        p.getPaymentId(),
                        p.getAmount(),
                        p.getDateCreated(),
                        p.getDateDeleted(),
                        p.getDateRefunded(),
                        p.getIsDeleted(),
                        p.getPaymentMethod(),
                        p.getStatus(),
                        p.getOrderId(),
                        p.getIsRefund(),
                        p.getLink()
                )).toList();

        return ResponseEntity.ok(new ListAllPaymentResponse(
                page,
                paymentsPage.getTotalPages(),
                limit,
                (int) paymentsPage.getTotalElements(),
                responses
        ));
    }


    @Transactional()
    public ResponseEntity<?> listAllPaymentRefundByStatus(String pageFromParam, String limitFromParam, Boolean statusRefund) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);

        pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "dateRefunded"));



        Page<PaymentSummary> paymentsPage = paymentRepository.findAllByStatusAndIsDeletedFalseAndIsRefund(Status_Payment.REFUND, statusRefund,pageable);

        List<CRUDPaymentResponse> responses = paymentsPage.stream()
                .map(p -> new CRUDPaymentResponse(
                        p.getPaymentId(),
                        p.getAmount(),
                        p.getDateCreated(),
                        p.getDateDeleted(),
                        p.getDateRefunded(),
                        p.getIsDeleted(),
                        p.getPaymentMethod(),
                        p.getStatus(),
                        p.getOrderId(),
                        p.getIsRefund(),
                        p.getLink()
                )).toList();

        return ResponseEntity.ok(new ListAllPaymentResponse(
                page,
                paymentsPage.getTotalPages(),
                limit,
                (int) paymentsPage.getTotalElements(),
                responses
        ));
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
    public ResponseEntity<?> activateRefund(int paymentId, String token) {
        Payment payment = paymentRepository.findByPaymentIdAndIsDeletedFalse(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }

        Shippment shippment = payment.getShipment();
        if (shippment.getStatus() == Status_Shipment.WAITING || shippment.getStatus() == Status_Shipment.SHIPPING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not refund with shipment");
        }

        if (payment.getPaymentMethod() == Payment_Method.CASH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not refund with payment method cash");
        }

        if (payment.getIsRefund()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Refund exists");
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
            paymentRepository.save(payment);
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
                paymentRepository.save(payment);
                return ResponseEntity.ok("Refund success: " + response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Refund failed: " + response.getBody());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Refund error: " + e.getMessage());
        }
    }



    @Transactional
    public ResponseEntity<?> checkTimePayment(){
        List<Payment> paymentList = paymentRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CREDIT);

        for(Payment payment : paymentList)
        {
            Orders orders1 = payment.getOrder();
            if(orders1 != null)
            {
                if(orders1.getStatus() == Status_Order.WAITING || (orders1.getStatus() == Status_Order.CONFIRMED && orders1.getPayment() == null))
                {
                    if(orders1.getDateDeleted().plusMinutes(20).isBefore(LocalDateTime.now()))
                    {
                              orders1.setDateCanceled(LocalDateTime.now());
                              orders1.setStatus(Status_Order.CONFIRMED);
                    }
                }

            }
            if(payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            if (payment.getDateCreated().plusMinutes(30).isBefore(LocalDateTime.now())) {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
                Orders orders = payment.getOrder();
                orders.setDateCanceled(LocalDateTime.now());
                orders.setStatus(Status_Order.CANCELLED);
                orderRepository.save(orders);
                Voucher voucher = orders.getVoucher();
                if(voucher != null) {
                    UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(orders.getUser().getUserId(), voucher.getVoucherId());
                    userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                    userVoucherRepository.save(userVoucher);
                }
                List<CartItem> cartItems = orders.getOrderItem().getCart().getCartItems();
                for(CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                }
                Cart cart = orders.getOrderItem().getCart();
                if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                {
                    cart.setStatus(Status_Cart.COMPLETED);
                    cartRepository.save(cart);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void checkTimePaymentSchedule(){
        List<Payment> paymentList = paymentRepository.findAllByPaymentMethodAndIsDeletedFalse(Payment_Method.CREDIT);

        for(Payment payment : paymentList)
        {
            Orders orders1 = payment.getOrder();
            if(orders1 != null)
            {
                if(orders1.getStatus() == Status_Order.WAITING || (orders1.getStatus() == Status_Order.CONFIRMED && orders1.getPayment() == null))
                {
                    if(orders1.getDateDeleted().plusMinutes(20).isBefore(LocalDateTime.now()))
                    {
                        orders1.setDateCanceled(LocalDateTime.now());
                        orders1.setStatus(Status_Order.CONFIRMED);
                    }
                }

            }
            if(payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            if (payment.getDateCreated().plusMinutes(30).isBefore(LocalDateTime.now())) {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
                Orders orders = payment.getOrder();
                orders.setDateCanceled(LocalDateTime.now());
                orders.setStatus(Status_Order.CANCELLED);
                orderRepository.save(orders);
                Voucher voucher = orders.getVoucher();
                if(voucher != null) {
                    UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(orders.getUser().getUserId(), voucher.getVoucherId());
                    userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                    userVoucherRepository.save(userVoucher);
                }
                List<CartItem> cartItems = orders.getOrderItem().getCart().getCartItems();
                for(CartItem cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                }
                Cart cart = orders.getOrderItem().getCart();
                if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                {
                    cart.setStatus(Status_Cart.COMPLETED);
                    cartRepository.save(cart);
                }
            }
        }

    }

}
