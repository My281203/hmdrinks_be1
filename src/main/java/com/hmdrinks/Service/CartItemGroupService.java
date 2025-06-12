package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.CRUDCartItemResponse;
import com.hmdrinks.Response.ChangeSizeItemResponse;
import com.hmdrinks.Response.DeleteCartItemResponse;
import com.hmdrinks.Response.IncreaseDecreaseItemQuantityResponse;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CartItemGroupService {
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private  GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private  GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private NotificationService notificationService;


    @Transactional
    public ResponseEntity<?> insertCartItem(InsertItemToCart req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }

        Product product= productRepository.findByProIdAndIsDeletedFalse(req.getProId());
        if(product == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product Not Found");
        }
        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(),req.getProId());
        if(productVariants == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("production size not exists");
        }


        CartGroup  cart = cartGroupRepository.findByCartId(req.getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }
        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if(groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED )
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        if(req.getQuantity() < 0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
        }
        CartItemGroup cartItem1 = cartItemGroupRepository.findByProductVariants_VarIdAndProductVariants_SizeAndCartGroup_CartIdAndIsDeletedFalse(productVariants.getVarId(),req.getSize(),req.getCartId());
        CartItemGroup cartItem = new CartItemGroup();
        if(cartItem1 == null)
        {
            if(req.getQuantity() > productVariants.getStock())
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
            }
            Double totalPrice = req.getQuantity() * productVariants.getPrice();
            Integer stock_quantity = productVariants.getStock() - req.getQuantity();
            cartItem.setCartGroup(cart);
            cartItem.setQuantity(req.getQuantity());
            cartItem.setProductVariants(productVariants);
            cartItem.setItemPrice(productVariants.getPrice());
            cartItem.setTotalPrice(totalPrice);
            cartItem.setIsDeleted(false);
            cartItem.setIsDisabled(false);
            cartItem.setDateUpdated(LocalDateTime.now());
            cartItemGroupRepository.save(cartItem);
            List<CartItemGroup> cartItemList2 = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
            AtomicReference<Double> totalPrice1 = new AtomicReference<>(0.0);
            AtomicInteger totalQuantity = new AtomicInteger(0);

            cartItemList2.forEach(cartItem_update -> {
                double price = cartItem_update.getTotalPrice();       // Giá của sản phẩm
                int quantity = cartItem_update.getQuantity();   // Số lượng sản phẩm

                // Tính tổng giá và tổng số lượng
                totalPrice1.updateAndGet(v -> v + (price * quantity));
                totalQuantity.addAndGet(quantity);
            });

            double finalTotalPrice = totalPrice1.get();
            int finalTotalQuantity = totalQuantity.get();
            cart.setTotalProduct(finalTotalQuantity);
            cart.setTotalPrice(finalTotalPrice);
            cart.setDateUpdated(LocalDateTime.now());
            cartGroupRepository.save(cart);

            List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartItem.getCartGroup().getCartId());
            Double Price = 0.0;
            Integer Quantity=0;
            for(CartItemGroup cartItem2: cartItemList)
            {
                Price = Price + Double.valueOf(cartItem2.getTotalPrice());
                Quantity = Quantity + cartItem2.getQuantity();
            }
            cart.setTotalProduct(Quantity);
            cart.setTotalPrice(Price);
            cartGroupRepository.save(cart);

            GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
            groupOrderMember.setAmount(Price);
            groupOrderMember.setQuantity(Quantity);
            groupOrderMember.setDateUpdated(LocalDateTime.now());
            groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);

            groupOrderMembersRepository.save(groupOrderMember);


            GroupOrders groupOrders = groupOrderMember.getGroupOrder();
            GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
            List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
            Double Price_Group = 0.0;
            Integer Quantity_Group=0;
            for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
                if (groupOrderMember2.getAmount() != null) {
                    Price_Group += groupOrderMember2.getAmount();
                }
                if (groupOrderMember2.getQuantity() != null) {
                    Quantity_Group += groupOrderMember2.getQuantity();
                }
            }

            groupOrders1.setTotalQuantity(Quantity_Group);
            groupOrders1.setTotalPrice(Price_Group);
            groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
            groupOrders1.setDateUpdated(LocalDateTime.now());
            groupOrdersRepository.save(groupOrders1);

            try {
                String message = "Add new product";
                Integer currentUserId = req.getUserId();
                groupOrders1.getGroupOrderMembers().stream()
                        .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId))
                        .forEach(m -> {
                            try {
                                Integer receiverId = m.getUser().getUserId();
                                notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                                System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                            } catch (Exception ex) {
                                System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo về việc thêm sản phẩm: " + e.getMessage());
            }

            String product_name_trans = "";
            if(req.getLanguage() == Language.VN)
            {
                product_name_trans = cartItem.getProductVariants().getProduct().getProName();
            }
            else if(req.getLanguage() == Language.EN)
            {
                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
                if(productTranslation != null)
                {
                    product_name_trans = productTranslation.getProName();
                } else if (product_name_trans.equals("") && productTranslation == null) {
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

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCartItemResponse(
                    cartItem.getCartItemId(),
                    cartItem.getProductVariants().getProduct().getProId(),
                    product_name_trans,
                    cartItem.getCartGroup().getCartId(),
                    cartItem.getProductVariants().getSize(),
                    cartItem.getTotalPrice(),
                    cartItem.getQuantity(),
                    firstImageUrl
            ));
        }
        else
        {
            if((req.getQuantity() + cartItem1.getQuantity()) > productVariants.getStock())
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
            }

// Cập nhật lại quantity và totalPrice
            Integer newQuantity = req.getQuantity() + cartItem1.getQuantity();
            Double newTotalPrice = newQuantity * productVariants.getPrice();

            cartItem1.setQuantity(newQuantity);
            cartItem1.setTotalPrice(newTotalPrice);
            cartItem1.setItemPrice(productVariants.getPrice());
            cartItem1.setDateUpdated(LocalDateTime.now());
            cartItem1.setIsDeleted(false);
            cartItem1.setIsDisabled(false);

            cartItemGroupRepository.save(cartItem1);


            List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartId());
            Double totalCartPrice = 0.0;
            Integer totalCartQuantity = 0;
            for (CartItemGroup cartItem2 : cartItemList) {
                if(cartItem2 .getTotalPrice() != null)
                {
                    totalCartPrice += cartItem2.getTotalPrice();
                }
                if(cartItem2.getQuantity() != null )

                    totalCartQuantity += cartItem2.getQuantity();
            }

            cart.setTotalProduct(totalCartQuantity);
            cart.setTotalPrice(totalCartPrice);
            cart.setDateUpdated(LocalDateTime.now());
            cartGroupRepository.save(cart);

            GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
            groupOrderMember.setAmount(totalCartPrice);
            groupOrderMember.setQuantity(totalCartQuantity);
            groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
            groupOrderMember.setDateUpdated(LocalDateTime.now());
            groupOrderMembersRepository.save(groupOrderMember);

            GroupOrders groupOrders = groupOrderMember.getGroupOrder();
            GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
            List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
            Double Price_Group = 0.0;
            Integer Quantity_Group=0;
            for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
                if (groupOrderMember2.getAmount() != null) {
                    Price_Group += groupOrderMember2.getAmount();
                }
                if (groupOrderMember2.getQuantity() != null) {
                    Quantity_Group += groupOrderMember2.getQuantity();
                }
            }

            groupOrders1.setTotalQuantity(Quantity_Group);
            groupOrders1.setTotalPrice(Price_Group);
            groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
            groupOrders1.setDateUpdated(LocalDateTime.now());
            groupOrdersRepository.save(groupOrders1);

            try {
                String message = "Add new product";
                Integer currentUserId = req.getUserId();
                groupOrders1.getGroupOrderMembers().stream()
                        .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId))
                        .forEach(m -> {
                            try {
                                Integer receiverId = m.getUser().getUserId();
                                notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                                System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                            } catch (Exception ex) {
                                System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo về việc thêm sản phẩm: " + e.getMessage());
            }

            String product_name_trans = "";
            if(req.getLanguage() == Language.VN)
            {
                product_name_trans = cartItem1.getProductVariants().getProduct().getProName();
            }
            else if(req.getLanguage() == Language.EN)
            {
                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem1.getProductVariants().getProduct().getProId());
                if(productTranslation != null)
                {
                    product_name_trans = productTranslation.getProName();
                }
                else if (product_name_trans.equals("") && productTranslation == null) {
                    ProductTranslation productTranslation1 = new ProductTranslation();
                    productTranslation1.setProduct(cartItem1.getProductVariants().getProduct());
                    productTranslation1.setIsDeleted(false);
                    productTranslation1.setDateCreated(LocalDateTime.now());
                    productTranslation1.setProName(supportFunction.convertLanguage(cartItem1.getProductVariants().getProduct().getProName(),Language.EN));
                    productTranslation1.setDescription(supportFunction.convertLanguage(cartItem1.getProductVariants().getProduct().getDescription(),Language.EN));
                    productTranslationRepository.save(productTranslation1);
                    product_name_trans = productTranslation1.getProName();
                }
            }
            String currentProImg = cartItem1.getProductVariants().getProduct().getListProImg();
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
            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCartItemResponse(
                    cartItem1.getCartItemId(),
                    cartItem1.getProductVariants().getProduct().getProId(),
                    product_name_trans,
                    cartItem1.getCartGroup().getCartId(),
                    cartItem1.getProductVariants().getSize(),
                    cartItem1.getTotalPrice(),
                    cartItem1.getQuantity(),
                    firstImageUrl
            ));
        }
    }


    @Transactional
    public ResponseEntity<?> increaseCartItemGroupQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItemGroup cartItem = cartItemGroupRepository.findByCartItemIdAndIsDisabledFalse(req.getCartItemId());
        if(cartItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }
        if(req.getQuantity() <= 0)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
        }
        ProductVariants productVariants = productVariantsRepository.findByVarId(cartItem.getProductVariants().getVarId());
        int Present_Quantity = cartItem.getQuantity() + 1 ;
        double Present_TotalPrice = productVariants.getPrice() * Present_Quantity;


        if((req.getQuantity() + 1 ) > productVariants.getStock())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
        }
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));
        cartItem.setDateUpdated(LocalDateTime.now());

        cartItemGroupRepository.save(cartItem);
        CartGroup cart = cartGroupRepository.findByCartId(cartItem.getCartGroup().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if(groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED )
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartItem.getCartGroup().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItemGroup cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cart.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cart);

        GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
        groupOrderMember.setAmount(Price);
        groupOrderMember.setQuantity(Quantity);
        groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
        groupOrderMember.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(groupOrderMember);

        GroupOrders groupOrders = groupOrderMember.getGroupOrder();
        GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
        List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
        Double Price_Group = 0.0;
        Integer Quantity_Group=0;
        for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
            if (groupOrderMember2.getAmount() != null) {
                Price_Group += groupOrderMember2.getAmount();
            }
            if (groupOrderMember2.getQuantity() != null) {
                Quantity_Group += groupOrderMember2.getQuantity();
            }
        }

        groupOrders1.setTotalQuantity(Quantity_Group);
        groupOrders1.setTotalPrice(Price_Group);
        groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
        groupOrders1.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders1);

        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }


    @Transactional
    public ResponseEntity<?> updateCartItemQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItemGroup cartItem = cartItemGroupRepository.findByCartItemIdAndIsDisabledFalse(req.getCartItemId());
        if(cartItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }
        if(req.getQuantity() <= 0)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
        }
        ProductVariants productVariants = productVariantsRepository.findByVarId(cartItem.getProductVariants().getVarId());
        int Present_Quantity = req.getQuantity();  ;
        double Present_TotalPrice = productVariants.getPrice() * Present_Quantity;
        System.out.println(productVariants.getStock());

        if((req.getQuantity() ) > productVariants.getStock())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
        }
        if(productVariants.getStock() == 0)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Out of stock");
        }
        CartGroup cart = cartGroupRepository.findByCartId(cartItem.getCartGroup().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if(groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED )
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));
        cartItem.setDateUpdated(LocalDateTime.now());

        cartItemGroupRepository.save(cartItem);

        List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartItem.getCartGroup().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItemGroup cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cart.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cart);

        GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
        groupOrderMember.setAmount(Price);
        groupOrderMember.setQuantity(Quantity);
        groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
        groupOrderMember.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(groupOrderMember);

        GroupOrders groupOrders = groupOrderMember.getGroupOrder();
        GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
        List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
        Double Price_Group = 0.0;
        Integer Quantity_Group=0;
        for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
            if (groupOrderMember2.getAmount() != null) {
                Price_Group += groupOrderMember2.getAmount();
            }
            if (groupOrderMember2.getQuantity() != null) {
                Quantity_Group += groupOrderMember2.getQuantity();
            }
        }

        groupOrders1.setTotalQuantity(Quantity_Group);
        groupOrders1.setTotalPrice(Price_Group);
        groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
        groupOrders1.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders1);

        Integer currentUserId = req.getUserId();

        try {
            String message = "Change quantity";
            groupOrders1.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId)) // loại bỏ người tự cập nhật
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo về việc thay đổi số lượng sản phẩm: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }

    @Transactional
    public ResponseEntity<?> changeSizeCartItemQuantity(ChangeSizeItemReq req) {
        CartItemGroup cartItem = cartItemGroupRepository.findByCartItemIdAndIsDisabledFalse(req.getCartItemId());
        if (cartItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }

        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(), cartItem.getProductVariants().getProduct().getProId());
        if (productVariants == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ProductVariants Not Found with Size");
        }

        CartGroup cart = cartGroupRepository.findByCartId(cartItem.getCartGroup().getCartId());
        if (cart == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if (groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        cartItem.setProductVariants(productVariants);
        double presentTotalPrice = productVariants.getPrice() * cartItem.getQuantity();
        cartItem.setTotalPrice(presentTotalPrice);
        cartItem.setDateUpdated(LocalDateTime.now());
        cartItemGroupRepository.save(cartItem);

        // Cập nhật tổng giá trị của giỏ hàng và số lượng sản phẩm trong giỏ hàng
        List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartItem.getCartGroup().getCartId());
        Double price = 0.0;
        Integer quantity = 0;
        for (CartItemGroup cartItem2 : cartItemList) {
            price += cartItem2.getTotalPrice();
            quantity += cartItem2.getQuantity();
        }
        cart.setTotalProduct(quantity);
        cart.setTotalPrice(price);
        cart.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cart);

        GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
        groupOrderMember.setAmount(price);
        groupOrderMember.setQuantity(quantity);
        groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
        groupOrderMember.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(groupOrderMember);

        // Cập nhật lại nhóm đơn hàng
        GroupOrders groupOrders = groupOrderMember.getGroupOrder();
        GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
        List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
        Double priceGroup = 0.0;
        Integer quantityGroup = 0;
        for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
            if (groupOrderMember2.getAmount() != null) {
                priceGroup += groupOrderMember2.getAmount();
            }
            if (groupOrderMember2.getQuantity() != null) {
                quantityGroup += groupOrderMember2.getQuantity();
            }
        }

        groupOrders1.setTotalQuantity(quantityGroup);
        groupOrders1.setTotalPrice(priceGroup);
        groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
        groupOrders1.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders1);

        // Gửi thông báo cho các thành viên trong nhóm
        // Lấy userId của người đang cập nhật sản phẩm
        Integer currentUserId = req.getUserId();

        try {
            String message = "Change size";
            groupOrders1.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId)) // loại bỏ người tự cập nhật
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo về việc thay đổi sản phẩm: " + e.getMessage());
        }


        return ResponseEntity.status(HttpStatus.OK).body(new ChangeSizeItemResponse(req.getSize(), quantity, presentTotalPrice));
    }



    @Transactional
    public ResponseEntity<?> decreaseCartItemQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItemGroup cartItem = cartItemGroupRepository.findByCartItemIdAndIsDisabledFalse(req.getCartItemId());
        if(cartItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }
        if(req.getQuantity() <= 0)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
        }
        ProductVariants productVariants = productVariantsRepository.findByVarId(cartItem.getProductVariants().getVarId());
        int Present_Quantity = cartItem.getQuantity() - 1 ;
        double Present_TotalPrice = productVariants.getPrice() * Present_Quantity;


        if((req.getQuantity() - 1 ) > productVariants.getStock())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
        }
        CartGroup cart = cartGroupRepository.findByCartId(cartItem.getCartGroup().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if(groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED )
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));
        cartItem.setDateUpdated(LocalDateTime.now());

        cartItemGroupRepository.save(cartItem);

        List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartItem.getCartGroup().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItemGroup cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cart.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cart);

        GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
        groupOrderMember.setAmount(Price);
        groupOrderMember.setQuantity(Quantity);
        groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
        groupOrderMember.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(groupOrderMember);

        GroupOrders groupOrders = groupOrderMember.getGroupOrder();
        GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
        List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
        Double Price_Group = 0.0;
        Integer Quantity_Group=0;
        for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
            if (groupOrderMember2.getAmount() != null) {
                Price_Group += groupOrderMember2.getAmount();
            }
            if (groupOrderMember2.getQuantity() != null) {
                Quantity_Group += groupOrderMember2.getQuantity();
            }
        }

        groupOrders1.setTotalQuantity(Quantity_Group);
        groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
        groupOrders1.setTotalPrice(Price_Group);
        groupOrders1.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders1);

        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }

    @Transactional
    public ResponseEntity<?> deleteOneItem(DeleteOneCartItemReq req) {
        CartItemGroup cartItem = cartItemGroupRepository.findByCartItemIdAndIsDisabledFalse(req.getCartItemId());
        if (cartItem == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }

        CartGroup cart = cartItem.getCartGroup();
        GroupOrders groupOrders_check = cart.getGroupOrderMember().getGroupOrder();
        if (groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED ||
                groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT ||
                groupOrders_check.getStatus() == StatusGroupOrder.CANCELED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        cartItemGroupRepository.delete(cartItem);

        // Cập nhật lại cart sau khi xóa
        List<CartItemGroup> cartItemList = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
        double totalPrice = 0.0;
        int totalQuantity = 0;
        for (CartItemGroup item : cartItemList) {
            totalPrice += item.getTotalPrice();
            totalQuantity += item.getQuantity();
        }

        cart.setTotalProduct(totalQuantity);
        cart.setTotalPrice(totalPrice);
        cart.setDateUpdated(LocalDateTime.now());
        cartGroupRepository.save(cart);

        // Cập nhật member
        GroupOrderMember groupOrderMember = cart.getGroupOrderMember();
        groupOrderMember.setAmount(totalPrice);
        groupOrderMember.setQuantity(totalQuantity);
        groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
        groupOrderMember.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(groupOrderMember);

        // Cập nhật group
        GroupOrders groupOrders = groupOrderMember.getGroupOrder();
        GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());

        double groupTotalPrice = 0.0;
        int groupTotalQuantity = 0;
        for (GroupOrderMember member : groupOrders1.getGroupOrderMembers()) {
            if (member.getAmount() != null) groupTotalPrice += member.getAmount();
            if (member.getQuantity() != null) groupTotalQuantity += member.getQuantity();
        }

        groupOrders1.setTotalQuantity(groupTotalQuantity);
        groupOrders1.setTotalPrice(groupTotalPrice);
        groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
        groupOrders1.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders1);

        // Gửi thông báo cho các thành viên khác trong nhóm
        try {
            String message = "Remove product";
            Integer currentUserId = req.getUserId();
            groupOrders1.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId))
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo về việc xóa sản phẩm: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse(
                "Delete item success"
        ));
    }



    @Transactional
    public ResponseEntity<?> deleteAllCartItem(DeleteAllCartItemReq req) {
        CartGroup cartRestore = cartGroupRepository.findByCartId(req.getCartId());
        GroupOrders groupOrders_check = cartRestore.getGroupOrderMember().getGroupOrder();
        if(groupOrders_check.getStatus() == StatusGroupOrder.COMPLETED || groupOrders_check.getStatus() == StatusGroupOrder.CHECKOUT || groupOrders_check.getStatus() == StatusGroupOrder.CANCELED )
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrders_check.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrders_check.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        if (cartRestore != null) {

            cartItemGroupRepository.deleteAllByCartId(cartRestore.getCartId());
            cartRestore.setTotalProduct(0);
            cartRestore.setTotalPrice(0);
            cartRestore.setDateUpdated(LocalDateTime.now());
            cartGroupRepository.save(cartRestore);
            GroupOrderMember groupOrderMember = cartRestore.getGroupOrderMember();
            groupOrderMember.setAmount(0.0);
            groupOrderMember.setStatus(StatusGroupOrderMember.SHOPPING);
            groupOrderMember.setQuantity(0);
            groupOrderMember.setDateUpdated(LocalDateTime.now());
            groupOrderMembersRepository.save(groupOrderMember);

            GroupOrders groupOrders = groupOrderMember.getGroupOrder();
            GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
            List<GroupOrderMember> groupOrderMemberList = groupOrders1.getGroupOrderMembers();
            Double Price_Group = 0.0;
            Integer Quantity_Group=0;
            for (GroupOrderMember groupOrderMember2 : groupOrderMemberList) {
                if (groupOrderMember2.getAmount() != null) {
                    Price_Group += groupOrderMember2.getAmount();
                }
                if (groupOrderMember2.getQuantity() != null) {
                    Quantity_Group += groupOrderMember2.getQuantity();
                }
            }

            groupOrders1.setTotalQuantity(Quantity_Group);
            groupOrders1.setTotalPrice(Price_Group);
            groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
            groupOrders1.setDateUpdated(LocalDateTime.now());
            groupOrdersRepository.save(groupOrders1);
            try {
                String message = "Remove all product";
                Integer currentUserId = req.getUserId(); // <-- Nhớ truyền userId vào request
                groupOrders1.getGroupOrderMembers().stream()
                        .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(currentUserId))
                        .forEach(m -> {
                            try {
                                Integer receiverId = m.getUser().getUserId();
                                notificationService.sendProductUpdateNotification(receiverId, groupOrders1.getGroupOrderId(), message);
                                System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                            } catch (Exception ex) {
                                System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo về việc xóa toàn bộ sản phẩm: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse("Delete all item success"));
        }


        return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse("Delete all item success"));
    }

}