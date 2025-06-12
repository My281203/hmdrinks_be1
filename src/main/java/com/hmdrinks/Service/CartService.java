package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Response.CRUDCartItemResponse;
import com.hmdrinks.Response.CreateNewCartResponse;
import com.hmdrinks.Response.ListAllCartUserResponse;
import com.hmdrinks.Response.ListItemCartResponse;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private SupportFunction supportFunction;


    @Transactional
    public ResponseEntity<?> createCart(CreateNewCart req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null)
        {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

        Cart cart = cartRepository.findByUserUserIdAndStatus(user.getUserId(), Status_Cart.NEW);
        if(cart != null)
        {
            return  ResponseEntity.status(HttpStatus.CONFLICT).body("Cart already exists");
        }
        Cart cart1 = new Cart();
        cart1.setTotalPrice(0);
        cart1.setUser(user);
        cart1.setStatus(Status_Cart.NEW);
        cart1.setTotalProduct(0);
        cartRepository.save(cart1);
        return  ResponseEntity.status(HttpStatus.OK).body( new CreateNewCartResponse(
                cart1.getCartId(),
                cart1.getTotalPrice(),
                cart1.getTotalProduct(),
                cart1.getUser().getUserId(),
                cart1.getStatus()
        ));
    }


    @Transactional
    public ResponseEntity<?> createCartAI(CreateNewCart req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null)
        {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

//        Cart cart = cartRepository.findByUserUserIdAndStatus(user.getUserId(), Status_Cart.NEW);
//        if(cart != null)
//        {
//            return  ResponseEntity.status(HttpStatus.CONFLICT).body("Cart already exists");
//        }
        Cart cart1 = new Cart();
        cart1.setTotalPrice(0);
        cart1.setUser(user);
        cart1.setStatus(Status_Cart.CART_AI);
        cart1.setTotalProduct(0);
        cartRepository.save(cart1);
        return  ResponseEntity.status(HttpStatus.OK).body( new CreateNewCartResponse(
                cart1.getCartId(),
                cart1.getTotalPrice(),
                cart1.getTotalProduct(),
                cart1.getUser().getUserId(),
                cart1.getStatus()
        ));
    }


    // Hello

    public interface CartProjection {
        Integer getCartId();
        Double getTotalPrice();        // getTotalPrice, nên alias đúng
        Integer getTotalProduct();
        Integer getUserId();
        String getStatus();            // getStatus
    }


    public ResponseEntity<?> getAllCartFromUser(int userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

        List<CartProjection> cartProjections = cartRepository.findCartProjectionsByUserId(userId);

        List<CreateNewCartResponse> cartResponses = cartProjections.stream()
                .map(cart -> new CreateNewCartResponse(
                        cart.getCartId(),
                        cart.getTotalPrice(),
                        cart.getTotalProduct(),
                        cart.getUserId(),
                        cart.getStatus()
                ))
                .toList();

        return ResponseEntity.ok(new ListAllCartUserResponse(userId, cartResponses.size(), cartResponses));
    }


    public interface CartItemProjection {
        Integer getCartItemId();
        Integer getProId();
        String getProName();
        Size getSize();
        Double getTotalPrice();
        Integer getQuantity();
        String getListProImg();
    }




    @Transactional
    public ResponseEntity<?> getAllItemCart(int id, Language language) {
        Cart cart = cartRepository.findByCartId(id);
        if (cart == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found cart");
        }

        List<CartItemProjection> cartItems =
                cartItemRepository.findCartItemsWithTranslation(id, language);

        // Duyệt và map
        List<CRUDCartItemResponse> crudCartItemResponses = cartItems.stream()
                .map(item -> new CRUDCartItemResponse(
                        item.getCartItemId(),
                        item.getProId(),
                        item.getProName(),
                        id,
                        item.getSize(),
                        item.getTotalPrice(),
                        item.getQuantity(),
                        item.getListProImg()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ListItemCartResponse(id, cartItems.size(), crudCartItemResponses));
    }



//    @Transactional
//    public ResponseEntity<?> getAllItemCart(int id, Language language) {
//        Cart cart = cartRepository.findByCartId(id);
//        if (cart == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found cart");
//        }
//
//        List<CartItemProjection> cartItems = cartItemRepository.findCartItemProjectionByCartId(id);
//        List<CRUDCartItemResponse> crudCartItemResponses = new ArrayList<>();
//
//        // Load Product Translations for English
//        Map<Integer, String> productNameTranslations = new HashMap<>();
//        if (language == Language.EN) {
//            List<Integer> productIds = cartItems.stream()
//                    .map(CartItemProjection::getProId)
//                    .collect(Collectors.toList());
//
//            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(new HashSet<>(productIds));
//            translations.forEach(t -> productNameTranslations.put(t.getProduct().getProId(), t.getProName()));
//        }
//
//        // Process Cart Items
//        cartItems.forEach(cartItem -> {
//            String productName = language == Language.EN
//                    ? productNameTranslations.getOrDefault(cartItem.getProId(),
//                    supportFunction.convertLanguage(cartItem.getProName(), Language.EN))
//                    : cartItem.getProName();
//
//            String firstImageUrl = extractFirstImageUrl(cartItem.getListProImg());
//
//            crudCartItemResponses.add(new CRUDCartItemResponse(
//                    cartItem.getCartItemId(),
//                    cartItem.getProId(),
//                    productName,
//                    id,
//                    Size.valueOf(cartItem.getSize()),
//                    cartItem.getTotalPrice(),
//                    cartItem.getQuantity(),
//                    firstImageUrl
//            ));
//        });
//
//        return ResponseEntity.status(HttpStatus.OK).body(new ListItemCartResponse(
//                id,
//                cartItems.size(),
//                crudCartItemResponses
//        ));
//    }

    // Utility Method to Extract First Image URL
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
}
