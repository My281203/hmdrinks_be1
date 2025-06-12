package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CRUDShipmentReq;
import com.hmdrinks.Request.UpdateTimeShipmentReq;
import com.hmdrinks.Response.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShipmentService {
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
    private NotificationService notificationService;
    @Autowired
    private  UserCointRepository userCointRepository;
    @Autowired
    private  ShipmentDirectionRepository shipmentDirectionRepository;
    @Autowired
    private StepDetailsRepository stepDetailsRepository;

    @Transactional
    public ResponseEntity<?> shipmentAllocation(CRUDShipmentReq req) {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(req.getShipmentId());
        if (shippment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }
        if (user.getRole() != Role.SHIPPER) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not shipper");
        }
        shippment.setDateDelivered(req.getDateDeliver());
        shippment.setDateShip(req.getDateShip());
        shippment.setUser(user);
        shipmentRepository.save(shippment);

        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        if (payment.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment is deleted");
        }
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        if (orders.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order is deleted");
        }

        User customer = userRepository.findByUserIdAndIsDeletedFalse(orders.getUser().getUserId());
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Not Found");
        }
        System.out.println("User ID: " + shippment.getUser().getUserId());

        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                orders.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> activateShipment(int shipmentId, int userId)
    {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        if(shippment.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment is deleted");
        }
        if(shippment.getStatus() != Status_Shipment.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Shipment is not waiting");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        shippment.setUser(userRepository.findByUserIdAndIsDeletedFalse(userId));
        shippment.setDateDelivered(LocalDateTime.now().plusMinutes(25));
        shippment.setStatus(Status_Shipment.SHIPPING);
        shipmentRepository.save(shippment);
        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
        for (CartItem cartItem : cartItems) {
            ProductVariants productVariants = cartItem.getProductVariants();
            if (productVariants.getStock() > cartItem.getQuantity()) {
                productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
                productVariantsRepository.save(productVariants);
            }
            else{
                return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Stock Not Enough");
            }
        }


        User customer = userRepository.findByUserIdAndIsDeletedFalse(orders.getUser().getUserId());
        // Gửi thông báo đến khách hàng
        try {
            String message = "Đơn hàng của bạn đã bắt đầu giao";
            notificationService.sendNotification(orders.getUser().getUserId(), payment.getOrder().getOrderId(), message);
        } catch (Exception e) {
            System.err.println("Failed to send notification to shipper. Error: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                orders.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> cancelShipment(int shipmentId)
    {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        if(shippment.getStatus() != Status_Shipment.SHIPPING && shippment.getStatus() != Status_Shipment.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }

        if(LocalDateTime.now().isAfter(shippment.getDateDelivered()))
        {
                    shippment.setDateCancel(LocalDateTime.now());
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shipmentRepository.save(shippment);
                    Payment payment = shippment.getPayment();
                    Orders orders = payment.getOrder();

                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orders.setDateCanceled(LocalDateTime.now());
                        orders.setStatus(Status_Order.CANCELLED);
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        if(payment.getAmount() == 0.0)
                        {
                            payment.setIsRefund(true);
                        }
                        paymentRepository.save(payment);
                        orders.setDateCanceled(LocalDateTime.now());
                        orders.setStatus(Status_Order.CANCELLED);
                        orderRepository.save(orders);

                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    Cart cart = cartRepository.findByCartId(orders.getOrderItem().getCart().getCartId());
                    List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

                    for (CartItem cartItem : cartItems) {
                      ProductVariants productVariants = cartItem.getProductVariants();
                      productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                      productVariantsRepository.save(productVariants);

            }

               return ResponseEntity.ok().build();
        }

        shippment.setStatus(Status_Shipment.CANCELLED);
        shippment.setDateCancel(LocalDateTime.now());
        shipmentRepository.save(shippment);

        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
        {
            payment.setStatus(Status_Payment.REFUND);
            payment.setDateRefunded(LocalDateTime.now());
            payment.setIsRefund(false);
            if(payment.getAmount() == 0.0)
            {
                payment.setIsRefund(true);
            }
            paymentRepository.save(payment);
            Orders orders = payment.getOrder();
            orders.setDateCanceled(LocalDateTime.now());
            orders.setStatus(Status_Order.CANCELLED);
            orderRepository.save(orders);
            UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
            if(userCoin != null)
            {
                float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                userCoin.setPointCoin(point_coint);
                userCointRepository.save(userCoin);
            }
        }
        else{
            payment.setStatus(Status_Payment.FAILED);
            paymentRepository.save(payment);
            Orders orders = payment.getOrder();
            orders.setDateCanceled(LocalDateTime.now());
            orders.setStatus(Status_Order.CANCELLED);
            orderRepository.save(orders);
            UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
            if(userCoin != null)
            {
                float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                userCoin.setPointCoin(point_coint);
                userCointRepository.save(userCoin);
            }
        }
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        User customer = userRepository.findByUserId(orders.getUser().getUserId());

        // Gửi thông báo đến khách hàng
        try {
            String message = "Đơn hàng của bạn đã bị hủy";
            Integer userId = orders.getUser().getUserId();
            notificationService.sendNotification(userId, orders.getOrderId(), message);
        } catch (Exception e) {
            System.err.println("Failed to send notification to shipper. Error: " + e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                orders.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> activate_Admin(int shipmentId, Status_Shipment statusShipment)
    {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        shippment.setStatus(statusShipment);
        if(statusShipment == Status_Shipment.CANCELLED)
        {
            shippment.setDateCancel(LocalDateTime.now());
            shipmentRepository.save(shippment);
        }
        shipmentRepository.save(shippment);
        if(statusShipment == Status_Shipment.SUCCESS)
        {
            shippment.setDateShip(LocalDateTime.now());
            shipmentRepository.save(shippment);
            Payment payment = shippment.getPayment();
            if(payment.getPaymentMethod() == Payment_Method.CASH)
            {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentRepository.save(payment);
            }
            float add_point = (float) (payment.getAmount() * 0.01);
            UserCoin userCoin = userCointRepository.findByUserUserId(shippment.getPayment().getOrder().getUser().getUserId());
            if(userCoin != null)
            {
                float total_point = userCoin.getPointCoin() + add_point;
                userCoin.setPointCoin(total_point);
                userCointRepository.save(userCoin);
            }
            else {
                UserCoin userCoin1 = new UserCoin();
                userCoin1.setPointCoin(add_point);
                userCoin1.setUser(shippment.getPayment().getOrder().getUser());
                userCointRepository.save(userCoin1);
            }


        }

        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        User customer = orders.getUser();

        if(statusShipment == Status_Shipment.CANCELLED) {
            Cart cart = orders.getOrderItem().getCart();
            List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

            for (CartItem cartItem : cartItems) {
                ProductVariants productVariants = cartItem.getProductVariants();
                productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                productVariantsRepository.save(productVariants);
            }
            if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
            {
                payment.setStatus(Status_Payment.REFUND);
                payment.setDateRefunded(LocalDateTime.now());
                payment.setIsRefund(false);
                if(payment.getAmount() == 0.0)
                {
                    payment.setIsRefund(true);
                }
                paymentRepository.save(payment);
            }
            if (payment.getPaymentMethod() == Payment_Method.CASH
            ) {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
            }
            orders.setStatus(Status_Order.CANCELLED);
            orders.setDateCanceled(LocalDateTime.now());
            orderRepository.save(orders);

        }

        User shipper = shippment.getUser();
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(), orders.getOrderId()
        ));
    }


    @Transactional
    public ResponseEntity<?> successShipment(int shipmentId,int userId)
    {
        Shippment shippment = shipmentRepository.findByUserUserIdAndShipmentId(userId,shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        if(shippment.getStatus() != Status_Shipment.SHIPPING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        shippment.setStatus(Status_Shipment.SUCCESS);
        shippment.setDateShip(LocalDateTime.now());
        shipmentRepository.save(shippment);

        Payment payment1 = shippment.getPayment();
        if(payment1.getPaymentMethod() == Payment_Method.CASH)
        {
            payment1.setStatus(Status_Payment.COMPLETED);
            paymentRepository.save(payment1);
        }

        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        User customer = userRepository.findByUserId(orders.getUser().getUserId());

        // Gửi thông báo đến khách hàng
        try {
            System.out.print("Giao đơn thành công");
            String message = "Đơn hàng của bạn đã được giao thành công";
            notificationService.sendNotification(orders.getUser().getUserId(), orders.getOrderId(), message);
        } catch (Exception e) {
            System.err.println("Failed to send notification to shipper. Error: " + e.getMessage());
        }
        float add_point = (float) (payment.getAmount() * 0.01);
        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
        if(userCoin != null)
        {
            float total_point = userCoin.getPointCoin() + add_point;
            userCoin.setPointCoin(total_point);
            userCointRepository.save(userCoin);
        } else {

            UserCoin userCoin1 = new UserCoin();
            userCoin1.setPointCoin(add_point);
            userCoin1.setUser(orders.getUser());
            userCointRepository.save(userCoin1);
        }


        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(), orders.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> getListShipmentStatusByShipper(String pageFromParam, String limitFromParam, int userId, Status_Shipment status) {

        User shipper_check = userRepository.findByUserId(userId);
        if(shipper_check.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Sort sort = switch (status) {
            case CANCELLED -> Sort.by(Sort.Direction.DESC, "dateCancel");
            case SUCCESS -> Sort.by(Sort.Direction.DESC, "dateShip");
            case SHIPPING, WAITING -> Sort.by(Sort.Direction.DESC, "dateCreated");
            default -> Sort.unsorted();
        };

        Pageable pageable = (sort.isUnsorted())
                ? PageRequest.of(page - 1, limit)
                : PageRequest.of(page - 1, limit, sort);

        List<Shippment> shippments = shipmentRepository.findAllByUserIdAndStatusFetchAll(userId, status, pageable);
        int totalRecords = shipmentRepository.countByUserIdAndStatus(userId, status);
        int totalPages = (int) Math.ceil((double) totalRecords / limit);

        List<CRUDShipmentResponse> responses = shippments.stream().map(shippment -> {
            Payment payment = shippment.getPayment();
            Orders orders = payment.getOrder();
            User customer = orders.getUser();
            User shipper = shippment.getUser();

            return new CRUDShipmentResponse(
                    shippment.getShipmentId(),
                    shipper != null ? shipper.getFullName() : null,
                    shippment.getDateCreated(),
                    shippment.getDateDeleted(),
                    shippment.getDateDelivered(),
                    shippment.getDateShip(),
                    shippment.getDateCancel(),
                    shippment.getIsDeleted(),
                    shippment.getStatus(),
                    shippment.getNote(),
                    payment.getPaymentId(),
                    shipper != null ? shipper.getUserId() : null,
                    customer.getFullName(),
                    customer.getUserId(),
                    orders.getAddress(),
                    customer.getPhoneNumber(),
                    customer.getEmail(),
                    orders.getOrderId()
            );
        }).toList();

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllScheduledShipmentsResponse(
                page,
                totalPages,
                limit,
                totalRecords,
                responses
        ));
    }


    @Getter
    @Setter
    public static class ShipmentListDTO {
        private Integer shipmentId;
        private String shipperName;
        private LocalDateTime orderDate;
        private LocalDateTime dateDeleted;
        private LocalDateTime dateDelivered;
        private LocalDateTime dateShip;
        private LocalDateTime dateCancel;
        private Boolean isDeleted;
        private Status_Shipment status;
        private String note;
        private Integer paymentId;
        private Integer shipperId;
        private String customerFullName;
        private Integer customerId;
        private String customerAddress;
        private String customerPhone;
        private String customerEmail;
        private Integer orderId;
        private  LocalDateTime dateCreated;

        public ShipmentListDTO(Integer shipmentId, String shipperName, LocalDateTime orderDate,
                               LocalDateTime dateDeleted, LocalDateTime dateDelivered,
                               LocalDateTime dateShip, LocalDateTime dateCancel,
                               Boolean isDeleted, Status_Shipment status, String note,
                               Integer paymentId, Integer shipperId, String customerFullName,
                               Integer customerId, String customerAddress,
                               String customerPhone, String customerEmail, Integer orderId,LocalDateTime dateCreated) {
            this.shipmentId = shipmentId;
            this.shipperName = shipperName;
            this.orderDate = orderDate;
            this.dateDeleted = dateDeleted;
            this.dateDelivered = dateDelivered;
            this.dateShip = dateShip;
            this.dateCancel = dateCancel;
            this.isDeleted = isDeleted;
            this.status = status;
            this.note = note;
            this.paymentId = paymentId;
            this.shipperId = shipperId;
            this.customerFullName = customerFullName;
            this.customerId = customerId;
            this.customerAddress = customerAddress;
            this.customerPhone = customerPhone;
            this.customerEmail = customerEmail;
            this.orderId = orderId;
            this.dateCreated = dateCreated;
        }

        // (Optional) Getter/Setter ở đây nếu bạn cần
    }

    @Transactional
    public ResponseEntity<?> getListAllShipmentByShipper(String pageFromParam, String limitFromParam, int userId) {
        int page = Math.max(1, Integer.parseInt(pageFromParam));
        int limit = Math.min(Math.max(1, Integer.parseInt(limitFromParam)), 100);
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "dateCreated"));

        User shipperCheck = userRepository.findByUserId(userId);
        if (shipperCheck == null || Boolean.TRUE.equals(shipperCheck.getIsDeleted())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted or not found");
        }

        Page<ShipmentListProjection> pageData = shipmentRepository.findShipmentDetailsByUserId(userId, pageable);
        List<CRUDShipmentResponse> responses = new ArrayList<>(pageData.getSize());
        for (ShipmentListProjection dto : pageData) {
            responses.add(new CRUDShipmentResponse(
                    dto.getShipmentId(),
                    dto.getShipperName(),
                    dto.getShipmentDateCreated(),
                    dto.getDateDeleted(),
                    dto.getDateDelivered(),
                    dto.getDateShip(),
                    dto.getDateCancel(),
                    dto.getIsDeleted(),
                    dto.getStatus(),
                    dto.getNote(),
                    dto.getPaymentId(),
                    dto.getShipperId(),
                    dto.getCustomerFullName(),
                    dto.getCustomerId(),
                    dto.getCustomerAddress(),
                    dto.getCustomerPhone(),
                    dto.getCustomerEmail(),
                    dto.getOrderId()
            ));
        }

        return ResponseEntity.ok(new ListAllScheduledShipmentsResponse(
                page,
                pageData.getTotalPages(),
                limit,
                (int) pageData.getTotalElements(),
                responses
        ));
    }



//    @Transactional
//    public ResponseEntity<?> getListAllShipment(String pageFromParam, String limitFromParam) {
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
//
//        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
//        Pageable pageable = PageRequest.of(page - 1, limit, sort);
//
//        Page<ShipmentListDTO> shipmentPage = shipmentRepository.findAllShipmentDetails(pageable);
//        List<CRUDShipmentResponse> responses = shipmentPage.getContent().stream()
//                .map(dto -> new CRUDShipmentResponse(
//                        dto.getShipmentId(),
//                        dto.getShipperName(),
//                        dto.getOrderDate(),
//                        dto.getDateDeleted(),
//                        dto.getDateDelivered(),
//                        dto.getDateShip(),
//                        dto.getDateCancel(),
//                        dto.getIsDeleted(),
//                        dto.getStatus(),
//                        dto.getNote(),
//                        dto.getPaymentId(),
//                        dto.getShipperId(),
//                        dto.getCustomerFullName(),
//                        dto.getCustomerId(),
//                        dto.getCustomerAddress(),
//                        dto.getCustomerPhone(),
//                        dto.getCustomerEmail(),
//                        dto.getOrderId()
//                ))
//                .collect(Collectors.toList());
//
//        return ResponseEntity.status(HttpStatus.OK).body(new ListAllScheduledShipmentsResponse(
//                page,
//                shipmentPage.getTotalPages(),
//                limit,
//                shipmentPage.getSize(),
//                responses
//        ));
//    }

    public interface ShipmentListProjection {
        Integer getShipmentId();
        String getShipperName();
        LocalDateTime getOrderDate();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateDelivered();
        LocalDateTime getDateShip();
        LocalDateTime getDateCancel();
        Boolean getIsDeleted();
        Status_Shipment getStatus();
        String getNote();
        Integer getPaymentId();
        Integer getShipperId();
        String getCustomerFullName();
        Integer getCustomerId();
        String getCustomerAddress();
        String getCustomerPhone();
        String getCustomerEmail();
        Integer getOrderId();
        LocalDateTime getShipmentDateCreated();
    }



    @Transactional
    public ResponseEntity<?> getListAllShipment(String pageFromParam, String limitFromParam) {
        int page = 1;
        int limit = 10;

        try {
            page = Integer.parseInt(pageFromParam);
            limit = Integer.parseInt(limitFromParam);
        } catch (NumberFormatException e) {
            // fallback mặc định
        }

        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;

        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        Page<ShipmentListProjection> pageData = shipmentRepository.findAllShipmentDetails(pageable);
        long activeShipmentCount = shipmentRepository.countAllActiveShipments();



        List<CRUDShipmentResponse> responses = pageData.stream()
                .map(dto -> new CRUDShipmentResponse(
                        dto.getShipmentId(),
                        dto.getShipperName(),
                        dto.getOrderDate(),
                        dto.getDateDeleted(),
                        dto.getDateDelivered(),
                        dto.getDateShip(),
                        dto.getDateCancel(),
                        dto.getIsDeleted(),
                        dto.getStatus(),
                        dto.getNote(),
                        dto.getPaymentId(),
                        dto.getShipperId(),
                        dto.getCustomerFullName(),
                        dto.getCustomerId(),
                        dto.getCustomerAddress(),
                        dto.getCustomerPhone(),
                        dto.getCustomerEmail(),
                        dto.getOrderId()
                )).toList();

        return ResponseEntity.ok(new ListAllScheduledShipmentsResponse(
                page,
                pageData.getTotalPages(),
                limit,
                (int) activeShipmentCount,
                responses
        ));
    }




    @Transactional()
    public ResponseEntity<?> getListAllShipmentByStatus(String pageFromParam, String limitFromParam, Status_Shipment status) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Sort sort = switch (status) {
            case CANCELLED -> Sort.by(Sort.Direction.DESC, "dateCancel");
            case SUCCESS -> Sort.by(Sort.Direction.DESC, "dateShip");
            default -> Sort.by(Sort.Direction.DESC, "dateCreated");
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        // Bước 1: chỉ lấy ID cần thiết
        Page<Integer> shipmentIdPage = shipmentRepository.findShipmentIdsByStatus(status, pageable);
        List<Integer> shipmentIds = shipmentIdPage.getContent();

        if (shipmentIds.isEmpty()) {
            return ResponseEntity.ok(new ListAllScheduledShipmentsResponse(
                    page,
                    shipmentIdPage.getTotalPages(),
                    limit,
                    0,
                    Collections.emptyList()
            ));
        }

        // Bước 2: fetch full info bằng DTO
        List<CRUDShipmentResponse> responses = shipmentRepository.findShipmentsByIds(shipmentIds);

        return ResponseEntity.ok(new ListAllScheduledShipmentsResponse(
                page,
                shipmentIdPage.getTotalPages(),
                limit,
                (int) shipmentIdPage.getTotalElements(),
                responses
        ));
    }




    public boolean isShipmentDatesValid(Shippment shipment, UpdateTimeShipmentReq updateRequest) {
        LocalDateTime dateCreated = shipment.getDateCreated();
        LocalDateTime dateShipped = updateRequest.getDateShipped();
        LocalDateTime dateDelivered = updateRequest.getDateDelivered();

        boolean isDateShippedValid = dateShipped != null && dateShipped.isAfter(dateCreated);
        boolean isDateDeliveredValid = dateDelivered != null && dateDelivered.isAfter(dateCreated);

        return isDateShippedValid && isDateDeliveredValid;
    }


    @Transactional
    public ResponseEntity<?> updateTimeShipment(UpdateTimeShipmentReq req)
    {
         User user1 = null;
         Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(req.getShipmentId());
         if(shippment == null)
         {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment not found");
         }

         boolean check = isShipmentDatesValid(shippment,req);
         if(!check)
         {
             return ResponseEntity.status(HttpStatus.CONFLICT).body("Shipment date is not valid");
         }
         shippment.setDateDelivered(req.getDateDelivered());
         shippment.setDateShip(req.getDateShipped());
         shipmentRepository.save(shippment);
        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        User customer = userRepository.findByUserId(orders.getUser().getUserId());
        User shipper = shippment.getUser();
         return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                 shippment.getShipmentId(),
                 shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                 shippment.getDateCreated(),
                 shippment.getDateDeleted(),
                 shippment.getDateDelivered(),
                 shippment.getDateShip(),
                 shippment.getDateCancel(),
                 shippment.getIsDeleted(),
                 shippment.getStatus(),
                 shippment.getNote(),
                 shippment.getPayment().getPaymentId(),
                 shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                 customer.getFullName(),
                 customer.getUserId(),
                 orders.getAddress(),
                 customer.getPhoneNumber(),
                 customer.getEmail(),
                 orders.getOrderId()
         ));
    }

    @Transactional
    public ResponseEntity<?> getInfoShipmentByOrderId(int orderId)
    {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if(order == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        Payment payment = paymentRepository.findByPaymentId(order.getPayment().getPaymentId());
        if(payment != null)
        {

        }
        User customer = order.getUser();
        Shippment shipment = shipmentRepository.findByPaymentPaymentIdAndIsDeletedFalse(payment.getPaymentId());
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shipment != null ?shipment.getShipmentId() : null,
                shipment != null && shipment.getUser() != null ? shipment.getUser().getFullName() : null,

                shipment != null ? shipment.getDateCreated(): null,
                shipment != null ? shipment.getDateDeleted(): null,
                shipment != null ? shipment.getDateDelivered(): null,
                shipment != null ? shipment.getDateShip(): null,
                shipment != null ? shipment.getDateCancel(): null,
                shipment != null ? shipment.getIsDeleted(): null,
                shipment != null ? shipment.getStatus(): null,
                shipment != null ? shipment.getNote(): null,
                shipment != null ? shipment.getPayment().getPaymentId(): null,
                shipment != null && shipment.getUser() != null  ? shipment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                order.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                order.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> checkTimeDelivery(){
        List<Shippment> shippmentList = shipmentRepository.findAll();
        LocalDateTime now  = LocalDateTime.now();
        for(Shippment shippment : shippmentList)
        {


            if(shippment.getUser() == null && shippment.getStatus() == Status_Shipment.WAITING)
            {
                LocalDateTime time_create = shippment.getDateCreated().plusHours(1);
                if(now.isAfter(time_create))
                {
                    Payment payment = shippment.getPayment();
                    Orders orders = payment.getOrder();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentRepository.save(shippment);
                }
            }
            if(shippment.getStatus() == Status_Shipment.WAITING)
            {
                LocalDateTime time_create = shippment.getDateCreated().plusHours(1);
                if(now.isAfter(time_create))
                {
                    Payment payment = shippment.getPayment();
                    Orders orders = payment.getOrder();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentRepository.save(shippment);
                }
            }
            if(shippment.getStatus() == Status_Shipment.SHIPPING && shippment.getUser() != null)
            {
                if(now.isAfter(shippment.getDateDelivered())) {
                    Payment payment = shippment.getPayment();
                    Orders orders = payment.getOrder();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentRepository.save(shippment);
                }
            }
            if(shippment.getStatus() == Status_Shipment.SHIPPING)
            {
                if(now.isAfter(shippment.getDateDelivered())) {
                    Payment payment = shippment.getPayment();
                    Orders orders = payment.getOrder();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentRepository.save(payment);
                        orders.setStatus(Status_Order.CANCELLED);
                        orders.setDateCanceled(LocalDateTime.now());
                        orderRepository.save(orders);
                        UserCoin userCoin = userCointRepository.findByUserUserId(orders.getUser().getUserId());
                        if(userCoin != null)
                        {
                            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
                            userCoin.setPointCoin(point_coint);
                            userCointRepository.save(userCoin);
                        }
                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentRepository.save(shippment);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> getOneShipment(int shipmentId){
        Shippment shipment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shipment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Payment payment = paymentRepository.findByPaymentId(shipment.getPayment().getPaymentId());
        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
        User customer = orders.getUser();

        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shipment.getShipmentId(),
                shipment.getUser() != null ? shipment.getUser().getFullName() : null,
                shipment.getDateCreated(),
                shipment.getDateDeleted(),
                shipment.getDateDelivered(),
                shipment.getDateShip(),
                shipment.getDateCancel(),
                shipment.getIsDeleted(),
                shipment.getStatus(),
                shipment.getNote(),
                shipment.getPayment().getPaymentId(),
                shipment.getUser() != null ? shipment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                orders.getOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> ActivateReceiving(int shipmentId, int userId)
    {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        User user = userRepository.findByUserId(userId);
        if(user == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }
        if(user.getRole() != Role.SHIPPER)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not shipper");
        }
        if (shippment.getUser() != null) {
            if (shippment.getUser().getUserId() != null && shippment.getUser().getUserId() != userId) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Shipment is receiving");
            }
        }
        if(shippment.getStatus() != Status_Shipment.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Shipment is not waiting");
        }
        shippment.setUser(user);
        shippment.setDateDelivered(LocalDateTime.now().plusMinutes(25));
        shipmentRepository.save(shippment);
        Payment payment = shippment.getPayment();
        Orders orders = payment.getOrder();
        User customer = orders.getUser();
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
                shippment.getShipmentId(),
                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                shippment.getDateCreated(),
                shippment.getDateDeleted(),
                shippment.getDateDelivered(),
                shippment.getDateShip(),
                shippment.getDateCancel(),
                shippment.getIsDeleted(),
                shippment.getStatus(),
                shippment.getNote(),
                shippment.getPayment().getPaymentId(),
                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                customer.getFullName(),
                customer.getUserId(),
                orders.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                orders.getOrderId()
        ));
    }

    @Transactional()
    public ResponseEntity<?> getListShipmentStatusWaitingByUserId(int userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        // ✅ Custom query: load toàn bộ order có shipment.status = WAITING (chỉ 1 truy vấn)
        List<Shippment> waitingShipments = shipmentRepository.findWaitingShipmentsByUserId(userId);

        List<CRUDShipmentResponse> responses = waitingShipments.stream()
                .map(shipment -> {
                    Payment payment = shipment.getPayment();
                    Orders order = payment.getOrder();
                    User customer = order.getUser();
                    User shipper = shipment.getUser();

                    // ✅ Bộ lọc logic thanh toán
                    if (order.getStatus() != Status_Order.CONFIRMED) return null;
                    if (payment.getStatus() != Status_Payment.COMPLETED && payment.getPaymentMethod() == Payment_Method.CREDIT) return null;
                    if (payment.getStatus() == Status_Payment.FAILED && payment.getPaymentMethod() == Payment_Method.CASH) return null;

                    return new CRUDShipmentResponse(
                            shipment.getShipmentId(),
                            shipper != null ? shipper.getFullName() : null,
                            shipment.getDateCreated(),
                            shipment.getDateDeleted(),
                            shipment.getDateDelivered(),
                            shipment.getDateShip(),
                            shipment.getDateCancel(),
                            shipment.getIsDeleted(),
                            shipment.getStatus(),
                            shipment.getNote(),
                            payment.getPaymentId(),
                            shipper != null ? shipper.getUserId() : null,
                            customer.getFullName(),
                            customer.getUserId(),
                            order.getAddress(),
                            customer.getPhoneNumber(),
                            customer.getEmail(),
                            order.getOrderId()
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(new ListAllShipmentsWaitingByUserId(responses.size(), responses));
    }



    @Transactional
    public ResponseEntity<?> searchShipment(String keyword, String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);

        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Shippment> shipments = shipmentRepository.searchByShipperName(keyword, pageable);
        List<CRUDShipmentResponse> responses = new ArrayList<>();

        for (Shippment shipment : shipments) {
            User shipper = shipment.getUser();
            Integer shipperId = (shipper != null) ? shipper.getUserId() : null;
            String shipperName = (shipper != null) ? shipper.getFullName() : "Unknown Shipper";

            Payment payment = paymentRepository.findByPaymentId(shipment.getPayment().getPaymentId());
            if (payment == null || payment.getOrder() == null) {
                continue;
            }

            Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
            User customer = userRepository.findByUserId(orders.getUser().getUserId());

            String customerName = (customer != null) ? customer.getFullName() : "Unknown Customer";
            Integer customerId = (customer != null) ? customer.getUserId() : null;
            String customerAddress = (customer != null)
                    ? (customer.getStreet() + ", " + customer.getWard() + ", " + customer.getDistrict() + ", " + customer.getCity())
                    : "Unknown Address";
            String customerPhone = (customer != null) ? customer.getPhoneNumber() : "Unknown Phone";
            String customerEmail = (customer != null) ? customer.getEmail() : "Unknown Email";

            CRUDShipmentResponse response = new CRUDShipmentResponse(
                    shipment.getShipmentId(),
                    shipperName,
                    orders.getOrderDate(),
                    shipment.getDateDeleted(),
                    shipment.getDateDelivered(),
                    shipment.getDateShip(),
                    shipment.getDateCancel(),
                    shipment.getIsDeleted(),
                    shipment.getStatus(),
                    shipment.getNote(),
                    shipment.getPayment().getPaymentId(),
                    shipperId,
                    customerName,
                    customerId,
                    customerAddress,
                    customerPhone,
                    customerEmail,
                    orders.getOrderId()
            );

            responses.add(response);
        }

        return ResponseEntity.ok(new TotalSearchShipmentResponse(
                page,
                shipments.getTotalPages(),
                limit,
                responses
        ));

    }

    @Transactional
    public  ResponseEntity<?> updateNote(int shipmentId, String note)
    {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if (shippment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        if (Objects.equals(note, ""))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Note is empty");
        }
        shippment.setNote(note);
        shipmentRepository.save(shippment);
        return ResponseEntity.status(HttpStatus.OK).body("Success");

    }


    @Transactional
    public ResponseEntity<?> getMapdirection(int shipmentId) {
        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if (shippment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not allow");
        }
        List<CRUDStepDetailResponse> detailResponses = new ArrayList<>();
        ShipmentDirection shipmentDirection = shipmentDirectionRepository.findByShipmentShipmentId(shipmentId);
        if (shipmentDirection == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Direction Not Found");
        }
        List<StepDetail> stepDetails = stepDetailsRepository.findByDirection(shipmentDirection);
        for(StepDetail stepDetail : stepDetails) {
            CRUDStepDetailResponse response = new CRUDStepDetailResponse(
                    Math.toIntExact(stepDetail.getId()),
                    stepDetail.getDistanceText(),
                    stepDetail.getDurationText(),
                    stepDetail.getInstruction(),
                    stepDetail.getLatitude(),
                    stepDetail.getLongitude(),
                    Math.toIntExact(stepDetail.getDirection().getId())
            );
            detailResponses.add(response);
        }
        String encodedPolyline = shipmentDirection.getOverviewPolyline(); // Lấy polyline động
        double longitudeStart = shipmentDirection.getLongitudeStart();
        double latitudeStart = shipmentDirection.getLatitudeStart();
        double longitudeEnd = shipmentDirection.getLongitudeEnd();
        double latitudeEnd = shipmentDirection.getLatitudeEnd();

        String mapHTML = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\" />\n" +
                "<title>Draw route on map using Goong Directions API</title>\n" +
                "<meta name=\"viewport\" content=\"initial-scale=1,maximum-scale=1,user-scalable=no\" />\n" +
                "<script src=\"https://cdn.jsdelivr.net/npm/@goongmaps/goong-js@1.0.9/dist/goong-js.js\"></script>\n" +
                "<link href=\"https://cdn.jsdelivr.net/npm/@goongmaps/goong-js@1.0.9/dist/goong-js.css\" rel=\"stylesheet\" />\n" +
                "<style>\n" +
                "    body { margin: 0; padding: 0; }\n" +
                "    #map { position: absolute; top: 0; bottom: 0; width: 100%; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<script src=\"https://cdn.jsdelivr.net/npm/@mapbox/polyline/src/polyline.js\"></script>\n" +
                "<script src=\"https://unpkg.com/@goongmaps/goong-sdk/umd/goong-sdk.min.js\"></script>\n" +
                "<div id=\"map\"></div>\n" +
                "<script>\n" +
                "    goongjs.accessToken = 'zPQbMw6ayi3gBnjuvZAVeNl7lW6xwn8AS7AX8gXO';\n" +
                "    var map = new goongjs.Map({\n" +
                "        container: 'map',\n" +
                "        style: 'https://tiles.goong.io/assets/goong_map_web.json',\n" +
                "        center: [" + longitudeStart + ", " + latitudeStart + "],\n" +
                "        zoom: 17,\n" +
                "        touchZoomRotate: true,\n"+
                "        dragPan: true,\n" +
                "        scrollZoom: true,\n" +
                "        doubleClickZoom: true,\n"+
                "        dragRotate: true,\n"+
                "    });\n" +

                "    map.on('load', function () {\n" +
                "        var layers = map.getStyle().layers;\n" +
                "        var firstSymbolId;\n" +
                "        for (var i = 0; i < layers.length; i++) {\n" +
                "            if (layers[i].type === 'symbol') {\n" +
                "                firstSymbolId = layers[i].id;\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +

                "        var encodedPolyline = \"" + encodedPolyline + "\";\n" +
                "        var geoJSON = polyline.toGeoJSON(encodedPolyline);\n" +

                "        map.addSource('route', {\n" +
                "            'type': 'geojson',\n" +
                "            'data': geoJSON\n" +
                "        });\n" +

                "        map.addLayer(\n" +
                "            {\n" +
                "                'id': 'route',\n" +
                "                'type': 'line',\n" +
                "                'source': 'route',\n" +
                "                'layout': {\n" +
                "                    'line-join': 'round',\n" +
                "                    'line-cap': 'round'\n" +
                "                },\n" +
                "                'paint': {\n" +
                "                    'line-color': '#1e88e5',\n" +
                "                    'line-width': 10\n" +
                "                }\n" +
                "            },\n" +
                "            firstSymbolId\n" +
                "        );\n" +

                "        new goongjs.Marker({ color: \"red\" })\n" +
                "            .setLngLat([" + longitudeStart + ", " + latitudeStart + "])\n" +
                "            .addTo(map);\n" +

                "        new goongjs.Marker({ color: \"red\" })\n" +
                "            .setLngLat([" + longitudeEnd + ", " + latitudeEnd + "])\n" +
                "            .addTo(map);\n" +
                "    });\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";


        String safeHTML = StringEscapeUtils.escapeJson(mapHTML);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MapDirectionResponse(
                        Math.toIntExact(shipmentDirection.getId()),
                        shipmentDirection.getShipment().getShipmentId(),
                        shipmentDirection.getOverviewPolyline(),
                        safeHTML,  // Đã escape HTML
                        detailResponses
                ));
    }


}
