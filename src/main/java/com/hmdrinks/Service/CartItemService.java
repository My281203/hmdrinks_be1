package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CartItemService {
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


//    @Transactional
//    public ResponseEntity<?> insertCartItem(InsertItemToCart req)
//    {
//        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
//        if(user == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
//        }
//        Product product= productRepository.findByProIdAndIsDeletedFalse(req.getProId());
//        if(product == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product Not Found");
//        }
//
//        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(),req.getProId());
//        if(productVariants == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("production size not exists");
//        }
//        Cart cart1 = cartRepository.findByCartId(req.getCartId());
//
//        if (cart1.getStatus() != Status_Cart.NEW && cart1.getStatus() != Status_Cart.CART_AI && cart1.getStatus() != Status_Cart.COMPLETED_PAUSE && cart1.getStatus() != Status_Cart.RESTORE) {
//
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
//        }
//
//        Cart cart = cartRepository.findByCartId(req.getCartId());
//        if(cart == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
//        }
//        if(req.getQuantity() < 0){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
//        }
//        CartItem cartItem1 = cartItemRepository.findByProductVariants_VarIdAndProductVariants_SizeAndCart_CartId(productVariants.getVarId(),req.getSize(),req.getCartId());
//        CartItem cartItem = new CartItem();
//        if(cartItem1 == null)
//        {
//            if(req.getQuantity() > productVariants.getStock())
//            {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
//            }
//            Double totalPrice = req.getQuantity() * productVariants.getPrice();
//            Integer stock_quantity = productVariants.getStock() - req.getQuantity();
//            cartItem.setCart(cart);
//            cartItem.setQuantity(req.getQuantity());
//            cartItem.setProductVariants(productVariants);
//            cartItem.setIsDeleted(false);
//            cartItem.setIsDisabled(false);
//            cartItem.setTotalPrice(totalPrice);
//            cartItemRepository.save(cartItem);
//            List<CartItem> cartItemList2 = cart.getCartItems();
//            AtomicReference<Double> totalPrice1 = new AtomicReference<>(0.0);
//            AtomicInteger totalQuantity = new AtomicInteger(0);
//
//            cartItemList2.forEach(cartItem_update -> {
//                // Lấy giá và số lượng từ từng CartItem
//                double price = cartItem_update.getTotalPrice();       // Giá của sản phẩm
//                int quantity = cartItem_update.getQuantity();   // Số lượng sản phẩm
//
//                // Tính tổng giá và tổng số lượng
//                totalPrice1.updateAndGet(v -> v + (price * quantity));
//                totalQuantity.addAndGet(quantity);
//            });
//
//            double finalTotalPrice = totalPrice1.get();
//            int finalTotalQuantity = totalQuantity.get();
//            cart.setTotalProduct(finalTotalQuantity);
//            cart.setTotalPrice(finalTotalPrice);
//            cartRepository.save(cart);
//
//            List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
//            Double Price = 0.0;
//            Integer Quantity=0;
//            for(CartItem cartItem2: cartItemList)
//            {
//                Price = Price + Double.valueOf(cartItem2.getTotalPrice());
//                Quantity = Quantity + cartItem2.getQuantity();
//            }
//            cart.setTotalProduct(Quantity);
//            cart.setTotalPrice(Price);
//            cartRepository.save(cart);
//            if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
//            {
//                OrderItem orderItem = cart.getOrderItem();
//                orderItem.setTotalPrice(Price);
//                orderItem.setQuantity(Quantity);
//                orderItem.setCart(cart);
//                orderItem.setDateUpdated(LocalDateTime.now());
//                orderItemRepository.save(orderItem);
//
//
//                Orders order = orderItem.getOrder();
//                order.setTotalPrice(Price);
//                order.setDateUpdated(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//            String product_name_trans = "";
//            if(req.getLanguage() == Language.VN)
//            {
//                product_name_trans = cartItem.getProductVariants().getProduct().getProName();
//            }
//            else if(req.getLanguage() == Language.EN)
//            {
//                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
//                if(productTranslation != null)
//                {
//                    product_name_trans = productTranslation.getProName();
//                } else if (product_name_trans.equals("") && productTranslation == null) {
//                    ProductTranslation productTranslation1 = new ProductTranslation();
//                    productTranslation1.setProduct(cartItem.getProductVariants().getProduct());
//                    productTranslation1.setIsDeleted(false);
//                    productTranslation1.setDateCreated(LocalDateTime.now());
//                    productTranslation1.setProName(supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getProName(),Language.EN));
//                    productTranslation1.setDescription(supportFunction.convertLanguage(cartItem.getProductVariants().getProduct().getDescription(),Language.EN));
//                    productTranslationRepository.save(productTranslation1);
//                    product_name_trans = productTranslation1.getProName();
//                }
//
//            }
//            String currentProImg = cartItem.getProductVariants().getProduct().getListProImg();
//            String firstImageUrl = null;
//
//            if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                String[] imageEntries1 = currentProImg.split(", ");
//                if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                    String[] parts = imageEntries1[0].split(": ");
//                    if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                        firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                    }
//                }
//            }
//            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCartItemResponse(
//                    cartItem.getCartItemId(),
//                    cartItem.getProductVariants().getProduct().getProId(),
//                    product_name_trans,
//                    cartItem.getCart().getCartId(),
//                    cartItem.getProductVariants().getSize(),
//                    cartItem.getTotalPrice(),
//                    cartItem.getQuantity(),
//                    firstImageUrl
//            ));
//        }
//        else
//        {
//            if((req.getQuantity() + cartItem1.getQuantity()) > productVariants.getStock())
//            {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
//            }
//
//// Cập nhật lại quantity và totalPrice
//            Integer newQuantity = req.getQuantity() + cartItem1.getQuantity();
//            Double newTotalPrice = newQuantity * productVariants.getPrice();
//
//            cartItem1.setQuantity(newQuantity);
//            cartItem1.setTotalPrice(newTotalPrice);
//
//// Lưu lại thay đổi vào database
//            cartItemRepository.save(cartItem1);
//
//// Cập nhật tổng số lượng và giá tiền của giỏ hàng
//            List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(req.getCartId());
//            Double totalCartPrice = 0.0;
//            Integer totalCartQuantity = 0;
//            for (CartItem cartItem2 : cartItemList) {
//                totalCartPrice += cartItem2.getTotalPrice();
//                totalCartQuantity += cartItem2.getQuantity();
//            }
//
//            cart.setTotalProduct(totalCartQuantity);
//            cart.setTotalPrice(totalCartPrice);
//            cartRepository.save(cart);
//
//// Nếu giỏ hàng đã tồn tại trong đơn hàng, cập nhật đơn hàng
//            if (cart.getStatus() == Status_Cart.COMPLETED_PAUSE) {
//                OrderItem orderItem = cart.getOrderItem();
//                orderItem.setTotalPrice(totalCartPrice);
//                orderItem.setQuantity(totalCartQuantity);
//                orderItem.setCart(cart);
//                orderItem.setDateUpdated(LocalDateTime.now());
//                orderItemRepository.save(orderItem);
//
//                Orders order = orderItem.getOrder();
//                order.setTotalPrice(totalCartPrice);
//                order.setDateUpdated(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//            String product_name_trans = "";
//            if(req.getLanguage() == Language.VN)
//            {
//                product_name_trans = cartItem1.getProductVariants().getProduct().getProName();
//            }
//            else if(req.getLanguage() == Language.EN)
//            {
//                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem1.getProductVariants().getProduct().getProId());
//                if(productTranslation != null)
//                {
//                    product_name_trans = productTranslation.getProName();
//                }
//                else if (product_name_trans.equals("") && productTranslation == null) {
//                    ProductTranslation productTranslation1 = new ProductTranslation();
//                    productTranslation1.setProduct(cartItem1.getProductVariants().getProduct());
//                    productTranslation1.setIsDeleted(false);
//                    productTranslation1.setDateCreated(LocalDateTime.now());
//                    productTranslation1.setProName(supportFunction.convertLanguage(cartItem1.getProductVariants().getProduct().getProName(),Language.EN));
//                    productTranslation1.setDescription(supportFunction.convertLanguage(cartItem1.getProductVariants().getProduct().getDescription(),Language.EN));
//                    productTranslationRepository.save(productTranslation1);
//                    product_name_trans = productTranslation1.getProName();
//                }
//            }
//            String currentProImg = cartItem1.getProductVariants().getProduct().getListProImg();
//            String firstImageUrl = null;
//
//            if (currentProImg != null && !currentProImg.trim().isEmpty()) {
//                String[] imageEntries1 = currentProImg.split(", ");
//                if (imageEntries1.length > 0) {  // Kiểm tra có ảnh không
//                    String[] parts = imageEntries1[0].split(": ");
//                    if (parts.length == 2) {  // Đảm bảo đúng định dạng "stt: url"
//                        firstImageUrl = parts[1];  // Lấy URL ảnh đầu tiên
//                    }
//                }
//            }
//            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCartItemResponse(
//                    cartItem1.getCartItemId(),
//                    cartItem1.getProductVariants().getProduct().getProId(),
//                    product_name_trans,
//                    cartItem1.getCart().getCartId(),
//                    cartItem1.getProductVariants().getSize(),
//                    cartItem1.getTotalPrice(),
//                    cartItem1.getQuantity(),
//                    firstImageUrl
//            ));
//        }
//    }

    @Transactional
    public ResponseEntity<?> insertCartItem(InsertItemToCart req) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found");
        }

        Product product = productRepository.findByProIdAndIsDeletedFalse(req.getProId());
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product Not Found");
        }

        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(), req.getProId());
        if (productVariants == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product size not exists");
        }

        Cart cart = cartRepository.findByCartId(req.getCartId());
        if (cart == null || !(cart.getStatus() == Status_Cart.NEW || cart.getStatus() == Status_Cart.CART_AI ||
                cart.getStatus() == Status_Cart.COMPLETED_PAUSE || cart.getStatus() == Status_Cart.RESTORE)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        if (req.getQuantity() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0");
        }

        CartItem existingItem = cartItemRepository.findByProductVariants_VarIdAndProductVariants_SizeAndCart_CartId(
                productVariants.getVarId(), req.getSize(), req.getCartId());

        CartItem cartItem;
        if (existingItem == null) {
            if (req.getQuantity() > productVariants.getStock()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
            }

            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setQuantity(req.getQuantity());
            cartItem.setProductVariants(productVariants);
            cartItem.setIsDeleted(false);
            cartItem.setIsDisabled(false);
            cartItem.setTotalPrice(req.getQuantity() * productVariants.getPrice());

            cartItemRepository.save(cartItem);
        } else {
            int newQuantity = req.getQuantity() + existingItem.getQuantity();
            if (newQuantity > productVariants.getStock()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
            }

            existingItem.setQuantity(newQuantity);
            existingItem.setTotalPrice(newQuantity * productVariants.getPrice());
            cartItemRepository.save(existingItem);
            cartItem = existingItem;
        }

        // Cập nhật cart: tổng giá + tổng số lượng
        List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart.getCartId());
        double totalCartPrice = cartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
        int totalCartQuantity = cartItems.stream().mapToInt(CartItem::getQuantity).sum();

        cart.setTotalProduct(totalCartQuantity);
        cart.setTotalPrice(totalCartPrice);
        cartRepository.save(cart);

        // Nếu cart đã gắn với order => update order
        if (cart.getStatus() == Status_Cart.COMPLETED_PAUSE && cart.getOrderItem() != null) {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(totalCartPrice);
            orderItem.setQuantity(totalCartQuantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);

            Orders order = orderItem.getOrder();
            order.setTotalPrice(totalCartPrice);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }

        // Lấy tên sản phẩm theo ngôn ngữ
        String productName = getTranslatedProductName(cartItem.getProductVariants().getProduct(), req.getLanguage());

        // Lấy ảnh đầu tiên
        String firstImageUrl = extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg());

        // Trả về kết quả
        return ResponseEntity.ok(new CRUDCartItemResponse(
                cartItem.getCartItemId(),
                cartItem.getProductVariants().getProduct().getProId(),
                productName,
                cartItem.getCart().getCartId(),
                cartItem.getProductVariants().getSize(),
                cartItem.getTotalPrice(),
                cartItem.getQuantity(),
                firstImageUrl
        ));
    }

    // Hàm phụ: lấy ảnh đầu tiên
    private String extractFirstImageUrl(String imageList) {
        if (imageList != null && !imageList.trim().isEmpty()) {
            String[] entries = imageList.split(", ");
            if (entries.length > 0) {
                String[] parts = entries[0].split(": ");
                if (parts.length == 2) return parts[1];
            }
        }
        return null;
    }

    // Hàm phụ: xử lý tên sản phẩm đa ngôn ngữ
    private String getTranslatedProductName(Product product, Language language) {
        if (language == Language.VN) {
            return product.getProName();
        }

        ProductTranslation translation = productTranslationRepository.findByProduct_ProId(product.getProId());
        if (translation != null) {
            return translation.getProName();
        }

        ProductTranslation newTrans = new ProductTranslation();
        newTrans.setProduct(product);
        newTrans.setIsDeleted(false);
        newTrans.setDateCreated(LocalDateTime.now());
        newTrans.setProName(supportFunction.convertLanguage(product.getProName(), Language.EN));
        newTrans.setDescription(supportFunction.convertLanguage(product.getDescription(), Language.EN));
        productTranslationRepository.save(newTrans);
        return newTrans.getProName();
    }


    @Transactional
    public ResponseEntity<?> increaseCartItemQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartItemId());
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
        System.out.println(productVariants.getStock());

        if((req.getQuantity() + 1 ) > productVariants.getStock())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
        }
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));

        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.findByCartId(cartItem.getCart().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }
        List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItem cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cartRepository.save(cart);
        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
        {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(Price);
            orderItem.setQuantity(Quantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);


            Orders order = orderItem.getOrder();
            order.setTotalPrice(Price);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }


        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }


    @Transactional
    public ResponseEntity<?> updateCartItemQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartItemId());
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
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));

        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.findByCartId(cartItem.getCart().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }
        List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItem cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cartRepository.save(cart);
        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
        {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(Price);
            orderItem.setQuantity(Quantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);


            Orders order = orderItem.getOrder();
            order.setTotalPrice(Price);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }

    @Transactional
    public ResponseEntity<?> changeSizeCartItemQuantity(ChangeSizeItemReq req)
    {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartItemId());
        if(cartItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }
        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(),cartItem.getProductVariants().getProduct().getProId());
        if(productVariants == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ProductVariants Not Found with Size");
        }
        cartItem.setProductVariants(productVariants);
        double Present_TotalPrice = productVariants.getPrice() * cartItem.getQuantity();
        cartItem.setTotalPrice(Present_TotalPrice);
        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.findByCartId(cartItem.getCart().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItem cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cartRepository.save(cart);
        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
        {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(Price);
            orderItem.setQuantity(Quantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);


            Orders order = orderItem.getOrder();
            order.setTotalPrice(Price);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ChangeSizeItemResponse(
                 req.getSize(),
                 Quantity,
                 Present_TotalPrice
                )
        );
    }


    @Transactional
    public ResponseEntity<?> decreaseCartItemQuantity(IncreaseDecreaseItemQuantityReq req)
    {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartItemId());
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
        System.out.println(productVariants.getStock());

        if((req.getQuantity() - 1 ) > productVariants.getStock())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is greater than stock");
        }
        cartItem.setQuantity((Present_Quantity));
        cartItem.setTotalPrice((Present_TotalPrice));

        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.findByCartId(cartItem.getCart().getCartId());
        if(cart == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }
        List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItem cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cartRepository.save(cart);
        cartRepository.save(cart);
        if(cart.getTotalProduct() == 0 && cart.getStatus() == Status_Cart.RESTORE)
        {
            cart.setTotalProduct(0);
            cart.setTotalPrice(0);
            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);
        }

        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
        {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(Price);
            orderItem.setQuantity(Quantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);


            Orders order = orderItem.getOrder();
            order.setTotalPrice(Price);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new IncreaseDecreaseItemQuantityResponse(
                Present_Quantity,
                Present_TotalPrice
        ));
    }

    @Transactional
    public ResponseEntity<?> deleteOneItem(DeleteOneCartItemReq req)
    {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(req.getCartItemId());
        if(cartItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found");
        }
        cartItemRepository.delete(cartItem);
        List<CartItem> cartItemList = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cartItem.getCart().getCartId());
        Double Price = 0.0;
        Integer Quantity=0;
        for(CartItem cartItem2: cartItemList)
        {
            Price = Price + Double.valueOf(cartItem2.getTotalPrice());
            Quantity = Quantity + cartItem2.getQuantity();
        }
        Cart cart = cartRepository.findByCartId(cartItem.getCart().getCartId());
        cart.setTotalProduct(Quantity);
        cart.setTotalPrice(Price);
        cartRepository.save(cart);
        if(cart.getTotalProduct() == 0 && cart.getStatus() == Status_Cart.RESTORE)
        {
            cart.setTotalProduct(0);
            cart.setTotalPrice(0);
            cart.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cart);
        }
        if(cart.getStatus() == Status_Cart.COMPLETED_PAUSE)
        {
            OrderItem orderItem = cart.getOrderItem();
            orderItem.setTotalPrice(Price);
            orderItem.setQuantity(Quantity);
            orderItem.setCart(cart);
            orderItem.setDateUpdated(LocalDateTime.now());
            orderItemRepository.save(orderItem);


            Orders order = orderItem.getOrder();
            order.setTotalPrice(Price);
            order.setDateUpdated(LocalDateTime.now());
            orderRepository.save(order);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse(
                "Delete item success"
        ));
    }


    @Transactional
    public ResponseEntity<?> deleteAllCartItem(DeleteAllCartItemReq req) {

        Cart cartPause = cartRepository.findByCartIdAndStatus(req.getCartId(),Status_Cart.COMPLETED_PAUSE);
        if(cartPause != null)
        {
            if (cartPause.getStatus() == Status_Cart.COMPLETED_PAUSE) {
                OrderItem orderItem = cartPause.getOrderItem();
                if (orderItem != null) {
                    orderItem.setTotalPrice(0.0);
                    orderItem.setQuantity(0);
                    orderItem.setCart(cartPause);
                    orderItem.setDateUpdated(LocalDateTime.now());
                    orderItemRepository.save(orderItem);

                    Orders order = orderItem.getOrder();
                    if (order != null) {
                        order.setTotalPrice(0.0);
                        order.setDateUpdated(LocalDateTime.now());
                        orderRepository.save(order);
                    }
                }
            }
        }

        // Bước 1: tìm cart có status RESTORE
        Cart cartRestore = cartRepository.findByCartIdAndStatus(req.getCartId(), Status_Cart.RESTORE);
        if (cartRestore != null) {
            // Xóa hết cartItem nhanh
            cartItemRepository.deleteAllByCartId(cartRestore.getCartId());

            // Reset lại cart
            cartRestore.setTotalProduct(0);
            cartRestore.setTotalPrice(0);
            cartRestore.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cartRestore);

            return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse("Delete all item success"));
        }

        Cart cartAI= cartRepository.findByCartIdAndStatus(req.getCartId(), Status_Cart.CART_AI);
        if (cartAI != null) {

            cartItemRepository.deleteAllByCartId(cartAI.getCartId());

            // Reset lại cart
            cartAI.setTotalProduct(0);
            cartAI.setTotalPrice(0);
            cartAI.setStatus(Status_Cart.COMPLETED);
            cartRepository.save(cartAI);

            return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse("Delete all item success"));
        }

        // Bước 2: tìm cart với status NEW
        Cart cart = cartRepository.findByCartIdAndStatus(req.getCartId(), Status_Cart.NEW);
        if (cart == null || (cart.getStatus() != Status_Cart.NEW && cart.getStatus() != Status_Cart.CART_AI && cart.getStatus() != Status_Cart.COMPLETED_PAUSE)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart Not Found");
        }

        // Xóa hết cartItem nhanh
        cartItemRepository.deleteAllByCartId(cart.getCartId());

        // Reset lại cart
        cart.setTotalProduct(0);
        cart.setTotalPrice(0);
        cartRepository.save(cart);

        // Nếu cart status là COMPLETED_PAUSE thì reset luôn OrderItem và Order
        if (cart.getStatus() == Status_Cart.COMPLETED_PAUSE) {
            OrderItem orderItem = cart.getOrderItem();
            if (orderItem != null) {
                orderItem.setTotalPrice(0.0);
                orderItem.setQuantity(0);
                orderItem.setCart(cart);
                orderItem.setDateUpdated(LocalDateTime.now());
                orderItemRepository.save(orderItem);

                Orders order = orderItem.getOrder();
                if (order != null) {
                    order.setTotalPrice(0.0);
                    order.setDateUpdated(LocalDateTime.now());
                    orderRepository.save(order);
                }
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(new DeleteCartItemResponse("Delete all item success"));
    }




}
