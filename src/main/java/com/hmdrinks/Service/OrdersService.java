package com.hmdrinks.Service;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.AddItemOrderConfirmRequest;
import com.hmdrinks.Request.CreateOrdersReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.DistanceAndDuration;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class OrdersService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserVoucherRepository userVoucherRepository;
    @Autowired
    private  CartRepository cartRepository;
    @Autowired
    private  PaymentRepository paymentRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private  ShipmentRepository shipmentRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private UserCointRepository userCointRepository;

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
    private ShipperDetailRepository shipperDetailRepository;


    public boolean isInWorkingHours() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(8, 0);      // 8:00 AM
        LocalTime end = LocalTime.of(21, 30);      // 9:30 PM
        return !now.isBefore(start) && !now.isAfter(end);
    }



    public boolean isNumeric(String voucherId) {
        if (voucherId == null) {
            return false;
        }
        try {
            Integer.parseInt(voucherId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    public boolean isVoucherValid(Voucher voucher) {
        LocalDateTime startDate = voucher.getStartDate();
        LocalDateTime endDate = voucher.getEndDate();
        LocalDateTime now = LocalDateTime.now();
        return (now.isEqual(startDate) || now.isAfter(startDate)) && (now.isEqual(endDate) || now.isBefore(endDate));
    }

    @Transactional
    public ResponseEntity<?> addOrder(CreateOrdersReq req) {
        // Step 1: Validate User and Cart
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");

        Cart cart = cartRepository.findByCartId(req.getCartId());
        if (cart == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found cart");

        if (cart.getStatus() == Status_Cart.COMPLETED) return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not allowed to add order");

        List<CartItem> cartItems = cart.getCartItems();
        List<String> errorMessages = new ArrayList<>();

        // Step 2: Validate Stock for each CartItem
        for (CartItem item : cartItems) {
            int requestedQty = item.getQuantity();
            int availableStock = item.getProductVariants().getStock();

            if (availableStock == 0 || requestedQty > availableStock) {
                String productName = req.getLanguage() == Language.VN
                        ? item.getProductVariants().getProduct().getProName()
                        : Optional.ofNullable(productTranslationRepository.findByProduct_ProId(item.getProductVariants().getProduct().getProId()))
                        .map(ProductTranslation::getProName)
                        .orElse("Unknown Product");

                item.setQuantity(0);
                item.setTotalPrice(0.0);
                cart.setTotalProduct(cart.getTotalProduct() - requestedQty);
                cart.setTotalPrice(cart.getTotalPrice() - requestedQty * item.getProductVariants().getPrice());

                errorMessages.add(String.format("Not enough product for %s, size %s. Requested: %d, Available: %d",
                        productName, item.getProductVariants().getSize(), requestedQty, availableStock));
            }
        }
        if (!errorMessages.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.join("; ", errorMessages));

        // Step 3: Validate User Coin
        UserCoin userCoin = userCointRepository.findByUserUserId(req.getUserId());
        float pointCoin = (req.getPointCoinUse() < 0) ? 0 : Math.min(req.getPointCoinUse(), (float) cart.getTotalPrice());
        if (userCoin != null && userCoin.getPointCoin() < pointCoin) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User coin exceed");
        }

        // Step 4: Validate Voucher
        Voucher voucher = null;
        if (isNumeric(req.getVoucherId())) {
            UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(req.getUserId(), Integer.parseInt(req.getVoucherId()));
            if (userVoucher == null || userVoucher.getStatus() != Status_UserVoucher.INACTIVE)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or used voucher");

            voucher = voucherRepository.findByVoucherIdAndIsDeletedFalse(userVoucher.getVoucher().getVoucherId());
            if (voucher == null || voucher.getStatus() == Status_Voucher.EXPIRED)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired or deleted");

            if (!isVoucherValid(voucher)) {
                voucher.setStatus(Status_Voucher.EXPIRED);
                voucherRepository.save(voucher);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired");
            }

            userVoucher.setStatus(Status_UserVoucher.USED);
            userVoucherRepository.save(userVoucher);
        }

        // Step 5: Check duplicate order
        if (orderItemRepository.findByUserUserIdAndCartCartId(req.getUserId(), req.getCartId()) != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cart already exists");

        // Step 6: Create Order
        if (Stream.of(user.getStreet(), user.getWard(), user.getDistrict(), user.getCity()).anyMatch(String::isEmpty))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Please update address before add order");

        String address = String.join(", ", user.getStreet(), user.getWard(), user.getDistrict(), user.getCity());
        double[] destination = supportFunction.getCoordinates(supportFunction.getLocation(address));
        double distance = supportFunction.getShortestDistance(new double[]{10.850575879000075,106.77190192800003}, destination).getDistance();
        if (distance > 20) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");

        Orders order = new Orders();
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(address);
        order.setLatitude(destination[0]);
        order.setLongitude(destination[1]);
        order.setPhoneNumber(user.getPhoneNumber());
        order.setStatus(Status_Order.WAITING);
        order.setUser(user);
        order.setDeliveryFee(calculateFee(distance));
        order.setDiscountPrice(voucher != null ? voucher.getDiscount() : 0.0);
        order.setPointCoinUse(pointCoin);
        order.setNote(req.getNote());
        order.setDateCreated(LocalDateTime.now());
        order.setDeliveryDate(LocalDateTime.now());
        order.setIsDeleted(false);

        Orders savedOrder = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setUser(user);
        orderItem.setCart(cart);
        orderItem.setDateCreated(LocalDateTime.now());
        orderItem.setIsDeleted(false);
        orderItem.setQuantity(cart.getTotalProduct());
        orderItem.setTotalPrice(cart.getTotalPrice());
        orderItemRepository.save(orderItem);

        savedOrder.setOrderItem(orderItem);
        savedOrder.setTotalPrice(orderItem.getTotalPrice());
        orderRepository.save(savedOrder);

        if (userCoin != null) {
            userCoin.setPointCoin(userCoin.getPointCoin() - pointCoin);
            userCointRepository.save(userCoin);
        }

        cart.setStatus(Status_Cart.COMPLETED);
        cartRepository.save(cart);

        // Step 7: Update product stock & handle out-of-stock logic
        for (CartItem item : cartItems) {
            ProductVariants variant = item.getProductVariants();
            int remaining = variant.getStock() - item.getQuantity();
            if (remaining > 0) {
                variant.setStock(remaining);
                productVariantsRepository.save(variant);
            }
            if (remaining == 0) {
                    handleOutOfStock(variant);
                }

        }

        // Step 8: Build response
        List<CRUDCartItemResponse> responseItems = cartItems.stream().map(item -> toCRUDCartItemResponse(item, req.getLanguage())).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(new CreateOrdersResponse(
                savedOrder.getOrderId(),
                savedOrder.getAddress(),
                savedOrder.getDeliveryFee(),
                savedOrder.getDateCreated(),
                savedOrder.getDateDeleted(),
                savedOrder.getDateUpdated(),
                savedOrder.getDeliveryDate(),
                savedOrder.getDateCanceled(),
                savedOrder.getDiscountPrice(),
                savedOrder.getIsDeleted(),
                savedOrder.getNote(),
                savedOrder.getOrderDate(),
                savedOrder.getPhoneNumber(),
                savedOrder.getStatus(),
                savedOrder.getTotalPrice(),
                savedOrder.getUser().getUserId(),
                voucher != null ? voucher.getVoucherId() : null,
                savedOrder.getPointCoinUse(),
                responseItems
        ));
    }


    private CRUDCartItemResponse toCRUDCartItemResponse(CartItem item, Language lang) {
        String proName = lang == Language.VN ? item.getProductVariants().getProduct().getProName() :
                Optional.ofNullable(productTranslationRepository.findByProduct_ProId(item.getProductVariants().getProduct().getProId()))
                        .map(ProductTranslation::getProName)
                        .orElseGet(() -> {
                            ProductTranslation trans = new ProductTranslation();
                            trans.setProduct(item.getProductVariants().getProduct());
                            trans.setIsDeleted(false);
                            trans.setDateCreated(LocalDateTime.now());
                            trans.setProName(supportFunction.convertLanguage(item.getProductVariants().getProduct().getProName(), Language.EN));
                            trans.setDescription(supportFunction.convertLanguage(item.getProductVariants().getProduct().getDescription(), Language.EN));
                            return productTranslationRepository.save(trans).getProName();
                        });

        String firstImage = Optional.ofNullable(item.getProductVariants().getProduct().getListProImg())
                .map(img -> img.split(", "))
                .filter(arr -> arr.length > 0 && arr[0].contains(": "))
                .map(arr -> arr[0].split(": "))
                .filter(parts -> parts.length == 2)
                .map(parts -> parts[1])
                .orElse(null);

        return new CRUDCartItemResponse(
                item.getCartItemId(),
                item.getProductVariants().getProduct().getProId(),
                proName,
                item.getCart().getCartId(),
                item.getProductVariants().getSize(),
                item.getTotalPrice(),
                item.getQuantity(),
                firstImage
        );
    }

    private void recalculateCart(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
        double total = 0.0;
        int quantity = 0;
        for (CartItem item : items) {
            total += item.getTotalPrice();
            quantity += item.getQuantity();
        }
        cart.setTotalPrice(total);
        cart.setTotalProduct(quantity);
        cartRepository.save(cart);
    }

    private void recalculateCartGroup(CartGroup cartGroup) {
        List<CartItemGroup> items = cartItemGroupRepository.findByCartGroupCartIdAndIsDisabledFalse(cartGroup.getCartId());
        double total = 0.0;
        int quantity = 0;
        for (CartItemGroup item : items) {
            total += item.getTotalPrice();
            quantity += item.getQuantity();
        }

        cartGroup.setTotalPrice(total);
        cartGroup.setTotalProduct(quantity);
        cartGroup.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cartGroup);

        GroupOrderMember member = cartGroup.getGroupOrderMember();
        member.setAmount(total);
        member.setQuantity(quantity);
        member.setStatus(StatusGroupOrderMember.SHOPPING);
        member.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(member);
    }


    private void updateGroupOrderState(GroupOrders groupOrder) {
        GroupOrders group = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrder.getGroupOrderId());
        List<GroupOrderMember> members = group.getGroupOrderMembers();
        double total = 0.0;
        int quantity = 0;

        for (GroupOrderMember member : members) {
            total += (member.getAmount() != null) ? member.getAmount() : 0.0;
            quantity += (member.getQuantity() != null) ? member.getQuantity() : 0;
        }

        group.setTotalPrice(total);
        group.setTotalQuantity(quantity);
        group.setStatus(StatusGroupOrder.SHOPPING);
        group.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(group);
    }


    private void handleOutOfStock(ProductVariants variant) {
        // Cập nhật tất cả CartItem thông thường
        List<CartItem> cartItems = cartItemRepository.findByProductVariants_VarId(variant.getVarId());
        for (CartItem item : cartItems) {
            Cart cart = item.getCart();
            if (cart.getStatus() == Status_Cart.NEW) {
                item.setQuantity(0);
                item.setNote("Hiện đang hết hàng");
                item.setTotalPrice(0.0);
                cartItemRepository.save(item);
                recalculateCart(cart);
            }
        }

        List<CartItemGroup> cartItemGroups = cartItemGroupRepository.findByProductVariants_VarId(variant.getVarId());
        for (CartItemGroup itemGroup : cartItemGroups) {
            CartGroup cartGroup = itemGroup.getCartGroup();
            GroupOrders groupOrder = cartGroup.getGroupOrderMember().getGroupOrder();

            if (groupOrder != null && groupOrder.getStatus() != StatusGroupOrder.COMPLETED &&
                    groupOrder.getStatus() != StatusGroupOrder.CANCELED) {

                recalculateCartGroup(cartGroup);
                updateGroupOrderState(cartGroup.getGroupOrderMember().getGroupOrder());
            }
        }
    }





//    @Transactional
//    public ResponseEntity<?> addOrder(CreateOrdersReq req) {
//
//        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
//        }
//
//        Cart cart = cartRepository.findByCartId(req.getCartId());
//        if (cart == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found cart");
//        }
//
//        List<CartItem> cartItem_Check = cart.getCartItems();
//        for(CartItem cartItem : cartItem_Check) {
//            Integer quantity = cartItem.getQuantity();
//            Integer quantity_product_remain = cartItem.getProductVariants().getStock();
//            if(quantity_product_remain == 0)
//            {
//                cartItem.setQuantity(0);
//                cartItem.setTotalPrice(0.0);
//                cartItemRepository.save(cartItem);
//                Integer quantity_remain = cart.getTotalProduct() - quantity;
//                Double total_price = cart.getTotalPrice() - quantity * cartItem.getProductVariants().getPrice();
//                cart.setTotalPrice(total_price);
//                cart.setTotalProduct(quantity_remain);
//                cartRepository.save(cart);
//                String product_name = "";
//
//                if (req.getLanguage() == Language.VN)
//                {
//                    product_name = cartItem.getProductVariants().getProduct().getProName();
//                }
//                else{
//                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
//                    product_name = productTranslation.getProName();
//                }
//                String errorMessage = String.format(
//                        "Not enough product for %s, size %s. Requested quantity: %d, Available: %d",
//                        product_name, cartItem.getProductVariants().getSize(), quantity, 0
//                );
//
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
//            }
//            if(quantity_product_remain < quantity)
//            {
//                String product_name = "";
//
//                if (req.getLanguage() == Language.VN)
//                {
//                    product_name = cartItem.getProductVariants().getProduct().getProName();
//                }
//                else{
//                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
//                    product_name = productTranslation.getProName();
//                }
//
//                String errorMessage = String.format(
//                        "Not enough product for %s, size %s. Requested quantity: %d, Available: %d",
//                         product_name, cartItem.getProductVariants().getSize(), quantity, quantity_product_remain
//                );
//
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
//
//            }
//        }
//
//
//        if (cart.getStatus() == Status_Cart.COMPLETED) {
//            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not allowed to add order");
//        }
//        UserCoin userCoin = userCointRepository.findByUserUserId(req.getUserId());
//        boolean check_coin = false;
//        if(userCoin != null)
//        {
//            check_coin = true;
//            if(userCoin.getPointCoin() < req.getPointCoinUse())
//            {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User coin exceed");
//            }
//        }
//
//
//        UserVoucher userVoucher = null;
//        Voucher voucher = null;
//        if (isNumeric(req.getVoucherId())) {
//
//            userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(req.getUserId(), Integer.parseInt(req.getVoucherId()));
//            if (userVoucher == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userVoucher");
//            }
//            if (userVoucher.getStatus() == Status_UserVoucher.USED) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher already in use");
//            }
//            if (userVoucher.getStatus() == Status_UserVoucher.EXPIRED) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired");
//            }
//            voucher = voucherRepository.findByVoucherIdAndIsDeletedFalse(userVoucher.getVoucher().getVoucherId());
//            if (voucher == null || voucher.getStatus() == Status_Voucher.EXPIRED || voucher.getIsDeleted()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher is deleted");
//            }
//            boolean checkVoucher = isVoucherValid(voucher);
//            if (!checkVoucher) {
//                voucher.setStatus(Status_Voucher.EXPIRED);
//                voucherRepository.save(voucher);
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired");
//            }
//
//        }
//
//        OrderItem existingOrderItem = orderItemRepository.findByUserUserIdAndCartCartId(req.getUserId(), req.getCartId());
//        if (existingOrderItem != null) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cart already exists");
//        }
//
//        Orders order = new Orders();
//        order.setOrderDate(LocalDateTime.now());
//        if(Objects.equals(user.getStreet(), "") || Objects.equals(user.getWard(), "") || Objects.equals(user.getDistrict(), "") ||Objects.equals(user.getCity(), "") )
//        {
//            return ResponseEntity.status(HttpStatus.CONFLICT).body("Please update address before add order");
//        }
//        String address = user.getStreet() +  ", " + user.getWard() + ", " + user.getDistrict() + ", " + user.getCity();
//        order.setAddress(address);
//
//        String place_id = supportFunction.getLocation(address);
//        double[] destinations= supportFunction.getCoordinates(place_id);
//        double[] origins = {10.850575879000075,106.77190192800003};
//        // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
//        DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
//        double distance = distanceAndDuration.getDistance();
//        if(distance > 20){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
//        }
//        double fee = calculateFee(distance);
//        order.setDeliveryFee(fee);
//        order.setPhoneNumber(user.getPhoneNumber());
//        order.setStatus(Status_Order.WAITING);
//        order.setUser(user);
//        order.setDeliveryFee(fee);
//        order.setIsDeleted(false);
//        order.setLatitude(destinations[0]);
//        order.setLongitude(destinations[1]);
//
//        if (voucher != null) {
//            order.setVoucher(voucher);
//            order.setDiscountPrice(voucher.getDiscount());
//        } else {
//            order.setDiscountPrice(0.0);
//        }
//        float point_coin = 0;
//        if(!check_coin && req.getPointCoinUse() != 0)
//        {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please no enter point coin");
//        }
//        if(req.getPointCoinUse() < 0)
//        {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please do not enter a coin amount less than 0. ");
//        }
//        if(req.getPointCoinUse() >= cart.getTotalPrice())
//        {
//            point_coin = (float) cart.getTotalPrice();
//        }
//        else {
//            point_coin = (float) req.getPointCoinUse();
//        }
//
//        if(userCoin != null)
//
//        {
//            float point_coin_remain = 0;
//            point_coin_remain = userCoin.getPointCoin() - point_coin;
//            if(point_coin_remain < 0)
//            {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User coin exceed");
//            }
//            else {
//                userCoin.setPointCoin(point_coin_remain);
//                userCointRepository.save(userCoin);
//            }
//        }
//        orderRepository.save(order);
//        OrderItem orderItem = new OrderItem();
//        orderItem.setOrder(order);
//        orderItem.setUser(user);
//        orderItem.setCart(cart);
//        orderItem.setDateCreated(LocalDateTime.now());
//        orderItem.setIsDeleted(false);
//        orderItem.setQuantity(cart.getTotalProduct());
//        orderItem.setTotalPrice(cart.getTotalPrice());
//        orderItemRepository.save(orderItem);
//
//        order.setOrderItem(orderItem);
//        order.setDateCreated(LocalDateTime.now());
//        order.setDeliveryDate(LocalDateTime.now());
//        order.setNote(req.getNote());
//
//
//
//        order.setPointCoinUse(point_coin);
//        order.setTotalPrice(orderItem.getTotalPrice());
//        order.setDateCanceled(null);
//        order.setLatitude(destinations[0]);
//        order.setLongitude(destinations[1]);
//        orderRepository.save(order);
//
//        cart.setStatus(Status_Cart.COMPLETED);
//        cartRepository.save(cart);
//
//        if (userVoucher != null) {
//            userVoucher.setStatus(Status_UserVoucher.USED);
//            userVoucherRepository.save(userVoucher);
//        }
//        List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
//        List<CRUDCartItemResponse> crudCartItemResponses = cartItems.stream()
//                .map(cartItem -> {
//                    String productNameTrans = "";
//                    if (req.getLanguage() == Language.VN) {
//                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
//                    } else if (req.getLanguage() == Language.EN) {
//                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
//                                cartItem.getProductVariants().getProduct().getProId()
//                        );
//
//                        if (productTranslation != null) {
//                            productNameTrans = productTranslation.getProName();
//                        } else {
//                            ProductTranslation newTranslation = new ProductTranslation();
//                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
//                            newTranslation.setIsDeleted(false);
//                            newTranslation.setDateCreated(LocalDateTime.now());
//                            newTranslation.setProName(
//                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
//                            );
//                            newTranslation.setDescription(
//                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
//                            );
//
//                            productTranslationRepository.save(newTranslation);
//                            productNameTrans = newTranslation.getProName();
//                        }
//                    }
//                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
//                    String firstImageUrl = null;
//
//                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                        String[] imageEntries1 = currentProImg.split(", ");
//                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                            String[] parts = imageEntries1[0].split(": ");
//                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                            }
//                        }
//                    }
//                    return new CRUDCartItemResponse(
//                            cartItem.getCartItemId(),
//                            cartItem.getProductVariants().getProduct().getProId(),
//                            productNameTrans,
//                            cartItem.getCart().getCartId(),
//                            cartItem.getProductVariants().getSize(),
//                            cartItem.getTotalPrice(),
//                            cartItem.getQuantity(),
//                            firstImageUrl
//                    );
//                })
//                .collect(Collectors.toList());
//        List<CartItem> cartItems1 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
//
//        for (CartItem cartItem : cartItems1) {
//            ProductVariants productVariants = cartItem.getProductVariants();
//            if (productVariants.getStock() >= cartItem.getQuantity()) {
//                productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
//                productVariantsRepository.save(productVariants);
//                if (productVariants.getStock() == 0) {
//                    List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
//                    for (CartItem cartItemList1 : cartItemList) {
//                        Cart cart1 = cartItemList1.getCart();
//                        if (cart1.getStatus() == Status_Cart.NEW) {
//                            cartItemList1.setQuantity(0);
//                            cartItemList1.setNote("Hiện đang hết hàng");
//                            cartItemList1.setTotalPrice(0.0);
//                            cartItemRepository.save(cartItemList1);
//                        }
//                        List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
//                        double total = 0.0;
//                        int total_quantity = 0;
//                        for (CartItem cartItemList3 : cartItemList2) {
//                            total += cartItemList3.getTotalPrice();
//                            total_quantity += cartItemList3.getQuantity();
//                        }
//                        cart1.setTotalPrice(total);
//                        cart1.setTotalProduct(total_quantity);
//                        cartRepository.save(cart1);
//                    }
//                    List<CartItemGroup> cartItemList0 = cartItemGroupRepository.findByProductVariants_VarId(productVariants.getVarId());
//                    for (CartItemGroup cartItemList1 : cartItemList0) {
//                        CartGroup cart1 = cartItemList1.getCartGroup();
//                        GroupOrders groupOrders_check = cart1.getGroupOrderMember().getGroupOrder();
//                        if (groupOrders_check != null && groupOrders_check.getStatus() != StatusGroupOrder.COMPLETED && groupOrders_check.getStatus() != StatusGroupOrder.CANCELED) {
//                            List<CartItemGroup> cartItemList2 = cartItemGroupRepository.findByCartGroupCartIdAndIsDisabledFalse(cart1.getCartId());
//                            double total = 0.0;
//                            int total_quantity = 0;
//                            for (CartItemGroup cartItemList3 : cartItemList2) {
//                                total += cartItemList3.getTotalPrice();
//                                total_quantity += cartItemList3.getQuantity();
//                            }
//                            cart1.setTotalPrice(total);
//                            cart1.setTotalProduct(total_quantity);
//                            cart1.setDateUpdated(LocalDateTime.now());
//                            cartGroupRepository.save(cart1);
//                            GroupOrderMember groupOrderMember = cart1.getGroupOrderMember();
//                            groupOrderMember.setAmount(total);
//                            groupOrderMember.setQuantity(total_quantity);
//                            groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
//                            groupOrderMember.setDateUpdated(LocalDateTime.now());
//                            groupOrderMembersRepository.save(groupOrderMember);
//
//                            GroupOrders groupOrders = groupOrderMember.getGroupOrder();
//                            GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
//                            List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
//                            Double Price_Group = 0.0;
//                            Integer Quantity_Group = 0;
//                            for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
//                                if (groupOrderMember2.getAmount() != null) {
//                                    Price_Group += groupOrderMember2.getAmount();
//                                }
//                                if (groupOrderMember2.getQuantity() != null) {
//                                    Quantity_Group += groupOrderMember2.getQuantity();
//                                }
//                            }
//
//                            groupOrders1.setTotalQuantity(Quantity_Group);
//                            groupOrders1.setTotalPrice(Price_Group);
//                            groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
//                            groupOrders1.setDateUpdated(LocalDateTime.now());
//                            groupOrdersRepository.save(groupOrders1);
//                        }
//                    }
//                }
//            }
//
//
//        }
//
//        return ResponseEntity.status(HttpStatus.OK).body(new CreateOrdersResponse(
//                order.getOrderId(),
//                order.getAddress(),
//                order.getDeliveryFee(),
//                order.getDateCreated(),
//                order.getDateDeleted(),
//                order.getDateUpdated(),
//                order.getDeliveryDate(),
//                order.getDateCanceled(),
//                order.getDiscountPrice(),
//                order.getIsDeleted(),
//                order.getNote(),
//                order.getOrderDate(),
//                order.getPhoneNumber(),
//                order.getStatus(),
//                order.getTotalPrice(),
//                order.getUser().getUserId(),
//                voucher != null ? voucher.getVoucherId() : null,
//                (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
//                crudCartItemResponses
//        ));
//    }

    @Transactional
    public ResponseEntity<?> getOrderFromCartPause(int cartId,Language language)
    {
        Cart cart = cartRepository.findByCartId(cartId);
        if (cart == null) {
            return ResponseEntity.notFound().build();
        }
        if(cart.getStatus() != Status_Cart.COMPLETED_PAUSE)
        {
            return ResponseEntity.notFound().build();
        }
        OrderItem orderItem = cart.getOrderItem();
        if(orderItem == null)
        {
            return ResponseEntity.notFound().build();
        }
        Orders order = orderItem.getOrder();
        if(order == null)
        {
            return ResponseEntity.notFound().build();
        }
        List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
        List<CRUDCartItemResponse> crudCartItemResponses = cartItems.stream()
                .map(cartItem -> {
                    String productNameTrans = "";
                    if (language == Language.VN) {
                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
                    } else if (language == Language.EN) {
                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
                                cartItem.getProductVariants().getProduct().getProId()
                        );

                        if (productTranslation != null) {
                            productNameTrans = productTranslation.getProName();
                        } else {
                            ProductTranslation newTranslation = new ProductTranslation();
                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
                            newTranslation.setIsDeleted(false);
                            newTranslation.setDateCreated(LocalDateTime.now());
                            newTranslation.setProName(
                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
                            );
                            newTranslation.setDescription(
                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
                            );

                            productTranslationRepository.save(newTranslation);
                            productNameTrans = newTranslation.getProName();
                        }
                    }
                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
                    String firstImageUrl = null;

                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                        String[] imageEntries1 = currentProImg.split(", ");
                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
                            String[] parts = imageEntries1[0].split(": ");
                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
                            }
                        }
                    }
                    return new CRUDCartItemResponse(
                            cartItem.getCartItemId(),
                            cartItem.getProductVariants().getProduct().getProId(),
                            productNameTrans,
                            cartItem.getCart().getCartId(),
                            cartItem.getProductVariants().getSize(),
                            cartItem.getTotalPrice(),
                            cartItem.getQuantity(),
                            firstImageUrl
                    );
                })
                .collect(Collectors.toList());
        Voucher voucher = order.getVoucher();

        return ResponseEntity.status(HttpStatus.OK).body(new CreateOrdersResponse(
                order.getOrderId(),
                order.getAddress(),
                order.getDeliveryFee(),
                order.getDateCreated(),
                order.getDateDeleted(),
                order.getDateUpdated(),
                order.getDeliveryDate(),
                order.getDateCanceled(),
                order.getDiscountPrice(),
                order.getIsDeleted(),
                order.getNote(),
                order.getOrderDate(),
                order.getPhoneNumber(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getUser().getUserId(),
                (order.getVoucher()  != null) ? order.getVoucher().getVoucherId() : null,
                (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
                crudCartItemResponses
        ));
    }

    @Transactional
    public ResponseEntity<?> confirmCancelOrder(int orderId) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        Orders orders = orderRepository.findByOrderIdAndStatusAndIsDeletedFalse(orderId, Status_Order.WAITING);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order status waiting");
        }
        orders.setDateCanceled(LocalDateTime.now());
        order.setStatus(Status_Order.CANCELLED);
        orderRepository.save(order);
        UserCoin userCoin = userCointRepository.findByUserUserId(order.getUser().getUserId());
        if(userCoin != null)
        {
            float point_coint = orders.getPointCoinUse() + userCoin.getPointCoin();
            userCoin.setPointCoin(point_coint);
            userCointRepository.save(userCoin);
        }
        if(order.getVoucher() != null)
        {
            UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(
                    order.getUser().getUserId(), order.getVoucher().getVoucherId()
            );
            userVoucher.setStatus(Status_UserVoucher.INACTIVE);
            userVoucherRepository.save(userVoucher);
        }
//        OrderItem orderItem1 = order.getOrderItem();
//        if(orderItem1 != null)
//        {
//            orderItemRepository.delete(order.getOrderItem());
//            Cart cart = cartRepository.findByCartId(order.getOrderItem().getCart().getCartId());
//            cart.setStatus(Status_Cart.NEW);
//            cartRepository.save(cart);
//        }
        return ResponseEntity.status(HttpStatus.OK).body("Order has been canceled");
    }

    @Transactional
    public ResponseEntity<?> addUserCoinOrderPause(int orderId, float pointCoinUse) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);

        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        if (order.getStatus() != Status_Order.WAITING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order already in use");
        }

        UserCoin userCoin = userCointRepository.findByUserUserId(order.getUser().getUserId());

        if (userCoin == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User does not have a coin account");
        }

        float coin_old = order.getPointCoinUse();
        float total_coin = coin_old + userCoin.getPointCoin();


        if (pointCoinUse > total_coin) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User coin exceed");
        }


        if (pointCoinUse < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please do not enter a coin amount less than 0.");
        }


        float point_coin;
        if (pointCoinUse >= order.getOrderItem().getCart().getTotalPrice()) {
            point_coin = (float) order.getOrderItem().getCart().getTotalPrice();
        } else {
            point_coin = pointCoinUse;
        }

        float point_coin_remain = total_coin - point_coin;
        userCoin.setPointCoin(point_coin_remain);
        userCointRepository.save(userCoin);


        order.setPointCoinUse(point_coin);
        orderRepository.save(order);


        return ResponseEntity.status(HttpStatus.OK).body("add coin success");

    }


    @Transactional
    public ResponseEntity<?> addVoucherPauseOrder(Integer orderId, String voucherId) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        if (order.getStatus() != Status_Order.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order already in use");
        }
        if(Integer.parseInt(voucherId) == -1)
        {
            Voucher voucher_old = order.getVoucher();
            if(voucher_old != null)
            {

                    LocalDateTime now  = LocalDateTime.now();
                    if (voucher_old.getStartDate().isAfter(now)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher chưa có hiệu lực");
                    }

                    if (voucher_old.getEndDate().isBefore(now)) {
                        // Cập nhật trạng thái voucher thành EXPIRED
                        voucher_old.setStatus(Status_Voucher.EXPIRED);
                        voucherRepository.save(voucher_old);

                        // Cập nhật tất cả user_voucher liên quan
                        List<UserVoucher> userVouchers = userVoucherRepository.findByVoucherVoucherId(voucher_old.getVoucherId());
                        for (UserVoucher uv : userVouchers) {
                            uv.setStatus(Status_UserVoucher.EXPIRED);
                            userVoucherRepository.save(uv);
                        }

                    }
                    if(voucher_old.getStatus() != Status_Voucher.EXPIRED)
                    {
                        UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), voucher_old.getVoucherId());
                        userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                        userVoucherRepository.save(userVoucher);
                    }


                }

            order.setVoucher(null);
            order.setDiscountPrice(0);
            orderRepository.save(order);
            return ResponseEntity.status(HttpStatus.OK).body("add Voucher success");
        }

        Voucher voucher_old = order.getVoucher();
        if(voucher_old != null)
        {
            if(voucher_old.getVoucherId() != Integer.parseInt(voucherId))
            {
                LocalDateTime now  = LocalDateTime.now();
                if (voucher_old.getStartDate().isAfter(now)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher chưa có hiệu lực");
                }

                if (voucher_old.getEndDate().isBefore(now)) {
                    // Cập nhật trạng thái voucher thành EXPIRED
                    voucher_old.setStatus(Status_Voucher.EXPIRED);
                    voucherRepository.save(voucher_old);

                    // Cập nhật tất cả user_voucher liên quan
                    List<UserVoucher> userVouchers = userVoucherRepository.findByVoucherVoucherId(voucher_old.getVoucherId());
                    for (UserVoucher uv : userVouchers) {
                        uv.setStatus(Status_UserVoucher.EXPIRED);
                        userVoucherRepository.save(uv);
                    }

                }
                if(voucher_old.getStatus() != Status_Voucher.EXPIRED)
                {
                    UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), voucher_old.getVoucherId());
                    userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                    userVoucherRepository.save(userVoucher);
                }
            }
        }

        Cart cart_check = order.getOrderItem().getCart();
        UserVoucher userVoucher = null;
        Voucher voucher = null;
        if (isNumeric(voucherId)) {
            userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(order.getUser().getUserId(), Integer.parseInt(voucherId));
            if (userVoucher == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found userVoucher");
            }
            if (userVoucher.getStatus() == Status_UserVoucher.USED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher already in use");
            }
            if (userVoucher.getStatus() == Status_UserVoucher.EXPIRED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired");
            }
            voucher = voucherRepository.findByVoucherIdAndIsDeletedFalse(userVoucher.getVoucher().getVoucherId());

            // Lấy thời gian hiện tại
            LocalDateTime now = LocalDateTime.now();

// Kiểm tra xem voucher có còn hiệu lực không
            if (voucher.getStartDate().isAfter(now)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher chưa có hiệu lực");
            }

            if (voucher.getEndDate().isBefore(now)) {
                // Cập nhật trạng thái voucher thành EXPIRED
                voucher.setStatus(Status_Voucher.EXPIRED);
                voucherRepository.save(voucher);

                // Cập nhật tất cả user_voucher liên quan
                List<UserVoucher> userVouchers = userVoucherRepository.findByVoucherVoucherId(voucher.getVoucherId());
                for (UserVoucher uv : userVouchers) {
                    if (Objects.equals(uv.getUser().getUserId(), order.getUser().getUserId())) {
                        uv.setStatus(Status_UserVoucher.USED);
                        userVoucherRepository.save(uv);
                    }
                    else
                    {
                        uv.setStatus(Status_UserVoucher.EXPIRED);
                        userVoucherRepository.save(uv);
                    }



                }

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher đã hết hạn");
            }



            if (voucher == null || voucher.getStatus() == Status_Voucher.EXPIRED || voucher.getIsDeleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher is deleted");
            }
            boolean checkVoucher = isVoucherValid(voucher);
            if (!checkVoucher) {
                voucher.setStatus(Status_Voucher.EXPIRED);
                voucherRepository.save(voucher);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Voucher expired");
            }

        }
        if (cart_check.getStatus() == Status_Cart.COMPLETED_PAUSE) {
            if(voucher != null)
            {
                order.setDiscountPrice(voucher.getDiscount());
                order.setVoucher(voucher);
                orderRepository.save(order);
            }

        }
        return ResponseEntity.status(HttpStatus.OK).body("add Voucher success");


    }

    @Transactional
    public ResponseEntity<?> confirmTemporarilyPauseOrder(int orderId) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        if (order.getStatus() != Status_Order.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order already in use");
        }
        Cart cart_check = order.getOrderItem().getCart();
        if (cart_check.getStatus() == Status_Cart.COMPLETED_PAUSE) {
            order.setStatus(Status_Order.CONFIRMED);
            orderRepository.save(order);
            cart_check.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart_check);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Confirm order success");
    }

    @Transactional
    public ResponseEntity<?> confirmOrder(int orderId) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        if (order.getStatus() != Status_Order.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order already in use");
        }
        if (Duration.between(order.getOrderDate(), LocalDateTime.now()).toMinutes() > 5) {
            order.setStatus(Status_Order.CANCELLED);
            orderRepository.save(order);
            if(order.getVoucher() != null)
            {
                UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(
                        order.getUser().getUserId(), order.getVoucher().getVoucherId()
                );
                userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                userVoucherRepository.save(userVoucher);
            }

            OrderItem orderItem1 = order.getOrderItem();
            if(orderItem1 != null)
            {
                orderItemRepository.delete(order.getOrderItem());
                Cart cart = cartRepository.findByCartId(order.getOrderItem().getCart().getCartId());
                cart.setStatus(Status_Cart.NEW);
                cartRepository.save(cart);
            }

            return ResponseEntity.status(HttpStatus.OK).body("Order has been canceled due to timeout");
        }
        else
        {
            order.setStatus(Status_Order.CONFIRMED);
            orderRepository.save(order);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Confirm success");
    }

    public ResponseEntity<?> getInformationPayment(int orderId) {
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if(order == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }
        Payment payment = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId);
        if(payment == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }
        Voucher voucher = null;
        return ResponseEntity.status(HttpStatus.OK).body(new getInformationPaymentFromOrderIdResponse(
                order.getOrderId(),
                order.getAddress(),
                order.getDeliveryFee(),
                order.getDateCreated(),
                order.getDateDeleted(),
                order.getDateUpdated(),
                order.getDeliveryDate(),
                order.getDiscountPrice(),
                order.getIsDeleted(),
                order.getNote(),
                order.getOrderDate(),
                order.getPhoneNumber(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getUser().getUserId(),
                voucher != null ? voucher.getVoucherId() :0,
                new CRUDPaymentResponse(
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

                )
        ));
    }

    @Transactional
    public ResponseEntity<?> getInformationPaymentLanguage(int orderId, Language language) {
        // Tìm đơn hàng
        Orders order = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found order");
        }

        // Tìm payment
        Payment payment = paymentRepository.findByOrderOrderIdAndIsDeletedFalse(orderId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment");
        }

        // Tìm voucher (nếu có)
        Voucher voucher = order.getVoucher(); // Giả sử Orders có phương thức getVoucher()

        // Lấy OrderItem và CartItems
        OrderItem orderItem = order.getOrderItem();
        List<CartItem> cartItems = (orderItem != null && orderItem.getCart() != null)
                ? orderItem.getCart().getCartItems()
                : List.of();

        // Xử lý dịch ngôn ngữ nếu language == EN
        Map<Integer, ProductTranslation> productTranslationMap = new HashMap<>();
        if (language == Language.EN && !cartItems.isEmpty()) {
            Set<Integer> productIds = cartItems.stream()
                    .map(ci -> ci.getProductVariants().getProduct().getProId())
                    .collect(Collectors.toSet());

            // Lấy các bản dịch hiện có và thêm vào productTranslationMap
            productTranslationRepository.findAllByProduct_ProIdIn(productIds)
                    .forEach(t -> productTranslationMap.put(t.getProduct().getProId(), t));

            // Tạo bản dịch cho các sản phẩm chưa có
            List<ProductTranslation> missingTranslations = productIds.stream()
                    .filter(id -> !productTranslationMap.containsKey(id))
                    .map(id -> {
                        Product product = productRepository.findById(id).orElse(null);
                        if (product == null) return null;
                        ProductTranslation trans = new ProductTranslation();
                        trans.setProduct(product);
                        trans.setIsDeleted(false);
                        trans.setDateCreated(LocalDateTime.now());
                        trans.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
                        trans.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
                        return trans;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!missingTranslations.isEmpty()) {
                productTranslationRepository.saveAll(missingTranslations);
                for (ProductTranslation trans : missingTranslations) {
                    productTranslationMap.put(trans.getProduct().getProId(), trans);
                }
            }
        }

        // Ánh xạ CartItem sang CartItemResponse
        List<CRUDCartItemResponse> cartItemResponses = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProductVariants().getProduct();
                    String proName = language == Language.EN && productTranslationMap.containsKey(product.getProId())
                            ? productTranslationMap.get(product.getProId()).getProName()
                            : product.getProName();

                    return new CRUDCartItemResponse(
                            cartItem.getCartItemId(),
                            product.getProId(),
                            proName,
                            cartItem.getCart().getCartId(),
                            cartItem.getProductVariants().getSize(),
                            cartItem.getTotalPrice(),
                            cartItem.getQuantity(),
                            product.getListProImg() // Giả sử Product có getImageUrl()
                    );
                })
                .toList();

        // Trả về response
        return ResponseEntity.status(HttpStatus.OK).body(new getInformationPaymentFromOrderIdLanguageResponse(
                order.getOrderId(),
                order.getAddress(),
                order.getDeliveryFee(),
                order.getDateCreated(),
                order.getDateDeleted(),
                order.getDateUpdated(),
                order.getDeliveryDate(),
                order.getDiscountPrice(),
                order.getIsDeleted(),
                order.getNote(),
                order.getOrderDate(),
                order.getPhoneNumber(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getUser().getUserId(),
                voucher != null ? voucher.getVoucherId() : 0,
                new CRUDPaymentResponse(
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
                ),
                cartItemResponses
        ));
    }
    @Transactional
    public ResponseEntity<?> getAllOrderByUserId(String pageFromParam, String limitFromParam, int userId, Language language) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "dateCreated"));
        Page<Orders> orders = orderRepository.findAllByUserUserIdAndIsDeletedFalse(userId, pageable);

        List<CreateOrdersResponse> list = new ArrayList<>();
        Map<Integer, ProductTranslation> translationCache = new HashMap<>();

        for (Orders order : orders) {
            OrderItem orderItem = order.getOrderItem();
            if (orderItem == null || orderItem.getCart() == null) continue;

            List<CartItem> cartItems = orderItem.getCart().getCartItems();
            if (cartItems == null) continue;

            List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                var variant = cartItem.getProductVariants();
                var product = variant.getProduct();
                int productId = product.getProId();

                // Translate name
                String productNameTrans;
                if (language == Language.VN) {
                    productNameTrans = product.getProName();
                } else {
                    ProductTranslation translation = translationCache.get(productId);
                    if (translation == null) {
                        translation = productTranslationRepository.findByProduct_ProId(productId);
                        if (translation == null) {
                            translation = new ProductTranslation();
                            translation.setProduct(product);
                            translation.setIsDeleted(false);
                            translation.setDateCreated(LocalDateTime.now());
                            translation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
                            translation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
                            productTranslationRepository.save(translation);
                        }
                        translationCache.put(productId, translation);
                    }
                    productNameTrans = translation.getProName();
                }

                // Get first image
                String firstImageUrl = null;
                String imgStr = product.getListProImg();
                if (imgStr != null && !imgStr.trim().isEmpty()) {
                    String[] entries = imgStr.split(", ");
                    if (entries.length > 0) {
                        String[] parts = entries[0].split(": ");
                        if (parts.length == 2) {
                            firstImageUrl = parts[1];
                        }
                    }
                }

                crudCartItemResponses.add(new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        productId,
                        productNameTrans,
                        cartItem.getCart().getCartId(),
                        variant.getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        firstImageUrl
                ));
            }

            list.add(new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    (order.getVoucher() != null) ? order.getVoucher().getVoucherId() : null,
                    order.getPointCoinUse(),
                    crudCartItemResponses
            ));
        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllOrdersResponse(
                page,
                orders.getTotalPages(),
                limit,
                list.size(),
                userId,
                list
        ));
    }

    @Transactional
    public ResponseEntity<?> getAllOrderByUserIdAndStatus(String pageFromParam, String limitFromParam, int userId, Status_Order status, Language language) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }

        // Xử lý phân trang và giới hạn
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "dateCreated"));

        // Truy vấn tất cả đơn hàng và tải các đối tượng liên quan ngay trong truy vấn (JOIN FETCH)
        Page<Orders> orders = orderRepository.findAllByUserUserIdAndStatusAndIsDeletedFalse(userId, status, pageable);

        // Lấy tất cả các sản phẩm trong đơn hàng và cache translations
        List<Integer> productIds = orders.stream()
                .flatMap(order -> {
                    if (order.getOrderItem() != null && order.getOrderItem().getCart() != null) {
                        return order.getOrderItem().getCart().getCartItems().stream();
                    }
                    return Stream.empty();
                })
                .map(cartItem -> cartItem.getProductVariants().getProduct().getProId())
                .distinct()
                .collect(Collectors.toList());



        // Kiểm tra kiểu dữ liệu của productIds trước khi ép kiểu (hoặc sử dụng HashSet nếu cần thiết)
        Set<Integer> productIdSet = (productIds instanceof Set) ? (Set<Integer>) productIds : new HashSet<>(productIds);

// Kiểm tra null và tránh NullPointerException
        Map<Integer, ProductTranslation> translationCache = Optional.ofNullable(productTranslationRepository.findAllByProduct_ProIdIn(productIdSet))
                .orElse(Collections.emptyList())  // Nếu trả về null, sử dụng danh sách rỗng
                .stream()
                .collect(Collectors.toMap(pt -> pt.getProduct().getProId(), Function.identity()));



        List<CreateOrdersResponse> list = new ArrayList<>();
        for (Orders order : orders) {
            OrderItem orderItem = order.getOrderItem();
            if (orderItem == null || orderItem.getCart() == null) continue;

            List<CartItem> cartItems = orderItem.getCart().getCartItems();
            if (cartItems == null) continue;

            List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                var product = cartItem.getProductVariants().getProduct();
                int productId = product.getProId();

                // Lấy tên sản phẩm từ cache
                String productNameTrans = language == Language.VN ? product.getProName() :
                        translationCache.getOrDefault(productId, new ProductTranslation()).getProName();

                // Lấy ảnh đầu tiên của sản phẩm
                String firstImageUrl = getFirstImageUrl(product);

                // Thêm item vào danh sách
                crudCartItemResponses.add(new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        productId,
                        productNameTrans,
                        cartItem.getCart().getCartId(),
                        cartItem.getProductVariants().getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        firstImageUrl
                ));
            }

            // Thêm đơn hàng vào danh sách trả về
            list.add(new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    null,  // Voucher hiện tại chưa có giá trị thực tế trong code ban đầu
                    order.getPointCoinUse(),
                    crudCartItemResponses
            ));
        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllOrdersResponse(
                page,
                orders.getTotalPages(),
                limit,
                list.size(),
                userId,
                list
        ));
    }

    // Lấy ảnh đầu tiên của sản phẩm
    private String getFirstImageUrl(Product product) {
        String currentProImg = product.getListProImg();
        String firstImageUrl = null;
        if (currentProImg != null && !currentProImg.trim().isEmpty()) {
            String[] imageEntries = currentProImg.split(", ");
            if (imageEntries.length > 0) {
                String[] parts = imageEntries[0].split(": ");
                if (parts.length == 2) {
                    firstImageUrl = parts[1];
                }
            }
        }
        return firstImageUrl;
    }

    @Transactional
    public ResponseEntity<?> cancelOrder(int orderId, int userId) {
        Orders order = orderRepository.findByOrderIdAndUserUserIdAndIsDeletedFalse(orderId, userId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        if (order.getStatus() == Status_Order.CANCELLED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Order already cancelled");
        }
        Payment payment = paymentRepository.findByOrderOrderId(order.getOrderId());
        if (payment != null) {

            if (payment.getStatus() == Status_Payment.PENDING) {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
            }
            Shippment shipment = shipmentRepository.findByPaymentPaymentIdAndIsDeletedFalse(payment.getPaymentId());
            if (shipment != null) {
                if (shipment.getStatus() == Status_Shipment.SUCCESS || (shipment.getStatus() == Status_Shipment.SHIPPING && shipment.getUser() != null) ) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Order cannot be cancelled as shipment is in progress or completed");
                }
                if (shipment.getStatus() == Status_Shipment.WAITING) {

                    if (payment.getStatus() == Status_Payment.COMPLETED) {
                        payment.setStatus(Status_Payment.REFUND);
                        payment.setDateRefunded(LocalDateTime.now());
                        payment.setIsRefund(false);
                        paymentRepository.save(payment);
                    }
                    shipment.setStatus(Status_Shipment.CANCELLED);
                    shipment.setDateCancel(LocalDateTime.now());
                    shipmentRepository.save(shipment);
                }
            } else {
                System.out.println("Shipment not found or already deleted");
            }
        } else {
            System.out.println("Payment not found");
        }
        order.setStatus(Status_Order.CANCELLED);
        order.setDateCanceled(LocalDateTime.now());
        orderRepository.save(order);
        Voucher voucher = order.getVoucher();
        if (voucher != null) {
            UserVoucher userVoucher = userVoucherRepository.findByUserUserIdAndVoucherVoucherId(userId, voucher.getVoucherId());
            if (userVoucher != null) {
                userVoucher.setStatus(Status_UserVoucher.INACTIVE);
                userVoucherRepository.save(userVoucher);
                System.out.println("User voucher updated to INACTIVE");
            }
        }

        UserCoin userCoin = userCointRepository.findByUserUserId(order.getUser().getUserId());
        if(userCoin != null)
        {
            float point_coint = order.getPointCoinUse() + userCoin.getPointCoin();
            userCoin.setPointCoin(point_coint);
            userCointRepository.save(userCoin);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Order cancelled successfully");
    }

    public ResponseEntity<?> detailItemOrders(int orderId,Language language)
    {
        Orders orders = orderRepository.findByOrderId(orderId);
        if (orders == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        OrderItem orderItem = orders.getOrderItem();
        Cart cart = orderItem.getCart();
        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
        List<ItemOrderResponse> itemOrderResponses = new ArrayList<>();
        for(CartItem cartItem: cartItems)
        {
            String product_name_trans = "";
            if(language == Language.VN)
            {
                product_name_trans = cartItem.getProductVariants().getProduct().getProName();
            }
            else if(language == Language.EN)
            {
                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
                if(productTranslation != null)
                {
                    product_name_trans = productTranslation.getProName();
                }
                else {
                    ProductTranslation productTranslation1 = new ProductTranslation();
                    productTranslation1.setProduct(cartItem.getProductVariants().getProduct());
                    productTranslation1.setIsDeleted(false);
                    productTranslation1.setDateCreated(LocalDateTime.now());
                    productTranslation1.setProName(supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(),Language.EN));
                    productTranslation1.setDescription(supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(),Language.EN));
                    productTranslationRepository.save(productTranslation1);
                    product_name_trans = productTranslation1.getProName();
                }
            }
            ProductVariants productVariants = cartItem.getProductVariants();
            itemOrderResponses.add(new ItemOrderResponse(
                    cartItem.getCartItemId(),
                    cart.getCartId(),
                    productVariants.getProduct().getProId(),
                    product_name_trans,
                    productVariants.getSize(),
                    productVariants.getPrice(),
                    cartItem.getTotalPrice(),
                    cartItem.getQuantity()
            ));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ListItemOrderResponse(orderId,itemOrderResponses.size(),itemOrderResponses));
    }

    @Transactional
    public ResponseEntity<?> listHistoryOrder(int userId, Language language) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Custom: Load orders cùng với orderItem, cart, cartItems, product và productVariants
        List<Orders> orders = orderRepository.findAllConfirmedWithItems(userId);  // ✅ Custom query JOIN FETCH

        if (orders.isEmpty()) {
            return ResponseEntity.ok(new ListAllHistoryOrderResponse(userId, 0, List.of()));
        }

        // ======= Preload Shipments =======
        List<Integer> paymentIds = orders.stream()
                .map(o -> o.getPayment() != null ? o.getPayment().getPaymentId() : null)
                .filter(Objects::nonNull)
                .toList();

        Map<Integer, Shippment> shipmentMap = shipmentRepository
                .findAllByPayment_PaymentIdInAndIsDeletedFalse(paymentIds)
                .stream()
                .filter(shipment -> shipment.getStatus() == Status_Shipment.SUCCESS)
                .collect(Collectors.toMap(s -> s.getPayment().getPaymentId(), Function.identity()));

        // ======= Preload Translation nếu cần =======
        Map<Integer, ProductTranslation> productTranslationMap = new HashMap<>();
        if (language == Language.EN) {
            Set<Integer> productIds = orders.stream()
                    .flatMap(order -> {
                        if (order.getOrderItem() == null || order.getOrderItem().getCart() == null) return Stream.empty();
                        return order.getOrderItem().getCart().getCartItems().stream()
                                .map(ci -> ci.getProductVariants().getProduct().getProId());
                    })
                    .collect(Collectors.toSet());

            // Lấy translation hiện có
            productTranslationMap = productTranslationRepository.findAllByProduct_ProIdIn(productIds)
                    .stream()
                    .collect(Collectors.toMap(t -> t.getProduct().getProId(), Function.identity()));

            // Gom những cái chưa có và convert batch
            final Map<Integer, ProductTranslation> productTranslationMap1 = productTranslationMap;
            List<ProductTranslation> missingTranslations = productIds.stream()
                    .filter(id -> !productTranslationMap1.containsKey(id))
                    .map(id -> {
                        Product product = productRepository.findById(id).orElse(null);
                        if (product == null) return null;
                        ProductTranslation trans = new ProductTranslation();
                        trans.setProduct(product);
                        trans.setIsDeleted(false);
                        trans.setDateCreated(LocalDateTime.now());
                        trans.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
                        trans.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
                        return trans;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!missingTranslations.isEmpty()) {
                productTranslationRepository.saveAll(missingTranslations);
                for (ProductTranslation trans : missingTranslations) {
                    productTranslationMap.put(trans.getProduct().getProId(), trans);
                }
            }
        }

        final Map<Integer, ProductTranslation> finalProductTranslationMap = productTranslationMap;

        // ======= Process song song (parallelStream) =======
        List<HistoryOrderResponse> historyOrderResponses = orders.stream()
                .map(order -> buildHistoryResponse(order, language, shipmentMap, finalProductTranslationMap))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((HistoryOrderResponse h) -> {
                    if (h.getShipment().getDateShipped() != null) {
                        return h.getShipment().getDateShipped();
                    }
                    return h.getOrder().getDateCreated();
                }).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ListAllHistoryOrderResponse(userId, historyOrderResponses.size(), historyOrderResponses));
    }




    // ======= Modified method with preloaded data =======
    private HistoryOrderResponse buildHistoryResponse(Orders order, Language language,
                                                      Map<Integer, Shippment> shipmentMap,
                                                      Map<Integer, ProductTranslation> productTranslationMap) {
        Payment payment = order.getPayment();
        if (payment == null) return null;

        Shippment shipment = shipmentMap.get(payment.getPaymentId());
        if (shipment == null) return null;

        List<CRUDCartItemResponse> cartItemResponses = buildCartItemResponses(order.getOrderItem(), language, productTranslationMap);
        if (cartItemResponses == null) return null;

        CreateOrdersResponse orderResponse = new CreateOrdersResponse(
                order.getOrderId(), order.getAddress(), order.getDeliveryFee(),
                order.getDateCreated(), order.getDateDeleted(), order.getDateUpdated(),
                order.getDeliveryDate(), order.getDateCanceled(), order.getDiscountPrice(),
                order.getIsDeleted(), order.getNote(), order.getOrderDate(),
                order.getPhoneNumber(), order.getStatus(), order.getTotalPrice(),
                order.getUser().getUserId(), null, order.getPointCoinUse(), cartItemResponses
        );

        User customer = order.getUser();
        User shipper = shipment.getUser();

        CRUDShipmentResponse shipmentResponse = new CRUDShipmentResponse(
                shipment.getShipmentId(), shipper.getFullName(), shipment.getDateCreated(),
                shipment.getDateDeleted(), shipment.getDateDelivered(), shipment.getDateShip(),
                shipment.getDateCancel(), shipment.getIsDeleted(), shipment.getStatus(),
                shipment.getNote(),
                shipment.getPayment().getPaymentId(), shipper.getUserId(),
                customer.getFullName(), customer.getUserId(),
                String.join(", ", customer.getStreet(), customer.getWard(), customer.getDistrict(), customer.getCity()),
                customer.getPhoneNumber(), customer.getEmail(), order.getOrderId()
        );

        return new HistoryOrderResponse(orderResponse, shipmentResponse, cartItemResponses);
    }


    private List<CRUDCartItemResponse> buildCartItemResponses(OrderItem orderItem, Language language,
                                                              Map<Integer, ProductTranslation> translationMap) {
        if (orderItem == null || orderItem.getCart() == null) return null;

        List<CartItem> cartItems = orderItem.getCart().getCartItems();
        if (cartItems == null) return null;

        return cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProductVariants().getProduct();
                    String productNameTrans = getTranslatedProductName(product, language, translationMap);
                    String firstImageUrl = extractFirstImage(product.getListProImg());

                    return new CRUDCartItemResponse(
                            cartItem.getCartItemId(), product.getProId(), productNameTrans,
                            cartItem.getCart().getCartId(), cartItem.getProductVariants().getSize(),
                            cartItem.getTotalPrice(), cartItem.getQuantity(), firstImageUrl
                    );
                }).collect(Collectors.toList());
    }


    private String getTranslatedProductName(Product product, Language language,
                                            Map<Integer, ProductTranslation> translationMap) {
        if (language == Language.VN) return product.getProName();

        ProductTranslation translation = translationMap.get(product.getProId());
        if (translation == null) {
            translation = new ProductTranslation();
            translation.setProduct(product);
            translation.setIsDeleted(false);
            translation.setDateCreated(LocalDateTime.now());
            translation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
            translation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
            productTranslationRepository.save(translation);
            translationMap.put(product.getProId(), translation); // ✅ Cache lại
        }
        return translation.getProName();
    }


    @Transactional
    public  ResponseEntity<?> fetchOrdersAwaitingPayment(int userId,Language language)
    {
        List<Orders> ordersWaiting = orderRepository.findAllByUserUserIdAndStatus(userId, Status_Order.WAITING);
        List<Orders> ordersConfirm = orderRepository.findAllByUserUserIdAndStatus(userId, Status_Order.CONFIRMED);
        List<CreateOrdersResponse> listOrderWaiting = new ArrayList<>();
        List<CreateOrdersResponse> listOrderConfirm = new ArrayList<>();
        List<CreateOrdersResponse> listOrderConfirmPaymentPending = new ArrayList<>();
        for(Orders order: ordersWaiting)
        {
            Voucher voucher = null;
            OrderItem orderItem = order.getOrderItem();
            List<CRUDCartItemResponse> crudCartItemResponses = null;
            if (orderItem != null) {
                Cart cart = orderItem.getCart();
                if (cart != null) {
                    List<CartItem> cartItems = cart.getCartItems();
                    crudCartItemResponses = null;
                    if (cartItems != null) {
                        crudCartItemResponses = cartItems.stream()
                                .map(cartItem -> {
                                    String productNameTrans = "";
                                    if (language == Language.VN) {
                                        productNameTrans = cartItemRepository.findProductNameByCartItemId(cartItem.getCartItemId());
                                    } else if (language == Language.EN) {
                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
                                                cartItem.getProductVariants().getProduct().getProId()
                                        );

                                        if (productTranslation != null) {
                                            productNameTrans = productTranslation.getProName();
                                        }
                                    }
                                    String currentProImg = cartItemRepository.findListProImgByCartItemId(cartItem.getCartItemId());
                                    String firstImageUrl = null;

                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                                        String[] imageEntries1 = currentProImg.split(", ");
                                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
                                            String[] parts = imageEntries1[0].split(": ");
                                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
                                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
                                            }
                                        }
                                    }
                                    return new CRUDCartItemResponse(
                                            cartItem.getCartItemId(),
                                            cartItem.getProductVariants().getProduct().getProId(),
                                            productNameTrans,
                                            cartItem.getCart().getCartId(),
                                            cartItem.getProductVariants().getSize(),
                                            cartItem.getTotalPrice(),
                                            cartItem.getQuantity(),
                                            firstImageUrl
                                    );
                                })
                                .collect(Collectors.toList());
                    }
                }
            }

            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    voucher != null ? voucher.getVoucherId() : null,
                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
                    crudCartItemResponses
            );
            listOrderWaiting.add(createOrdersResponse);
        }

        for(Orders order: ordersConfirm)
        {
            Payment payment = order.getPayment();
            if(payment != null) {
                continue;
            }
            Voucher voucher = null;
            OrderItem orderItem = order.getOrderItem();
            List<CRUDCartItemResponse> crudCartItemResponses = null;
            if (orderItem != null) {
                Cart cart = orderItem.getCart();
                if (cart != null) {
                    List<CartItem> cartItems = cart.getCartItems();
                    crudCartItemResponses = null;
                    if (cartItems != null) {
                        crudCartItemResponses = cartItems.stream()
                                .map(cartItem -> {
                                    String productNameTrans = "";
                                    if (language == Language.VN) {
                                        productNameTrans = cartItemRepository.findProductNameByCartItemId(cartItem.getCartItemId());
                                    } else if (language == Language.EN) {
                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
                                                cartItem.getProductVariants().getProduct().getProId()
                                        );

                                        if (productTranslation != null) {
                                            productNameTrans = productTranslation.getProName();
                                        }
                                    }
                                    String currentProImg = cartItemRepository.findListProImgByCartItemId(cartItem.getCartItemId());
                                    String firstImageUrl = null;


                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                                        String[] imageEntries1 = currentProImg.split(", ");
                                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
                                            String[] parts = imageEntries1[0].split(": ");
                                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
                                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
                                            }
                                        }
                                    }
                                    return new CRUDCartItemResponse(
                                            cartItem.getCartItemId(),
                                            cartItem.getProductVariants().getProduct().getProId(),
                                            productNameTrans,
                                            cartItem.getCart().getCartId(),
                                            cartItem.getProductVariants().getSize(),
                                            cartItem.getTotalPrice(),
                                            cartItem.getQuantity(),
                                            firstImageUrl
                                    );
                                })
                                .collect(Collectors.toList());
                    }
                }
            }
            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    voucher != null ? voucher.getVoucherId() : null,
                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
                    crudCartItemResponses
            );
            listOrderConfirm.add(createOrdersResponse);
        }

        for(Orders order: ordersConfirm)
        {
            Payment payment = order.getPayment();
            if(payment == null) {
                continue;
            }
            if (payment.getPaymentMethod() == Payment_Method.CASH && payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            if(payment.getStatus() != Status_Payment.PENDING)
            {
                continue;
            }
            OrderItem orderItem = order.getOrderItem();
            List<CRUDCartItemResponse> crudCartItemResponses = null;
            if (orderItem != null) {
                Cart cart = orderItem.getCart();
                if (cart != null) {
                    List<CartItem> cartItems = cart.getCartItems();
                    crudCartItemResponses = null;
                    if (cartItems != null) {
                        crudCartItemResponses = cartItems.stream()
                                .map(cartItem -> {
                                    String productNameTrans = "";
                                    if (language == Language.VN) {
                                        productNameTrans = cartItemRepository.findProductNameByCartItemId(cartItem.getCartItemId());
                                    } else if (language == Language.EN) {
                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
                                                cartItem.getProductVariants().getProduct().getProId()
                                        );

                                        if (productTranslation != null) {
                                            productNameTrans = productTranslation.getProName();
                                        }
                                    }
                                    String currentProImg = cartItemRepository.findListProImgByCartItemId(cartItem.getCartItemId());
                                    String firstImageUrl = null;

                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                                        String[] imageEntries1 = currentProImg.split(", ");
                                        if (imageEntries1.length > 0) {
                                            String[] parts = imageEntries1[0].split(": ");
                                            if (parts.length == 2) {
                                                firstImageUrl = parts[1];
                                            }
                                        }
                                    }
                                    return new CRUDCartItemResponse(
                                            cartItem.getCartItemId(),
                                            cartItem.getProductVariants().getProduct().getProId(),
                                            productNameTrans,
                                            cartItem.getCart().getCartId(),
                                            cartItem.getProductVariants().getSize(),
                                            cartItem.getTotalPrice(),
                                            cartItem.getQuantity(),
                                            firstImageUrl
                                    );
                                })
                                .collect(Collectors.toList());
                    }
                }
            }
            Voucher voucher = null;

            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    voucher != null ? voucher.getVoucherId() : null,
                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
                    crudCartItemResponses
            );
            listOrderConfirmPaymentPending.add(createOrdersResponse);
        }
        listOrderWaiting.sort((h1, h2) -> {
            if (h1.getDateOders() != null && h2.getDateOders() != null) {
                return h2.getDateOders().compareTo(h1.getDateOders());
            }
            return h2.getDateCreated().compareTo(h1.getDateCreated());
        });

        listOrderConfirm.sort((h1, h2) -> {
            if (h1.getDateOders() != null && h2.getDateOders() != null) {
                return h2.getDateOders().compareTo(h1.getDateOders());
            }
            return h2.getDateCreated().compareTo(h1.getDateCreated());
        });

        listOrderConfirmPaymentPending.sort((h1, h2) -> {
            if (h1.getDateOders() != null && h2.getDateOders() != null) {
                return h2.getDateOders().compareTo(h1.getDateOders());
            }
            return h2.getDateCreated().compareTo(h1.getDateCreated());
        });

        ListOrderWaiting list1 = new ListOrderWaiting(listOrderWaiting.size(),listOrderWaiting);
        ListAllOrderConfirmAndNotPayment list2 = new ListAllOrderConfirmAndNotPayment(listOrderConfirm.size(),listOrderConfirm);
        ListAllOrderConfirmAndNotPayment list3 = new ListAllOrderConfirmAndNotPayment(listOrderConfirmPaymentPending.size(), listOrderConfirmPaymentPending);
        return ResponseEntity.status(HttpStatus.OK).body(
                new fetchOrdersAwaitingPayment(
                        list1.getTotal() + list2.getTotal() + list3.getTotal(),
                        list1,
                        list2,
                        list3
                )
        );
    }





    private CRUDCartItemResponse buildCartItemResponse(CartItem cartItem, Map<Integer, ProductTranslation> transMap, Language language) {
        Product product = cartItem.getProductVariants().getProduct();
        String name = language == Language.VN ? product.getProName() :
                Optional.ofNullable(transMap.get(product.getProId()))
                        .map(ProductTranslation::getProName)
                        .orElseGet(() -> {
                            String transName = supportFunction.convertLanguage(product.getProName(), Language.EN);
                            String transDesc = supportFunction.convertLanguage(product.getDescription(), Language.EN);

                            return transName;
                        });

        String firstImageUrl = Optional.ofNullable(product.getListProImg())
                .filter(img -> !img.trim().isEmpty())
                .map(img -> img.split(", "))
                .filter(arr -> arr.length > 0)
                .map(arr -> arr[0].split(": "))
                .filter(parts -> parts.length == 2)
                .map(parts -> parts[1])
                .orElse(null);

        return new CRUDCartItemResponse(
                cartItem.getCartItemId(),
                product.getProId(),
                name,
                cartItem.getCart().getCartId(),
                cartItem.getProductVariants().getSize(),
                cartItem.getTotalPrice(),
                cartItem.getQuantity(),
                firstImageUrl
        );
    }






//    @Transactional
//    public  ResponseEntity<?> fetchOrdersAwaitingPayment(int userId,Language language)
//    {
//        List<Orders> ordersWaiting = orderRepository.findAllByUserUserIdAndStatus(userId, Status_Order.WAITING);
//        List<Orders> ordersConfirm = orderRepository.findAllByUserUserIdAndStatus(userId, Status_Order.CONFIRMED);
//        List<CreateOrdersResponse> listOrderWaiting = new ArrayList<>();
//        List<CreateOrdersResponse> listOrderConfirm = new ArrayList<>();
//        List<CreateOrdersResponse> listOrderConfirmPaymentPending = new ArrayList<>();
//        for(Orders order: ordersWaiting)
//        {
//            Voucher voucher = null;
//            OrderItem orderItem = order.getOrderItem();
//            List<CRUDCartItemResponse> crudCartItemResponses = null;
//            if (orderItem != null) {
//                Cart cart = orderItem.getCart();
//                if (cart != null) {
//                    List<CartItem> cartItems = cart.getCartItems();
//                    crudCartItemResponses = null;
//                    if (cartItems != null) {
//                        crudCartItemResponses = cartItems.stream()
//                                .map(cartItem -> {
//                                    String productNameTrans = "";
//                                    if (language == Language.VN) {
//                                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
//                                    } else if (language == Language.EN) {
//                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
//                                                cartItem.getProductVariants().getProduct().getProId()
//                                        );
//
//                                        if (productTranslation != null) {
//                                            productNameTrans = productTranslation.getProName();
//                                        } else {
//                                            ProductTranslation newTranslation = new ProductTranslation();
//                                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
//                                            newTranslation.setIsDeleted(false);
//                                            newTranslation.setDateCreated(LocalDateTime.now());
//                                            newTranslation.setProName(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
//                                            );
//                                            newTranslation.setDescription(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
//                                            );
//
//                                            productTranslationRepository.save(newTranslation);
//                                            productNameTrans = newTranslation.getProName();
//                                        }
//                                    }
//                                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
//                                    String firstImageUrl = null;
//
//                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                                        String[] imageEntries1 = currentProImg.split(", ");
//                                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                                            String[] parts = imageEntries1[0].split(": ");
//                                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                                            }
//                                        }
//                                    }
//                                    return new CRUDCartItemResponse(
//                                            cartItem.getCartItemId(),
//                                            cartItem.getProductVariants().getProduct().getProId(),
//                                            productNameTrans,
//                                            cartItem.getCart().getCartId(),
//                                            cartItem.getProductVariants().getSize(),
//                                            cartItem.getTotalPrice(),
//                                            cartItem.getQuantity(),
//                                            firstImageUrl
//                                    );
//                                })
//                                .collect(Collectors.toList());
//                    }
//                }
//            }
//
//            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
//                    order.getOrderId(),
//                    order.getAddress(),
//                    order.getDeliveryFee(),
//                    order.getDateCreated(),
//                    order.getDateDeleted(),
//                    order.getDateUpdated(),
//                    order.getDeliveryDate(),
//                    order.getDateCanceled(),
//                    order.getDiscountPrice(),
//                    order.getIsDeleted(),
//                    order.getNote(),
//                    order.getOrderDate(),
//                    order.getPhoneNumber(),
//                    order.getStatus(),
//                    order.getTotalPrice(),
//                    order.getUser().getUserId(),
//                    voucher != null ? voucher.getVoucherId() : null,
//                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
//                    crudCartItemResponses
//            );
//            listOrderWaiting.add(createOrdersResponse);
//        }
//
//        for(Orders order: ordersConfirm)
//        {
//                Payment payment = order.getPayment();
//                if(payment != null) {
//                    continue;
//                }
//                Voucher voucher = null;
//            OrderItem orderItem = order.getOrderItem();
//            List<CRUDCartItemResponse> crudCartItemResponses = null;
//            if (orderItem != null) {
//                Cart cart = orderItem.getCart();
//                if (cart != null) {
//                    List<CartItem> cartItems = cart.getCartItems();
//                    crudCartItemResponses = null;
//                    if (cartItems != null) {
//                        crudCartItemResponses = cartItems.stream()
//                                .map(cartItem -> {
//                                    String productNameTrans = "";
//                                    if (language == Language.VN) {
//                                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
//                                    } else if (language == Language.EN) {
//                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
//                                                cartItem.getProductVariants().getProduct().getProId()
//                                        );
//
//                                        if (productTranslation != null) {
//                                            productNameTrans = productTranslation.getProName();
//                                        } else {
//                                            ProductTranslation newTranslation = new ProductTranslation();
//                                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
//                                            newTranslation.setIsDeleted(false);
//                                            newTranslation.setDateCreated(LocalDateTime.now());
//                                            newTranslation.setProName(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
//                                            );
//                                            newTranslation.setDescription(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
//                                            );
//
//                                            productTranslationRepository.save(newTranslation);
//                                            productNameTrans = newTranslation.getProName();
//                                        }
//                                    }
//                                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
//                                    String firstImageUrl = null;
//
//                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                                        String[] imageEntries1 = currentProImg.split(", ");
//                                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                                            String[] parts = imageEntries1[0].split(": ");
//                                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                                            }
//                                        }
//                                    }
//                                    return new CRUDCartItemResponse(
//                                            cartItem.getCartItemId(),
//                                            cartItem.getProductVariants().getProduct().getProId(),
//                                            productNameTrans,
//                                            cartItem.getCart().getCartId(),
//                                            cartItem.getProductVariants().getSize(),
//                                            cartItem.getTotalPrice(),
//                                            cartItem.getQuantity(),
//                                            firstImageUrl
//                                    );
//                                })
//                                .collect(Collectors.toList());
//                    }
//                }
//            }
//                CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
//                        order.getOrderId(),
//                        order.getAddress(),
//                        order.getDeliveryFee(),
//                        order.getDateCreated(),
//                        order.getDateDeleted(),
//                        order.getDateUpdated(),
//                        order.getDeliveryDate(),
//                        order.getDateCanceled(),
//                        order.getDiscountPrice(),
//                        order.getIsDeleted(),
//                        order.getNote(),
//                        order.getOrderDate(),
//                        order.getPhoneNumber(),
//                        order.getStatus(),
//                        order.getTotalPrice(),
//                        order.getUser().getUserId(),
//                        voucher != null ? voucher.getVoucherId() : null,
//                        (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
//                        crudCartItemResponses
//                );
//                listOrderConfirm.add(createOrdersResponse);
//            }
//
//        for(Orders order: ordersConfirm)
//        {
//            Payment payment = order.getPayment();
//            if(payment == null) {
//                continue;
//            }
//            if (payment.getPaymentMethod() == Payment_Method.CASH && payment.getStatus() != Status_Payment.PENDING)
//            {
//                continue;
//            }
//            if(payment.getStatus() != Status_Payment.PENDING)
//            {
//                continue;
//            }
//            OrderItem orderItem = order.getOrderItem();
//            List<CRUDCartItemResponse> crudCartItemResponses = null;
//            if (orderItem != null) {
//                Cart cart = orderItem.getCart();
//                if (cart != null) {
//                    List<CartItem> cartItems = cart.getCartItems();
//                    crudCartItemResponses = null;
//                    if (cartItems != null) {
//                        crudCartItemResponses = cartItems.stream()
//                                .map(cartItem -> {
//                                    String productNameTrans = "";
//                                    if (language == Language.VN) {
//                                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
//                                    } else if (language == Language.EN) {
//                                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
//                                                cartItem.getProductVariants().getProduct().getProId()
//                                        );
//
//                                        if (productTranslation != null) {
//                                            productNameTrans = productTranslation.getProName();
//                                        } else {
//                                            ProductTranslation newTranslation = new ProductTranslation();
//                                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
//                                            newTranslation.setIsDeleted(false);
//                                            newTranslation.setDateCreated(LocalDateTime.now());
//                                            newTranslation.setProName(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
//                                            );
//                                            newTranslation.setDescription(
//                                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
//                                            );
//
//                                            productTranslationRepository.save(newTranslation);
//                                            productNameTrans = newTranslation.getProName();
//                                        }
//                                    }
//                                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
//                                    String firstImageUrl = null;
//
//                                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                                        String[] imageEntries1 = currentProImg.split(", ");
//                                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                                            String[] parts = imageEntries1[0].split(": ");
//                                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                                            }
//                                        }
//                                    }
//                                    return new CRUDCartItemResponse(
//                                            cartItem.getCartItemId(),
//                                            cartItem.getProductVariants().getProduct().getProId(),
//                                            productNameTrans,
//                                            cartItem.getCart().getCartId(),
//                                            cartItem.getProductVariants().getSize(),
//                                            cartItem.getTotalPrice(),
//                                            cartItem.getQuantity(),
//                                            firstImageUrl
//                                    );
//                                })
//                                .collect(Collectors.toList());
//                    }
//                }
//            }
//            Voucher voucher = null;
//
//            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
//                    order.getOrderId(),
//                    order.getAddress(),
//                    order.getDeliveryFee(),
//                    order.getDateCreated(),
//                    order.getDateDeleted(),
//                    order.getDateUpdated(),
//                    order.getDeliveryDate(),
//                    order.getDateCanceled(),
//                    order.getDiscountPrice(),
//                    order.getIsDeleted(),
//                    order.getNote(),
//                    order.getOrderDate(),
//                    order.getPhoneNumber(),
//                    order.getStatus(),
//                    order.getTotalPrice(),
//                    order.getUser().getUserId(),
//                    voucher != null ? voucher.getVoucherId() : null,
//                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
//                    crudCartItemResponses
//            );
//            listOrderConfirmPaymentPending.add(createOrdersResponse);
//        }
//        listOrderWaiting.sort((h1, h2) -> {
//            if (h1.getDateOders() != null && h2.getDateOders() != null) {
//                return h2.getDateOders().compareTo(h1.getDateOders());
//            }
//            return h2.getDateCreated().compareTo(h1.getDateCreated());
//        });
//
//        listOrderConfirm.sort((h1, h2) -> {
//            if (h1.getDateOders() != null && h2.getDateOders() != null) {
//                return h2.getDateOders().compareTo(h1.getDateOders());
//            }
//            return h2.getDateCreated().compareTo(h1.getDateCreated());
//        });
//
//        listOrderConfirmPaymentPending.sort((h1, h2) -> {
//            if (h1.getDateOders() != null && h2.getDateOders() != null) {
//                return h2.getDateOders().compareTo(h1.getDateOders());
//            }
//            return h2.getDateCreated().compareTo(h1.getDateCreated());
//        });
//
//        ListOrderWaiting list1 = new ListOrderWaiting(ordersWaiting.size(),listOrderWaiting);
//        ListAllOrderConfirmAndNotPayment list2 = new ListAllOrderConfirmAndNotPayment(ordersConfirm.size(),listOrderConfirm);
//        ListAllOrderConfirmAndNotPayment list3 = new ListAllOrderConfirmAndNotPayment(listOrderConfirmPaymentPending.size(), listOrderConfirmPaymentPending);
//        return ResponseEntity.status(HttpStatus.OK).body(
//                new fetchOrdersAwaitingPayment(
//                        list1.getTotal() + list2.getTotal(),
//                        list1,
//                        list2,
//                        list3
//                )
//        );
//        }




    @Transactional
    public ResponseEntity<?> listOrderCancelnotPayment(int userId, Language language) {
        List<Orders> orders = orderRepository.findAllCanceledOrdersWithoutPayment(userId, Status_Order.CANCELLED);

        Set<Integer> productIds = new HashSet<>();
        for (Orders order : orders) {
            for (CartItem cartItem : order.getOrderItem().getCart().getCartItems()) {
                productIds.add(cartItem.getProductVariants().getProduct().getProId());
            }
        }

        Map<Integer, ProductTranslation> translationCache = new HashMap<>();
        if (language == Language.EN && !productIds.isEmpty()) {
            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
            for (ProductTranslation pt : translations) {
                translationCache.put(pt.getProduct().getProId(), pt);
            }

            Set<Integer> missingIds = new HashSet<>(productIds);
            missingIds.removeAll(translationCache.keySet());

            List<ProductTranslation> toSave = new ArrayList<>();
            for (Orders order : orders) {
                for (CartItem cartItem : order.getOrderItem().getCart().getCartItems()) {
                    Product product = cartItem.getProductVariants().getProduct();
                    int productId = product.getProId();

                    if (missingIds.contains(productId)) {
                        ProductTranslation translation = new ProductTranslation();
                        translation.setProduct(product);
                        translation.setIsDeleted(false);
                        translation.setDateCreated(LocalDateTime.now());
                        translation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
                        translation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
                        toSave.add(translation);
                        translationCache.put(productId, translation);
                        missingIds.remove(productId);
                    }
                }
            }

            if (!toSave.isEmpty()) {
                productTranslationRepository.saveAll(toSave);
            }
        }

        List<CreateOrdersResponse> createOrdersResponses = new ArrayList<>();

        for (Orders order : orders) {
            List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
            List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();

            for (CartItem cartItem : cartItems) {
                ProductVariants productVariant = cartItem.getProductVariants();
                Product product = productVariant.getProduct();
                int productId = product.getProId();

                String productNameTrans;
                if (language == Language.VN) {
                    productNameTrans = product.getProName();
                } else {
                    productNameTrans = translationCache.get(productId).getProName();
                }

                String firstImageUrl = extractFirstImage(product.getListProImg());

                crudCartItemResponses.add(new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        productId,
                        productNameTrans,
                        cartItem.getCart().getCartId(),
                        productVariant.getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        firstImageUrl
                ));
            }

            createOrdersResponses.add(new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    (order.getVoucher() != null) ? order.getVoucher().getVoucherId() : null,
                    order.getPointCoinUse(),
                    crudCartItemResponses
            ));
        }

        createOrdersResponses.sort((o1, o2) -> {
            if (o1.getDateCanceled() != null && o2.getDateCanceled() != null) {
                return o2.getDateCanceled().compareTo(o1.getDateCanceled());
            }
            return o2.getDateCreated().compareTo(o1.getDateCreated());
        });

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ListAllOrderCancelAndNotPayment(createOrdersResponses.size(), createOrdersResponses));
    }



        public interface OrderCancelProjection {
            Integer getOrderId();
            String getAddress();
            BigDecimal getDeliveryFee();
            LocalDateTime getDateCreated();
            LocalDateTime getDateDeleted();
            LocalDateTime getDateUpdated();
            LocalDateTime getDeliveryDate();
            LocalDateTime getDateCanceled();
            BigDecimal getDiscountPrice();
            Boolean getIsDeleted();
            String getNote();
            LocalDateTime getOrderDate();
            String getPhoneNumber();
            String getStatus();
            BigDecimal getTotalPrice();
            Integer getUserId();
            Integer getVoucherId();
            Integer getPointCoinUse();
        }




    @Transactional()
    public ResponseEntity<?> listOrderCancelHavetPayment(int userId, Language language) {
        // 1. Fetch cancelled orders with optimized query and sorting
        List<Orders> orders = orderRepository.findAllCancelledOrdersWithDetails(
                userId,
                Sort.by(Sort.Order.desc("dateCanceled").nullsLast(), Sort.Order.desc("dateCreated").nullsLast())
        );

        // 2. Preload translations for EN language
        Map<Integer, ProductTranslation> translationMap = new HashMap<>();
        if (language == Language.EN) {
            Set<Integer> productIds = new HashSet<>();
            for (Orders order : orders) {
                for (CartItem cartItem : order.getOrderItem().getCart().getCartItems()) {
                    productIds.add(cartItem.getProductVariants().getProduct().getProId());
                }
            }
            if (!productIds.isEmpty()) {
                translationMap = productTranslationRepository.findAllByProduct_ProIdIn(productIds)
                        .stream()
                        .collect(Collectors.toMap(t -> t.getProduct().getProId(), t -> t));
            }
        }

        // 3. Process orders in a single pass
        List<CreateOrdersResponse> responseList = new ArrayList<>(orders.size());
        for (Orders order : orders) {
            if (!isOrderCancelledAndNotRefunded(order)) {
                continue;
            }
            List<CRUDCartItemResponse> cartItems = buildCartItemResponses(
                    order.getOrderItem(), language, translationMap
            );
            responseList.add(new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
                    order.getPointCoinUse(),
                    cartItems
            ));
        }

        return ResponseEntity.ok(new ListAllOrderCancelAndNotPayment(responseList.size(), responseList));
    }



//    @Transactional
//    public ResponseEntity<?> listOrderCancelHavetPayment(int userId, Language language) {
//        // 1. Tải toàn bộ đơn hàng CANCELED với JOIN FETCH để tránh N+1
//        List<Orders> orders = orderRepository.findAllCancelledOrdersWithDetails(userId);
//
//        // 2. Nếu language = EN thì preload toàn bộ ProductTranslation một lần
//        Map<Integer, ProductTranslation> translationMap;
//        if (language == Language.EN) {
//            Set<Integer> productIds = orders.stream()
//                    .flatMap(order -> order.getOrderItem().getCart().getCartItems().stream())
//                    .map(cartItem -> cartItem.getProductVariants().getProduct().getProId())
//                    .collect(Collectors.toSet());
//            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
//            translationMap = translations.stream().collect(Collectors.toMap(
//                    t -> t.getProduct().getProId(), t -> t
//            ));
//        } else {
//            translationMap = new HashMap<>();
//        }
//
//        // 3. Xử lý stream đơn hàng
//        List<CreateOrdersResponse> responseList = orders.stream()
//                .filter(this::isOrderCancelledAndNotRefunded)
//                .map(order -> {
//                    List<CRUDCartItemResponse> cartItems = buildCartItemResponses(
//                            order.getOrderItem(), language, translationMap
//                    );
//                    return new CreateOrdersResponse(
//                            order.getOrderId(),
//                            order.getAddress(),
//                            order.getDeliveryFee(),
//                            order.getDateCreated(),
//                            order.getDateDeleted(),
//                            order.getDateUpdated(),
//                            order.getDeliveryDate(),
//                            order.getDateCanceled(),
//                            order.getDiscountPrice(),
//                            order.getIsDeleted(),
//                            order.getNote(),
//                            order.getOrderDate(),
//                            order.getPhoneNumber(),
//                            order.getStatus(),
//                            order.getTotalPrice(),
//                            order.getUser().getUserId(),
//                            order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
//                            order.getPointCoinUse(),
//                            cartItems
//                    );
//                })
//                .sorted(Comparator.comparing(CreateOrdersResponse::getDateCanceled,
//                                Comparator.nullsLast(Comparator.reverseOrder()))
//                        .thenComparing(CreateOrdersResponse::getDateCreated,
//                                Comparator.nullsLast(Comparator.reverseOrder())))
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(new ListAllOrderCancelAndNotPayment(responseList.size(), responseList));
//    }

    private boolean isOrderCancelledAndNotRefunded(Orders order) {
        Payment payment = order.getPayment();
        if (payment == null) return true;
        if (payment.getStatus() == Status_Payment.REFUND && payment.getPaymentMethod() == Payment_Method.CREDIT) return false;
        Shippment shipment = payment.getShipment();
        return shipment == null || shipment.getStatus() != Status_Shipment.SUCCESS;
    }




    private String extractFirstImage(String imageList) {
        if (imageList == null || imageList.trim().isEmpty()) return null;

        String[] images = imageList.split(", ");
        if (images.length > 0) {
            String[] parts = images[0].split(": ");
            if (parts.length == 2) return parts[1];
        }
        return null;
    }



    @Transactional
    public ResponseEntity<?> listOrderCancelAndPaymentRefund(Language language) {
        // Query orders with cancelled status and refunds in one batch
        List<Orders> orders = orderRepository.findAllByStatusAndPaymentStatus(Status_Order.CANCELLED, Status_Payment.REFUND);

        // Extract product IDs that need translations in one go
        Set<Integer> productIds = orders.stream()
                .flatMap(order -> order.getOrderItem().getCart().getCartItems().stream())
                .map(cartItem -> cartItem.getProductVariants().getProduct().getProId())
                .collect(Collectors.toSet());

        // Query all ProductTranslations in one batch instead of querying for each product
        Map<Integer, ProductTranslation> productTranslationMap = new HashMap<>();
        if (language == Language.EN) {
            List<ProductTranslation> productTranslations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
            productTranslationMap = productTranslations.stream()
                    .collect(Collectors.toMap(pt -> pt.getProduct().getProId(), pt -> pt));
        }

        // Collect the responses
        List<OrderCancelPaymentRefund> historyOrderResponses = new ArrayList<>();
        List<ProductTranslation> newTranslationsToSave = new ArrayList<>();

        // Process orders
        for (Orders order : orders) {
            Payment payment = order.getPayment();
            List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
            List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();

            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProductVariants().getProduct();
                String productNameTrans = getProductTranslation(product, productTranslationMap, language, newTranslationsToSave);

                String firstImageUrl = getFirstImageUrl(product);

                crudCartItemResponses.add(new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        product.getProId(),
                        productNameTrans,
                        cartItem.getCart().getCartId(),
                        cartItem.getProductVariants().getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        firstImageUrl
                ));
            }

            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
                    order.getPointCoinUse(),
                    crudCartItemResponses
            );

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

            historyOrderResponses.add(new OrderCancelPaymentRefund(createOrdersResponse, crudPaymentResponse, crudCartItemResponses));
        }

        // Bulk save new translations
        if (!newTranslationsToSave.isEmpty()) {
            productTranslationRepository.saveAll(newTranslationsToSave);
        }

        // Sort results by refund date or order date in descending order
        historyOrderResponses.sort(Comparator.comparing((OrderCancelPaymentRefund h) ->
                        h.getPayment().getDateRefund() != null ? h.getPayment().getDateRefund() : h.getOrder().getDateCreated(),
                Comparator.reverseOrder()));

        return ResponseEntity.ok(new ListAllOrderCancelAndPaymentRefund(historyOrderResponses.size(), historyOrderResponses));
    }

    // Helper method for product name translation with caching
    private String getProductTranslation(Product product, Map<Integer, ProductTranslation> productTranslationMap, Language language, List<ProductTranslation> newTranslationsToSave) {
        if (language == Language.VN) {
            return product.getProName();
        }

        // Check if translation is already in the cache
        ProductTranslation translation = productTranslationMap.get(product.getProId());

        if (translation == null) {
            translation = new ProductTranslation();
            translation.setProduct(product);
            translation.setIsDeleted(false);
            translation.setDateCreated(LocalDateTime.now());
            translation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
            translation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));

            // Collect translations to save later in bulk
            newTranslationsToSave.add(translation);
            productTranslationMap.put(product.getProId(), translation);
        }

        return translation.getProName();
    }




    // Helper method for first image URL extraction




//    @Transactional
//    public ResponseEntity<?> listOrderCancelAndPaymentRefundUser(int userId, Language language) {
//        // Truy vấn tất cả các đơn hàng đã hủy và có trạng thái refund, kết hợp cùng thông tin payment và cart item
//        List<Orders> orders = orderRepository.findAllByUserUserIdAndStatusAndPaymentStatus(userId, Status_Order.CANCELLED, Status_Payment.REFUND);
//
//        // Set để lưu tất cả các productIds từ các đơn hàng đã lọc
//        Set<Integer> productIds = orders.stream()
//                .flatMap(order -> order.getOrderItem().getCart().getCartItems().stream())
//                .map(cartItem -> cartItem.getProductVariants().getProduct().getProId())
//                .collect(Collectors.toSet());
//
//        // Truy vấn tất cả các bản dịch sản phẩm trong một lần duy nhất
//        Map<Integer, ProductTranslation> translationMap = new HashMap<>();
//        if (language == Language.EN && !productIds.isEmpty()) {
//            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
//            translations.forEach(pt -> translationMap.put(pt.getProduct().getProId(), pt));
//        }
//
//        List<OrderCancelPaymentRefund> historyOrderResponses = new ArrayList<>();
//
//        for (Orders order : orders) {
//            // Lọc các cartItem của mỗi đơn hàng
//            List<CRUDCartItemResponse> crudCartItemResponses = order.getOrderItem().getCart().getCartItems().stream()
//                    .map(cartItem -> {
//                        var product = cartItem.getProductVariants().getProduct();
//                        int productId = product.getProId();
//
//                        String productNameTrans;
//                        if (language == Language.VN) {
//                            productNameTrans = product.getProName();
//                        } else {
//                            ProductTranslation productTranslation = translationMap.get(productId);
//                            if (productTranslation == null) {
//                                // Tạo mới bản dịch nếu không có sẵn trong map
//                                productTranslation = new ProductTranslation();
//                                productTranslation.setProduct(product);
//                                productTranslation.setIsDeleted(false);
//                                productTranslation.setDateCreated(LocalDateTime.now());
//                                productTranslation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
//                                productTranslation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
//                                productTranslationRepository.save(productTranslation);
//                                translationMap.put(productId, productTranslation);
//                            }
//                            productNameTrans = productTranslation.getProName();
//                        }
//
//                        return new CRUDCartItemResponse(
//                                cartItem.getCartItemId(),
//                                productId,
//                                productNameTrans,
//                                cartItem.getCart().getCartId(),
//                                cartItem.getProductVariants().getSize(),
//                                cartItem.getTotalPrice(),
//                                cartItem.getQuantity(),
//                                extractFirstImage(product.getListProImg())
//                        );
//                    })
//                    .collect(Collectors.toList());
//
//            Payment payment = order.getPayment();
//            CRUDPaymentResponse crudPaymentResponse = new CRUDPaymentResponse(
//                    payment.getPaymentId(),
//                    payment.getAmount(),
//                    payment.getDateCreated(),
//                    payment.getDateDeleted(),
//                    payment.getDateRefunded(),
//                    payment.getIsDeleted(),
//                    payment.getPaymentMethod(),
//                    payment.getStatus(),
//                    payment.getOrder().getOrderId(),
//                    payment.getIsRefund(),
//                    payment.getLink()
//            );
//
//            // Tạo đối tượng response cho đơn hàng và payment
//            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
//                    order.getOrderId(),
//                    order.getAddress(),
//                    order.getDeliveryFee(),
//                    order.getDateCreated(),
//                    order.getDateDeleted(),
//                    order.getDateUpdated(),
//                    order.getDeliveryDate(),
//                    order.getDateCanceled(),
//                    order.getDiscountPrice(),
//                    order.getIsDeleted(),
//                    order.getNote(),
//                    order.getOrderDate(),
//                    order.getPhoneNumber(),
//                    order.getStatus(),
//                    order.getTotalPrice(),
//                    order.getUser().getUserId(),
//                    order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
//                    (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
//                    crudCartItemResponses
//            );
//
//            historyOrderResponses.add(new OrderCancelPaymentRefund(createOrdersResponse, crudPaymentResponse, crudCartItemResponses));
//        }
//
//        // Sắp xếp theo ngày refund hoặc ngày tạo order
//        historyOrderResponses.sort((h1, h2) -> {
//            if (h1.getPayment().getDateRefund() != null && h2.getPayment().getDateRefund() != null) {
//                return h2.getPayment().getDateRefund().compareTo(h1.getPayment().getDateRefund());
//            }
//            return h2.getOrder().getDateCreated().compareTo(h1.getOrder().getDateCreated());
//        });
//
//        return ResponseEntity.ok(new ListAllOrderCancelAndPaymentRefund(historyOrderResponses.size(), historyOrderResponses));
//    }


    public interface OrderRefundProjection {
        Integer getOrderId();
        String getAddress();
        Double getDeliveryFee();
        LocalDateTime getDateCreated();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateUpdated();
        LocalDateTime getDeliveryDate();
        LocalDateTime getDateCanceled();
        Double getDiscountPrice();
        Boolean getIsDeleted();
        String getNote();
        LocalDateTime getOrderDate();
        String getPhoneNumber();
        String getStatus(); // Enum to string
        Double getTotalPrice();
        Integer getUserId();
        Integer getVoucherId();
        Integer getPointCoinUse();

        // Payment
        Integer getPaymentId();
        Double getAmount();
        LocalDateTime getDateRefunded();
        String getPaymentMethod();
        String getPaymentStatus();
        Boolean getIsRefund();
        String getLink();

        // Cart Item
        Integer getCartItemId();
        Integer getCartId();
        String getSize();
        Integer getQuantity();
        Double getTotalCartItemPrice();

        // Product
        Integer getProductId();
        String getProName();
        String getProImg();
    }


    @Transactional
    public ResponseEntity<?> listOrderCancelAndPaymentRefundUser(int userId, Language language) {
        List<Orders> orders = orderRepository.findAllCancelledOrdersWithRefundAndFullDetails(userId);

        // Truy vấn tất cả productId cần dịch
        Set<Integer> productIds = orders.stream()
                .flatMap(o -> o.getOrderItem().getCart().getCartItems().stream())
                .map(c -> c.getProductVariants().getProduct().getProId())
                .collect(Collectors.toSet());

        Map<Integer, ProductTranslation> translationMap = new HashMap<>();
        if (language == Language.EN && !productIds.isEmpty()) {
            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
            for (ProductTranslation pt : translations) {
                translationMap.put(pt.getProduct().getProId(), pt);
            }
        }

        // Tạo response (chỉ là mapping từ dữ liệu đã được join sẵn)
        List<OrderCancelPaymentRefund> historyOrderResponses = orders.stream().map(order -> {
            List<CRUDCartItemResponse> cartItemResponses = order.getOrderItem().getCart().getCartItems().stream().map(cartItem -> {
                var product = cartItem.getProductVariants().getProduct();
                String productName = language == Language.VN ? product.getProName() :
                        Optional.ofNullable(translationMap.get(product.getProId()))
                                .map(ProductTranslation::getProName)
                                .orElseGet(() -> supportFunction.convertLanguage(product.getProName(), Language.EN));

                return new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        product.getProId(),
                        productName,
                        cartItem.getCart().getCartId(),
                        cartItem.getProductVariants().getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        extractFirstImage(product.getListProImg())
                );
            }).toList();

            var payment = order.getPayment();
            var paymentResponse = new CRUDPaymentResponse(
                    payment.getPaymentId(), payment.getAmount(), payment.getDateCreated(),
                    payment.getDateDeleted(), payment.getDateRefunded(), payment.getIsDeleted(),
                    payment.getPaymentMethod(), payment.getStatus(), payment.getOrder().getOrderId(),
                    payment.getIsRefund(), payment.getLink()
            );

            var orderResponse = new CreateOrdersResponse(
                    order.getOrderId(), order.getAddress(), order.getDeliveryFee(), order.getDateCreated(),
                    order.getDateDeleted(), order.getDateUpdated(), order.getDeliveryDate(),
                    order.getDateCanceled(), order.getDiscountPrice(), order.getIsDeleted(),
                    order.getNote(), order.getOrderDate(), order.getPhoneNumber(), order.getStatus(),
                    order.getTotalPrice(), order.getUser().getUserId(),
                    order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
                    order.getPointCoinUse(), cartItemResponses
            );

            return new OrderCancelPaymentRefund(orderResponse, paymentResponse, cartItemResponses);
        }).sorted((h1, h2) -> {
            var d1 = h1.getPayment().getDateRefund();
            var d2 = h2.getPayment().getDateRefund();
            return d2 != null && d1 != null ? d2.compareTo(d1)
                    : h2.getOrder().getDateCreated().compareTo(h1.getOrder().getDateCreated());
        }).toList();

        return ResponseEntity.ok(new ListAllOrderCancelAndPaymentRefund(historyOrderResponses.size(), historyOrderResponses));
    }




    @Transactional
    public ResponseEntity<?> getDetailOrder(int orderId,Language language) {
        Orders order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }

        Payment payment = order.getPayment();
        Shippment shipment = payment != null
                ? shipmentRepository.findByPaymentPaymentIdAndIsDeletedFalse(payment.getPaymentId())
                : null;
        List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
        List<CRUDCartItemResponse> crudCartItemResponses = cartItems.stream()
                .map(cartItem -> {
                    String productNameTrans = "";
                    if (language == Language.VN) {
                        productNameTrans = cartItem.getProductVariants().getProduct().getProName();
                    } else if (language == Language.EN) {
                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(
                                cartItem.getProductVariants().getProduct().getProId()
                        );

                        if (productTranslation != null) {
                            productNameTrans = productTranslation.getProName();
                        } else {
                            ProductTranslation newTranslation = new ProductTranslation();
                            newTranslation.setProduct(cartItem.getProductVariants().getProduct());
                            newTranslation.setIsDeleted(false);
                            newTranslation.setDateCreated(LocalDateTime.now());
                            newTranslation.setProName(
                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(), Language.EN)
                            );
                            newTranslation.setDescription(
                                    supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(), Language.EN)
                            );

                            productTranslationRepository.save(newTranslation);
                            productNameTrans = newTranslation.getProName();
                        }
                    }
                    String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
                    String firstImageUrl = null;

                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                        String[] imageEntries1 = currentProImg.split(", ");
                        if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
                            String[] parts = imageEntries1[0].split(": ");
                            if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
                                firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
                            }
                        }
                    }
                    return new CRUDCartItemResponse(
                            cartItem.getCartItemId(),
                            cartItem.getProductVariants().getProduct().getProId(),
                            productNameTrans,
                            cartItem.getCart().getCartId(),
                            cartItem.getProductVariants().getSize(),
                            cartItem.getTotalPrice(),
                            cartItem.getQuantity(),
                            firstImageUrl
                    );
                })
                .collect(Collectors.toList());
        CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                order.getOrderId(),
                order.getAddress(),
                order.getDeliveryFee(),
                order.getDateCreated(),
                order.getDateDeleted(),
                order.getDateUpdated(),
                order.getDeliveryDate(),
                order.getDateCanceled(),
                order.getDiscountPrice(),
                order.getIsDeleted(),
                order.getNote(),
                order.getOrderDate(),
                order.getPhoneNumber(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getUser().getUserId(),
                order.getVoucher() != null ? order.getVoucher().getVoucherId() : null,
                (order.getPointCoinUse() != null) ? order.getPointCoinUse() : null,
                crudCartItemResponses
        );

        CRUDPaymentResponse crudPaymentResponse = payment != null
                ? new CRUDPaymentResponse(
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
        )
                : null;

        CRUDShipmentResponse crudShipmentResponse = shipment != null
                ? new CRUDShipmentResponse(
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
                order.getUser().getFullName(),
                order.getUser().getUserId(),
                order.getAddress(),
                order.getUser().getPhoneNumber(),
                order.getUser().getEmail(),
                order.getOrderId()
        )
                : null;




        DetailOrderResponse detailOrderResponse = new DetailOrderResponse(
                order != null ? order.getUser().getFullName():null,
                createOrdersResponse,
                crudPaymentResponse,
                crudShipmentResponse,
                crudCartItemResponses
        );

        return ResponseEntity.status(HttpStatus.OK).body(detailOrderResponse);
    }

    private String extractFirstImageUrl(String imgList) {
        if (imgList == null || imgList.isBlank()) return null;
        try {
            String[] parts = imgList.split(", ");
            if (parts.length == 0) return null;
            String[] pair = parts[0].split(": ");
            return pair.length == 2 ? pair[1] : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public ResponseEntity<?> listOrderConfirmed(int userId, Language language) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        List<Orders> orders = orderRepository.findConfirmedOrdersWithShipmentAndCartItems(userId);
        Set<Integer> productIds = new HashSet<>();
        for (Orders order : orders) {
            order.getOrderItem().getCart().getCartItems().forEach(cartItem ->
                    productIds.add(cartItem.getProductVariants().getProduct().getProId()));
        }

        Map<Integer, ProductTranslation> translationMap = new HashMap<>();
        if (language == Language.EN) {
            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(productIds);
            for (ProductTranslation t : translations) {
                translationMap.put(t.getProduct().getProId(), t);
            }
        }

        List<HistoryOrderResponse> historyOrderResponses = new ArrayList<>();

        for (Orders order : orders) {
            Shippment shipment = order.getPayment().getShipment();
            List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();

            for (CartItem cartItem : order.getOrderItem().getCart().getCartItems()) {
                var product = cartItem.getProductVariants().getProduct();
                String productName = (language == Language.VN) ? product.getProName() : null;

                if (language == Language.EN) {
                    ProductTranslation translation = translationMap.get(product.getProId());
                    if (translation == null) {
                        translation = new ProductTranslation();
                        translation.setProduct(product);
                        translation.setIsDeleted(false);
                        translation.setDateCreated(LocalDateTime.now());
                        translation.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
                        translation.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
                        productTranslationRepository.save(translation);
                        translationMap.put(product.getProId(), translation);
                    }
                    productName = translation.getProName();
                }

                String image = extractFirstImageUrl(product.getListProImg());

                crudCartItemResponses.add(new CRUDCartItemResponse(
                        cartItem.getCartItemId(),
                        product.getProId(),
                        productName,
                        cartItem.getCart().getCartId(),
                        cartItem.getProductVariants().getSize(),
                        cartItem.getTotalPrice(),
                        cartItem.getQuantity(),
                        image
                ));
            }

            CreateOrdersResponse createOrdersResponse = new CreateOrdersResponse(
                    order.getOrderId(),
                    order.getAddress(),
                    order.getDeliveryFee(),
                    order.getDateCreated(),
                    order.getDateDeleted(),
                    order.getDateUpdated(),
                    order.getDeliveryDate(),
                    order.getDateCanceled(),
                    order.getDiscountPrice(),
                    order.getIsDeleted(),
                    order.getNote(),
                    order.getOrderDate(),
                    order.getPhoneNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getUser().getUserId(),
                    null,
                    order.getPointCoinUse(),
                    crudCartItemResponses
            );

            User shipper = shipment.getUser();
            User customer = order.getUser();

            CRUDShipmentResponse crudShipmentResponse = new CRUDShipmentResponse(
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
                    shipment.getPayment().getPaymentId(),
                    shipper != null ? shipper.getUserId() : null,
                    customer.getFullName(),
                    customer.getUserId(),
                    order.getAddress(),
                    customer.getPhoneNumber(),
                    customer.getEmail(),
                    order.getOrderId()
            );

            historyOrderResponses.add(new HistoryOrderResponse(createOrdersResponse, crudShipmentResponse, crudCartItemResponses));
        }

        historyOrderResponses.sort(Comparator.comparing(
                (HistoryOrderResponse h) -> Optional.ofNullable(h.getShipment().getDateShipped()).orElse(h.getOrder().getDateCreated())
        ).reversed());

        return ResponseEntity.ok(historyOrderResponses);
    }


    @Transactional
    public  ResponseEntity<?> CancelReason(CancelReasonReq req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        Orders orders = orderRepository.findByOrderIdAndUserUserIdAndIsDeletedFalse(req.getOrderId(), req.getUserId());
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        if(orders.getIsCancelReason() != null && orders.getIsCancelReason())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is accept");
        }
        if(orders.getIsCancelReason() != null && !orders.getIsCancelReason())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is reject");
        }
        if(orders.getIsCancelReason() == null)
        {
            orders.setCancelReason(req.getCancelReason());
            orderRepository.save(orders);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Transactional
    public ResponseEntity<?> listAllCancelReasonAwait() {
        try {
            List<Object[]> results = orderRepository.findCancelReasonAwaitInfo();
            List<CancelReasonResponse> listCancelReasonResponses = new ArrayList<>();

            for (Object[] result : results) {
                Integer orderId = (Integer) result[0];
                Integer userId = (Integer) result[1];
                CancelReason cancelReasonEnum = (CancelReason) result[2];
                CancelReasonResponse cancelReasonResponse = new CancelReasonResponse(
                        userId, orderId, cancelReasonEnum
                );
                listCancelReasonResponses.add(cancelReasonResponse);
            }

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ListAllCancelReasonResponse(listCancelReasonResponses.size(), listCancelReasonResponses)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching cancel reasons");
        }
    }


    @Transactional
    public ResponseEntity<?> acceptCancelReason(int orderId)
    {
        Orders orders = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        if(orders.getIsCancelReason() != null)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is accept or reject");
        }
        orders.setIsCancelReason(true);
        orders.setDateCanceled(LocalDateTime.now());
        orders.setStatus(Status_Order.CANCELLED);
        orderRepository.save(orders);
        Payment payment = orders.getPayment();

        Cart cart = orders.getOrderItem().getCart();
        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());

        for (CartItem cartItem : cartItems) {
            ProductVariants productVariants = cartItem.getProductVariants();
            productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
            productVariantsRepository.save(productVariants);
        }
        if(payment != null)
        {
            if(payment.getPaymentMethod() == Payment_Method.CREDIT && payment.getStatus() == Status_Payment.COMPLETED)
            {
                payment.setStatus(Status_Payment.REFUND);
                payment.setDateRefunded(LocalDateTime.now());
                if(payment.getAmount() == 0.0)
                {
                    payment.setIsRefund(true);
                }
                else
                {
                    payment.setIsRefund(false);
                }
                paymentRepository.save(payment);
            }
            if (payment.getPaymentMethod() == Payment_Method.CASH) {
                payment.setStatus(Status_Payment.FAILED);
                paymentRepository.save(payment);
            }
        }
        assert payment != null;
        Shippment shipment = payment.getShipment();
        if(shipment != null)
        {
            shipment.setDateCancel(LocalDateTime.now());
            shipment.setStatus(Status_Shipment.CANCELLED);
            shipmentRepository.save(shipment);
            ShipperDetail shipperDetail = shipperDetailRepository.findByUserUserId(shipment.getUser().getUserId());
            shipperDetail.setOnLeave(false);
            shipperDetail.setStatus("available");
            shipperDetailRepository.save(shipperDetail);

        }
        return ResponseEntity.status(HttpStatus.OK).body("Success");
    }

    @Transactional
    public ResponseEntity<?> rejectCancelReason(int orderId) {
        Orders orders = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        if(orders.getIsCancelReason() != null)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is accept or reject");
        }
        if (orders.getCancelReason() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not allow");
        }
        orders.setIsCancelReason(false);
        orders.setDateCanceled(LocalDateTime.now());
        orders.setStatus(Status_Order.CONFIRMED);
        orderRepository.save(orders);
        return ResponseEntity.ok("Success");
    }

    @Transactional
    public ResponseEntity<?> restoreOrder(int orderId) {
        Orders orders = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        Cart cart_restore = cartRepository.findByUserUserIdAndStatus(orders.getUser().getUserId(),Status_Cart.RESTORE);
        if(cart_restore != null)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Restore already exists");
        }
        Cart cart = orders.getOrderItem().getCart();
        Cart newCart = new Cart();
        User user = orders.getUser();
        newCart.setTotalPrice(0);
        newCart.setTotalProduct(0);
        newCart.setStatus(Status_Cart.RESTORE);
        newCart.setUser(cart.getUser());
        cartRepository.save(newCart);
        List<CartItem> cartItems = cart.getCartItems();
        double price = 0.0;
        int quantity = 0;
        for (CartItem cartItem : cartItems) {

            if(!cartItem.getProductVariants().getIsDeleted())
            {
                CartItem newCartItem = new CartItem();
                newCartItem.setQuantity(cartItem.getQuantity());
                newCartItem.setIsDeleted(false);
                newCartItem.setProductVariants(cartItem.getProductVariants());
                newCartItem.setCart(newCart);
                newCartItem.setTotalPrice(newCartItem.getQuantity() * newCartItem.getProductVariants().getPrice());
                cartItemRepository.save(newCartItem);

                if (newCartItem.getProductVariants().getStock() == 0) {
                    newCartItem.setQuantity(0);
                    newCartItem.setTotalPrice(0);
                    cartItemRepository.save(newCartItem);
                }
                if (newCartItem.getProductVariants().getStock() < newCartItem.getQuantity()) {
                    newCartItem.setQuantity(1);
                    newCartItem.setTotalPrice(newCartItem.getQuantity() * newCartItem.getProductVariants().getPrice());
                    cartItemRepository.save(newCartItem);
                }
                quantity += newCartItem.getQuantity();
                price += newCartItem.getQuantity() * newCartItem.getProductVariants().getPrice();
            }

        }
        newCart.setTotalPrice(price);
        newCart.setTotalProduct(quantity);
        cartRepository.save(newCart);

        return ResponseEntity.ok(new CreateNewCartResponse(
                newCart.getCartId(),
                newCart.getTotalPrice(),
                newCart.getTotalProduct(),
                newCart.getUser().getUserId(),
                newCart.getStatus()
        ));
    }

    @Transactional
    public ResponseEntity<?> restoreAddItemOrder(AddItemOrderConfirmRequest req) {
        Orders orders = orderRepository.findByOrderIdAndUserUserIdAndIsDeletedFalse(req.getOrderId(),req.getUserId());
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }
        if(orders.getStatus() != Status_Order.WAITING)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order is not waiting");
        }
        Cart cart = orders.getOrderItem().getCart();
        if(cart != null && cart.getStatus() == Status_Cart.COMPLETED)
        {
            cart.setStatus(Status_Cart.COMPLETED_PAUSE);
            cartRepository.save(cart);
        }
        return ResponseEntity.ok("Success");
    }

    @Transactional
    public ResponseEntity<?> checkTimeOrderRemain(Integer orderId) {
        LocalDateTime now = LocalDateTime.now();

        Orders orders = orderRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (orders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }

        if(orders.getStatus() == Status_Order.CANCELLED)
        {
            return ResponseEntity.ok("-1");
        }
        // Kiểm tra nếu đơn hàng đã được tạo quá 30 phút
        if (orders.getDateCreated().plusMinutes(30).isBefore(now)) {
            return ResponseEntity.ok("-1");  // Hết thời gian
        }
        Payment payment = orders.getPayment();
        if(payment != null)
        {
            return ResponseEntity.ok("2");
        }



        return ResponseEntity.ok("1");  // Còn trong thời gian
    }


    @Transactional
    public ResponseEntity<?> checkTimeOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Xử lý đơn hàng ở trạng thái WAITING
        List<Orders> waitingOrders = orderRepository.findAllByStatus(Status_Order.WAITING);
        for (Orders order : waitingOrders) {
            if (order.getDateCreated().plusMinutes(29).isBefore(now)) {
                order.setStatus(Status_Order.CANCELLED);

                Payment payment = order.getPayment();
                if (payment != null) {
                    payment.setStatus(Status_Payment.FAILED);
                    paymentRepository.save(payment);
                    List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
                    for(CartItem cartItem : cartItems) {
                        ProductVariants productVariants = cartItem.getProductVariants();
                        productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                        productVariantsRepository.save(productVariants);
                    }
                }

                Cart cart = order.getOrderItem().getCart();
                if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                {
                    cart.setStatus(Status_Cart.COMPLETED);
                    cartRepository.save(cart);
                }
                orderRepository.save(order);
            }
        }

        // Xử lý đơn hàng ở trạng thái CONFIRMED
        List<Orders> confirmedOrders = orderRepository.findAllByStatus(Status_Order.CONFIRMED);
        for (Orders order : confirmedOrders) {
            // Bỏ qua đơn không phải tạo hôm nay
            if (!order.getDateCreated().toLocalDate().isEqual(today)) {
                continue;
            }

            // Nếu quá 30 phút
            if (order.getDateCreated().plusMinutes(29).isBefore(now)) {
                Payment payment = order.getPayment();

                // Nếu có payment và shipment → bỏ qua không xử lý
                if (payment != null && payment.getShipment() != null) {
                    continue;
                }

                // Nếu có payment nhưng chưa có shipment
                if (payment != null) {
                    if(payment.getStatus() == Status_Payment.PENDING)
                    {
                        order.setStatus(Status_Order.CANCELLED);
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orderRepository.save(order);
                        List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
                        for(CartItem cartItem : cartItems) {
                            ProductVariants productVariants = cartItem.getProductVariants();
                            productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                            productVariantsRepository.save(productVariants);
                        }
                        Cart cart = order.getOrderItem().getCart();
                        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                        {
                            cart.setStatus(Status_Cart.COMPLETED);
                            cartRepository.save(cart);
                        }
                    }

                }

                if(payment == null)
                {
                    order.setStatus(Status_Order.CANCELLED);
                    orderRepository.save(order);
                    Cart cart = order.getOrderItem().getCart();
                    if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                    {
                        cart.setStatus(Status_Cart.COMPLETED);
                        cartRepository.save(cart);
                    }

                }
            }
        }

        return ResponseEntity.ok("Success");
    }


    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void checkTimeOrdersSchedule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Xử lý đơn hàng ở trạng thái WAITING
        List<Orders> waitingOrders = orderRepository.findAllByStatus(Status_Order.WAITING);
        for (Orders order : waitingOrders) {
            if (order.getDateCreated().plusMinutes(29).isBefore(now)) {
                order.setStatus(Status_Order.CANCELLED);

                Payment payment = order.getPayment();
                if (payment != null) {
                    payment.setStatus(Status_Payment.FAILED);
                    paymentRepository.save(payment);
                    List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
                    for(CartItem cartItem : cartItems) {
                        ProductVariants productVariants = cartItem.getProductVariants();
                        productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                        productVariantsRepository.save(productVariants);
                    }
                }
                Cart cart = order.getOrderItem().getCart();
                if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                {
                    cart.setStatus(Status_Cart.COMPLETED);
                    cartRepository.save(cart);
                }

                orderRepository.save(order);
            }
        }

        // Xử lý đơn hàng ở trạng thái CONFIRMED
        List<Orders> confirmedOrders = orderRepository.findAllByStatus(Status_Order.CONFIRMED);
        for (Orders order : confirmedOrders) {
            // Bỏ qua đơn không phải tạo hôm nay
            if (!order.getDateCreated().toLocalDate().isEqual(today)) {
                continue;
            }

            // Nếu quá 30 phút
            if (order.getDateCreated().plusMinutes(29).isBefore(now)) {
                Payment payment = order.getPayment();

                // Nếu có payment và shipment → bỏ qua không xử lý
                if (payment != null && payment.getShipment() != null) {
                    continue;
                }

                // Nếu có payment nhưng chưa có shipment
                if (payment != null) {
                    if(payment.getStatus() == Status_Payment.PENDING)
                    {
                        order.setStatus(Status_Order.CANCELLED);
                        payment.setStatus(Status_Payment.FAILED);
                        paymentRepository.save(payment);
                        orderRepository.save(order);
                        List<CartItem> cartItems = order.getOrderItem().getCart().getCartItems();
                        for(CartItem cartItem : cartItems) {
                            ProductVariants productVariants = cartItem.getProductVariants();
                            productVariants.setStock(productVariants.getStock() + cartItem.getQuantity());
                            productVariantsRepository.save(productVariants);
                        }
                        Cart cart = order.getOrderItem().getCart();
                        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                        {
                            cart.setStatus(Status_Cart.COMPLETED);
                            cartRepository.save(cart);
                        }

                    }

                }

                if(payment == null)
                {
                    order.setStatus(Status_Order.CANCELLED);
                    orderRepository.save(order);
                    Cart cart = order.getOrderItem().getCart();
                    if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.CART_AI)
                    {
                        cart.setStatus(Status_Cart.COMPLETED);
                        cartRepository.save(cart);
                    }

                }


            }
        }


    }



}
