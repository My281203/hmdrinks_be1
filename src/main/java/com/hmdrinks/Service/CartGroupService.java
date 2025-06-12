package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartGroupService {
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
    @Autowired
    private CartGroupRepository cartGroupRepository;
    @Autowired
    private  CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;


    // Hello

    public interface CartGroupProjection {
        Integer getCartId();
        Double getTotalPrice();        // getTotalPrice, nên alias đúng
        Integer getTotalProduct();
        Integer getUserId();
         // getStatus
    }


    public ResponseEntity<?> getAllCartGroupFromUserAndMemberId(int userId,int memberId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

        List<CartGroupProjection> cartProjections = cartGroupRepository.findCartProjectionsByUserIdAndGroupOrderMemberMemberId(userId,memberId);

        List<CreateNewCartResponse> cartResponses = cartProjections.stream()
                .map(cart -> new CreateNewCartResponse(
                        cart.getCartId(),
                        cart.getTotalPrice(),
                        cart.getTotalProduct(),
                        cart.getUserId()

                ))
                .toList();

        return ResponseEntity.ok(new ListAllCartUserResponse(userId, cartResponses.size(), cartResponses));
    }


    public interface CartItemGroupProjection {
        Integer getCartItemId();
        Integer getProId();
        String getProName();
        String getSize();
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

        List<CartItemGroupProjection> cartItems = cartItemGroupRepository.findCartItemProjectionByCartId(id);
        List<CRUDCartItemGroupResponse> crudCartItemResponses = new ArrayList<>();

        // Load Product Translations for English
        Map<Integer, String> productNameTranslations = new HashMap<>();
        if (language == Language.EN) {
            List<Integer> productIds = cartItems.stream()
                    .map(CartItemGroupProjection::getProId)
                    .collect(Collectors.toList());

            List<ProductTranslation> translations = productTranslationRepository.findAllByProduct_ProIdIn(new HashSet<>(productIds));
            translations.forEach(t -> productNameTranslations.put(t.getProduct().getProId(), t.getProName()));
        }

        // Process Cart Items
        cartItems.forEach(cartItemGroup -> {
            String productName = language == Language.EN
                    ? productNameTranslations.getOrDefault(cartItemGroup.getProId(),
                    supportFunction.convertLanguage(cartItemGroup.getProName(), Language.EN))
                    : cartItemGroup.getProName();

            String firstImageUrl = extractFirstImageUrl(cartItemGroup.getListProImg());

            ProductVariants productVariants = productVariantsRepository.findByVarIdAndSize(cartItemGroup.getProId(),Size.valueOf(cartItemGroup.getSize()));
            crudCartItemResponses.add(new CRUDCartItemGroupResponse(
                    cartItemGroup.getCartItemId(),
                    cartItemGroup.getProId(),
                    productName,
                    id,
                    Size.valueOf(cartItemGroup.getSize()),
                    productVariants.getPrice(),
                    cartItemGroup.getTotalPrice(),
                    cartItemGroup.getQuantity(),
                    firstImageUrl
            ));
        });

        return ResponseEntity.status(HttpStatus.OK).body(new ListItemCartGroupResponse(
                id,
                cartItems.size(),
                crudCartItemResponses
        ));
    }

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
