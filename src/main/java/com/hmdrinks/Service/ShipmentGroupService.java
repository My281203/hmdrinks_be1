package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.UpdateTimeShipmentReq;
import com.hmdrinks.Response.*;
import jakarta.transaction.Transactional;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ShipmentGroupService {
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
    @Autowired
    private  ProductTranslationRepository productTranslationRepository;

//    @Transactional
//    public ResponseEntity<?> shipmentAllocation(CRUDShipmentReq req) {
//        Shippment shippment = shipmentRepository.findByShipmentIdAndIsDeletedFalse(req.getShipmentId());
//        if (shippment == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
//        }
//        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
//        }
//        if (user.getRole() != Role.SHIPPER) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not shipper");
//        }
//        shippment.setDateDelivered(req.getDateDeliver());
//        shippment.setDateShip(req.getDateShip());
//        shippment.setUser(user);
//        shipmentRepository.save(shippment);
//
//        Payment payment = paymentRepository.findByPaymentId(shippment.getPayment().getPaymentId());
//        if (payment.getIsDeleted()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment is deleted");
//        }
//        Orders orders = orderRepository.findByOrderId(payment.getOrder().getOrderId());
//        if (orders.getIsDeleted()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order is deleted");
//        }
//
//        User customer = userRepository.findByUserIdAndIsDeletedFalse(orders.getUser().getUserId());
//        if (customer == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer Not Found");
//        }
//        System.out.println("User ID: " + shippment.getUser().getUserId());
//
//        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
//                shippment.getShipmentId(),
//                shippment.getUser() != null ? shippment.getUser().getFullName() : null,
//                shippment.getDateCreated(),
//                shippment.getDateDeleted(),
//                shippment.getDateDelivered(),
//                shippment.getDateShip(),
//                shippment.getDateCancel(),
//                shippment.getIsDeleted(),
//                shippment.getStatus(),
//                shippment.getPayment().getPaymentId(),
//                shippment.getUser() != null ? shippment.getUser().getUserId() : null,
//                customer.getFullName(),
//                customer.getUserId(),
//                orders.getAddress(),
//                customer.getPhoneNumber(),
//                customer.getEmail(),
//                orders.getOrderId()
//        ));
//    }

    @Transactional
    public ResponseEntity<?> activateShipment(int shipmentId, int userId)
    {
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
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
        shipmentGroupRepository.save(shippment);
        PaymentGroup payment = paymentGroupRepository.findByPaymentId(shippment.getPayment().getPaymentId());


        List<GroupOrderMember> groupOrderMembers = payment.getGroupOrder().getGroupOrderMembers();
        for(GroupOrderMember groupOrderMember : groupOrderMembers)
        {
            CartGroup cart = groupOrderMember.getCartGroup();
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
        }



        User customer = userRepository.findByUserIdAndIsDeletedFalse(payment.getGroupOrder().getUser().getUserId());

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
                payment.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                payment.getGroupOrder().getGroupOrderId()
        ));
    }

    @Transactional
    public ResponseEntity<?> cancelShipment(int shipmentId)
    {
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        if(shippment.getStatus() != Status_Shipment.SHIPPING && shippment.getStatus() != Status_Shipment.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad request");
        }


        if(LocalDateTime.now().isAfter(shippment.getDateDelivered()))
        {
                    shippment.setDateCancel(LocalDateTime.now());
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shipmentGroupRepository.save(shippment);
                    PaymentGroup payment = shippment.getPayment();


                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentGroupRepository.save(payment);

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
                        paymentGroupRepository.save(payment);

                    }
                    List<GroupOrderMember> groupOrderMembers = payment.getGroupOrder().getGroupOrderMembers();
                    for(GroupOrderMember groupOrderMember: groupOrderMembers)
                    {
                        CartGroup cart = groupOrderMember.getCartGroup();
                        List<CartItemGroup> cartItems = cart.getCartItems();

                        for (CartItemGroup cartItem : cartItems) {
                            ProductVariants productVariants = cartItem.getProductVariants();
                            productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                            productVariantsRepository.save(productVariants);

                    }



            }

               return ResponseEntity.ok().build();
        }

        shippment.setStatus(Status_Shipment.CANCELLED);
        shippment.setDateCancel(LocalDateTime.now());
        shipmentGroupRepository.save(shippment);

        PaymentGroup payment = paymentGroupRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
        {
            payment.setStatus(Status_Payment.REFUND);
            payment.setDateRefunded(LocalDateTime.now());
            payment.setIsRefund(false);
            if(payment.getAmount() == 0.0)
            {
                payment.setIsRefund(true);
            }
            paymentGroupRepository.save(payment);

        }
        else{
            payment.setStatus(Status_Payment.FAILED);
            paymentGroupRepository.save(payment);
            GroupOrders groupOrders = payment.getGroupOrder();
            groupOrders.setStatus(StatusGroupOrder.CANCELED);
            groupOrders.setDateUpdated(LocalDateTime.now());

        }

        User customer = userRepository.findByUserId(payment.getGroupOrder().getUser().getUserId());
        User shipper = shippment.getUser();
        // Gửi thông báo đến khách hàng
        try {
            String message = "Đơn hàng của bạn đã bị hủy";
            Integer userId = payment.getGroupOrder().getUser().getUserId();
            notificationService.sendNotification(userId, payment.getGroupOrder().getGroupOrderId(), message);
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
                payment.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                payment.getGroupOrder().getGroupOrderId()
        ));
    }


    @Transactional
    public ResponseEntity<?> activate_Admin(int shipmentId, Status_Shipment statusShipment)
    {
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        shippment.setStatus(statusShipment);
        if(statusShipment == Status_Shipment.CANCELLED)
        {
            shippment.setDateCancel(LocalDateTime.now());
            shipmentGroupRepository.save(shippment);
        }
        shipmentGroupRepository.save(shippment);
        if(statusShipment == Status_Shipment.SUCCESS)
        {
            shippment.setDateShip(LocalDateTime.now());
            shipmentGroupRepository.save(shippment);
            PaymentGroup payment = shippment.getPayment();
            if(payment.getPaymentMethod() == Payment_Method.CASH)
            {
                payment.setStatus(Status_Payment.COMPLETED);
                paymentGroupRepository.save(payment);
            }



        }

        PaymentGroup payment = paymentGroupRepository.findByPaymentId(shippment.getPayment().getPaymentId());
        User customer = payment.getGroupOrder().getUser();

        if(statusShipment == Status_Shipment.CANCELLED) {
            List<GroupOrderMember> groupOrderMembers = payment.getGroupOrder().getGroupOrderMembers();
            for(GroupOrderMember groupOrderMember : groupOrderMembers)
            {
                CartGroup cart = groupOrderMember.getCartGroup();
                List<CartItemGroup> cartItems = cart.getCartItems();

                for (CartItemGroup cartItem : cartItems) {
                    ProductVariants productVariants = cartItem.getProductVariants();
                    productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                    productVariantsRepository.save(productVariants);
                }

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
                paymentGroupRepository.save(payment);
            }
            if (payment.getPaymentMethod() == Payment_Method.CASH
            ) {
                payment.setStatus(Status_Payment.FAILED);
                paymentGroupRepository.save(payment);
                GroupOrders groupOrders = payment.getGroupOrder();
                groupOrders.setStatus(StatusGroupOrder.CANCELED);
                groupOrders.setDateUpdated(LocalDateTime.now());
                groupOrdersRepository.save(groupOrders);
            }


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
                payment.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(), payment.getGroupOrder().getGroupOrderId()
        ));
    }


    @Transactional
    public ResponseEntity<?> successShipment(int shipmentId,int userId)
    {
        ShippmentGroup shippment = shipmentGroupRepository.findByUserUserIdAndShipmentId(userId,shipmentId);
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
        shipmentGroupRepository.save(shippment);

        PaymentGroup payment1 = shippment.getPayment();
        if(payment1.getPaymentMethod() == Payment_Method.CASH)
        {
            payment1.setStatus(Status_Payment.COMPLETED);
            paymentGroupRepository.save(payment1);
            GroupOrders groupOrders = payment1.getGroupOrder();
            groupOrders.setStatus(StatusGroupOrder.COMPLETED);
            groupOrders.setDateUpdated(LocalDateTime.now());
            groupOrdersRepository.save(groupOrders);
        }


        User customer = payment1.getGroupOrder().getUser();

        // Gửi thông báo đến khách hàng
        try {
            System.out.print("Giao đơn thành công");
            String message = "Đơn hàng của bạn đã được giao thành công";
            notificationService.sendNotification(customer.getUserId(),payment1.getGroupOrder().getGroupOrderId(), message);
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
                payment1.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(), payment1.getGroupOrder().getGroupOrderId()
        ));
    }

    /// fix

    @Transactional
    public ResponseEntity<?> getListShipmentStatusByShipper(String pageFromParam, String limitFromParam, int userId, Status_Shipment status) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Sort sort = switch (status) {
            case CANCELLED -> Sort.by(Sort.Direction.DESC, "dateCancel");
            case SUCCESS -> Sort.by(Sort.Direction.DESC, "dateShip");
            case SHIPPING, WAITING -> Sort.by(Sort.Direction.DESC, "dateCreated");
            default -> Sort.unsorted();
        };

        User shipper_check = userRepository.findByUserId(userId);
        if(shipper_check.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }

        Pageable pageable = (sort.isUnsorted())
                ? PageRequest.of(page - 1, limit)
                : PageRequest.of(page - 1, limit, sort);

        List<ShippmentGroup> shippments = shipmentGroupRepository.findAllByUserIdAndStatusFetchAll(userId, status, pageable);
        int totalRecords = shipmentGroupRepository.countByUserIdAndStatus(userId, status);
        int totalPages = (int) Math.ceil((double) totalRecords / limit);

        List<CRUDShipmentResponse> responses = shippments.stream().map(shippment -> {
            PaymentGroup payment = shippment.getPayment();
            User customer = payment.getGroupOrder().getUser();
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
                    payment.getGroupOrder().getAddress(),
                    customer.getPhoneNumber(),
                    customer.getEmail(),
                    payment.getGroupOrder().getGroupOrderId()
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


    @Transactional
    public ResponseEntity<?> getListAllShipmentByShipper(String pageFromParam, String limitFromParam, int userId) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        User shipper_check = userRepository.findByUserId(userId);
        if(shipper_check.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        List<ShippmentGroup> shippments = shipmentGroupRepository.findAllByUserIdFetchAll(userId, pageable);
        int totalRecords = shipmentGroupRepository.countByShipper(userId);
        int totalPages = (int) Math.ceil((double) totalRecords / limit);

        List<CRUDShipmentResponse> responses = shippments.stream().map(shippment -> {
            PaymentGroup payment = shippment.getPayment();

            User customer = payment.getGroupOrder().getUser();
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
                    payment.getGroupOrder().getAddress(),
                    customer.getPhoneNumber(),
                    customer.getEmail(),
                    payment.getGroupOrder().getGroupOrderId()
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


    @Transactional
    public ResponseEntity<?> getListAllShipment(String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);

        if (limit >= 100) limit = 100;
        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit, sort);


        Page<ShippmentGroup> shippments = shipmentGroupRepository.findAllWithDetails(pageable);
        List<CRUDShipmentResponse> responses = new ArrayList<>();

        for (ShippmentGroup shippment : shippments) {
            User shipper = shippment.getUser();
            Integer shipperId = (shipper != null) ? shipper.getUserId() : null;
            PaymentGroup payment = shippment.getPayment();

            User customer = payment != null ? payment.getGroupOrder().getUser() : null;

            // Ensure payment and associated order exist
            if (payment == null || payment.getGroupOrder() == null || customer == null) {
                continue;
            }

            String customerFullName = customer != null ? customer.getFullName() : "Unknown Customer";
            String customerAddress = customer != null
                    ? (customer.getStreet() + ", " + customer.getWard() + ", " + customer.getDistrict() + ", " + customer.getCity())
                    : "Unknown Address";
            String customerPhone = customer != null ? customer.getPhoneNumber() : "Unknown Phone";
            String customerEmail = customer != null ? customer.getEmail() : "Unknown Email";

            String nameShipper = (shipper != null) ? shipper.getFullName() : "Unknown Shipper";

            CRUDShipmentResponse response = new CRUDShipmentResponse(
                    shippment.getShipmentId(),
                    nameShipper,
                    shippment.getDateCreated(),
                    shippment.getDateDeleted(),
                    shippment.getDateDelivered(),
                    shippment.getDateShip(),
                    shippment.getDateCancel(),
                    shippment.getIsDeleted(),
                    shippment.getStatus(),
                    shippment.getNote(),
                    payment.getPaymentId(),
                    shipperId,  // Use shipperId, it can be null
                    customerFullName,
                    customer.getUserId(),
                    customerAddress,
                    customerPhone,
                    customerEmail,
                    shippment.getPayment().getGroupOrder().getGroupOrderId()
            );

            responses.add(response);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllScheduledShipmentsResponse(
                page,
                shippments.getTotalPages(),
                limit,
                shippments.getSize(),
                responses
        ));
    }


    @Transactional
    public ResponseEntity<?> getListAllShipmentByStatus(String pageFromParam, String limitFromParam, Status_Shipment status) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Sort sort = switch (status) {
            case CANCELLED -> Sort.by(Sort.Direction.DESC, "dateCancel");
            case SUCCESS -> Sort.by(Sort.Direction.DESC, "dateShip");
            default -> Sort.by(Sort.Direction.DESC, "dateCreated");
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        List<ShippmentGroup> shippments = shipmentGroupRepository.findAllByStatusFetchAll(status, pageable);

        List<CRUDShipmentResponse> responses = shippments.stream().map(shippment -> {
            PaymentGroup payment = shippment.getPayment();

            User customer = payment.getGroupOrder().getUser();
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
                    payment.getGroupOrder().getAddress(),
                    customer.getPhoneNumber(),
                    customer.getEmail(),
                    payment.getGroupOrder().getGroupOrderId()
            );
        }).toList();

        return ResponseEntity.status(HttpStatus.OK).body(
                new ListAllScheduledShipmentsResponse(
                        page,
                        0, // pagination total page sẽ cần thêm custom nếu bạn cần
                        limit,
                        responses.size(),
                        responses
                )
        );
    }



    public boolean isShipmentDatesValid(ShippmentGroup shipment, UpdateTimeShipmentReq updateRequest) {
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
         ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(req.getShipmentId());
         if(shippment == null)
         {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment not found");
         }

        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
         boolean check = isShipmentDatesValid(shippment,req);
         if(!check)
         {
             return ResponseEntity.status(HttpStatus.CONFLICT).body("Shipment date is not valid");
         }
         shippment.setDateDelivered(req.getDateDelivered());
         shippment.setDateShip(req.getDateShipped());
         shipmentGroupRepository.save(shippment);
        PaymentGroup payment = shippment.getPayment();

        User customer = payment.getGroupOrder().getUser();
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
                 payment.getGroupOrder().getAddress(),
                 customer.getPhoneNumber(),
                 customer.getEmail(),
                 payment.getGroupOrder().getGroupOrderId()
         ));
    }

//    @Transactional
//    public ResponseEntity<?> getInfoShipmentByGroupOrderId(int orderId)
//    {
//
//        GroupOrders groupOrders = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(orderId);
//        if(groupOrders == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
//        }
//        Payment payment = groupOrders.get
//        if(payment != null)
//        {
//
//        }
//        User customer = order.getUser();
//        Shippment shipment = shipmentRepository.findByPaymentPaymentIdAndIsDeletedFalse(payment.getPaymentId());
//        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentResponse(
//                shipment != null ?shipment.getShipmentId() : null,
//                shipment != null && shipment.getUser() != null ? shipment.getUser().getFullName() : null,
//
//                shipment != null ? shipment.getDateCreated(): null,
//                shipment != null ? shipment.getDateDeleted(): null,
//                shipment != null ? shipment.getDateDelivered(): null,
//                shipment != null ? shipment.getDateShip(): null,
//                shipment != null ? shipment.getDateCancel(): null,
//                shipment != null ? shipment.getIsDeleted(): null,
//                shipment != null ? shipment.getStatus(): null,
//                shipment != null ? shipment.getPayment().getPaymentId(): null,
//                shipment != null && shipment.getUser() != null  ? shipment.getUser().getUserId() : null,
//                customer.getFullName(),
//                customer.getUserId(),
//                order.getAddress(),
//                customer.getPhoneNumber(),
//                customer.getEmail(),
//                order.getOrderId()
//        ));
//    }

    @Transactional
    public ResponseEntity<?> checkTimeDelivery(){
        List<ShippmentGroup> shippmentList = shipmentGroupRepository.findAll();
        LocalDateTime now  = LocalDateTime.now();
        for(ShippmentGroup shippment : shippmentList)
        {
            if(shippment.getUser() == null && shippment.getStatus() == Status_Shipment.WAITING)
            {
                LocalDateTime time_create = shippment.getDateCreated().plusHours(1);
                if(now.isAfter(time_create))
                {
                    PaymentGroup payment = shippment.getPayment();

                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentGroupRepository.save(payment);

                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentGroupRepository.save(payment);

                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentGroupRepository.save(shippment);
                }
            }
            if( shippment.getStatus() == Status_Shipment.WAITING)
            {
                LocalDateTime time_create = shippment.getDateCreated().plusHours(1);
                if(now.isAfter(time_create))
                {
                    PaymentGroup payment = shippment.getPayment();

                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentGroupRepository.save(payment);

                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentGroupRepository.save(payment);

                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentGroupRepository.save(shippment);
                }
            }
            if(shippment.getStatus() == Status_Shipment.SHIPPING && shippment.getUser() != null)
            {
                if(now.isAfter(shippment.getDateDelivered())) {
                    PaymentGroup payment = shippment.getPayment();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentGroupRepository.save(payment);

                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentGroupRepository.save(payment);

                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentGroupRepository.save(shippment);
                }
            }
            if(shippment.getStatus() == Status_Shipment.SHIPPING)
            {
                if(now.isAfter(shippment.getDateDelivered())) {
                    PaymentGroup payment = shippment.getPayment();
                    if(payment.getPaymentMethod() == Payment_Method.CASH)
                    {
                        payment.setStatus(Status_Payment.FAILED);
                        paymentGroupRepository.save(payment);

                    }
                    if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
                    {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentGroupRepository.save(payment);

                    }
                    shippment.setStatus(Status_Shipment.CANCELLED);
                    shippment.setDateCancel(LocalDateTime.now());
                    shipmentGroupRepository.save(shippment);
                }
            }
        }
        return ResponseEntity.ok().build();
    }


    @Transactional
    public ResponseEntity<?> getOneShipment(int shipmentId){
        ShippmentGroup shipment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shipment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PaymentGroup payment = shipment.getPayment();
        User customer = payment.getGroupOrder().getUser();

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
                payment.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                payment.getGroupOrder().getGroupOrderId()
        ));
    }




    @Transactional
    public ResponseEntity<?> ActivateReceiving(int shipmentId, int userId)
    {
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if(shippment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
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
        shipmentGroupRepository.save(shippment);
        PaymentGroup payment = shippment.getPayment();

        User customer = payment.getGroupOrder().getUser();
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
                payment.getGroupOrder().getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                payment.getGroupOrder().getGroupOrderId()
        ));
    }

    @Transactional()
    public ResponseEntity<?> getListShipmentStatusWaitingByUserId(int userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        List<ShippmentGroup> waitingShipments = shipmentGroupRepository.findWaitingShipmentsByUserId(userId);

        List<CRUDShipmentResponse> responses = waitingShipments.stream()
                .map(shipment -> {
                    PaymentGroup payment = shipment.getPayment();

                    User customer = payment.getGroupOrder().getUser();
                    User shipper = shipment.getUser();


                    if (payment.getGroupOrder().getStatus() != StatusGroupOrder.COMPLETED) return null;
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
                            payment.getGroupOrder().getAddress(),
                            customer.getPhoneNumber(),
                            customer.getEmail(),
                            payment.getGroupOrder().getGroupOrderId()
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
        Page<ShippmentGroup> shipments = shipmentGroupRepository.searchByShipperName(keyword, pageable);
        List<CRUDShipmentResponse> responses = new ArrayList<>();

        for (ShippmentGroup shipment : shipments) {
            User shipper = shipment.getUser();
            Integer shipperId = (shipper != null) ? shipper.getUserId() : null;
            String shipperName = (shipper != null) ? shipper.getFullName() : "Unknown Shipper";

            PaymentGroup payment = shipment.getPayment();
            if (payment == null || payment.getGroupOrder() == null) {
                continue;
            }


            User customer = payment.getGroupOrder().getUser();

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
                    shipment.getDateCreated(),
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
                    payment.getGroupOrder().getGroupOrderId()
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
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if (shippment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        if (Objects.equals(note, ""))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Note is empty");
        }
        User shipper = shippment.getUser();
        if(shipper.getIsDeleted() == Boolean.TRUE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipper is deleted");
        }
        shippment.setNote(note);
        shipmentGroupRepository.save(shippment);
        return ResponseEntity.status(HttpStatus.OK).body("Success");

    }


    @Transactional
    public ResponseEntity<?> getMapdirection(int shipmentId) {
        ShippmentGroup shippment = shipmentGroupRepository.findByShipmentIdAndIsDeletedFalse(shipmentId);
        if (shippment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found");
        }
        List<CRUDStepDetailResponse> detailResponses = new ArrayList<>();
        ShipmentDirection shipmentDirection = shippment.getShipmentDirection();
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
                        shipmentDirection.getShipmentGroup().getShipmentId(),
                        shipmentDirection.getOverviewPolyline(),
                        safeHTML,
                        detailResponses
                ));
    }


    private String extractFirstImageUrl(String listProImg) {
        if (listProImg != null && !listProImg.trim().isEmpty()) {
            String[] imageEntries = listProImg.split(", ");
            if (imageEntries.length > 0) {
                String[] parts = imageEntries[0].split(": ");
                if (parts.length == 2) {
                    return parts[1];
                }
            }
        }
        return null;
    }
    @Transactional
    public ResponseEntity<?> getInfoShipmentByGroupOrderId(int groupOrderId,Language language)
    {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        PaymentGroup payment = null;
        List<PaymentGroup> paymentGroups = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndIsDeletedFalse(groupOrderId);
        for(PaymentGroup paymentGroup : paymentGroups) {
            if(paymentGroup.getPaymentMethod() == Payment_Method.CASH)
            {
                payment = paymentGroup;
            }
            else
            {
                if(paymentGroup.getStatus() == Status_Payment.COMPLETED)
                {
                    payment = paymentGroup;
                }
            }

        }

        List<CRUDCartGroupResponse> cartGroupResponseArrayList = new ArrayList<>();
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrderId);



        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;


            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartGroup.getCartId());
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        String proname = "";
                        if (language == Language.VN) {
                            proname = cartItem.getProductVariants().getProduct().getProName();
                        } else {
                            Product product = cartItem.getProductVariants().getProduct();
                            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                            proname = productTranslation.getProName();
                        }
                        cartItemResponses.add(new CRUDCartItemGroupResponse(
                                cartItem.getCartItemId(),
                                cartItem.getProductVariants().getProduct().getProId(),
                                proname,
                                cartItem.getCartGroup().getCartId(),
                                cartItem.getProductVariants().getSize(),
                                cartItem.getProductVariants().getPrice(),
                                cartItem.getTotalPrice(),
                                cartItem.getQuantity(),
                                extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg())
                        ));
                    }

                }

                crudCartGroupResponse = new CRUDCartGroupResponse(
                        cartGroup.getCartId(),
                        cartGroup.getGroupOrderMember().getGroupOrder().getGroupOrderId(),
                        cartGroup.getUser().getUserId(),
                        cartGroup.getGroupOrderMember().getMemberId(),
                        cartGroup.getTotalPrice(),
                        cartGroup.getTotalProduct(),
                        cartItemResponses
                );

                cartGroupResponseArrayList.add(crudCartGroupResponse);

            }



        }
        User customer = groupOrder.getUser();
        ShippmentGroup shipment = payment.getShipment();
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDShipmentGroupViewResponse(
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
                groupOrder.getAddress(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                groupOrder.getGroupOrderId(),
                cartGroupResponseArrayList
        ));
    }


}
