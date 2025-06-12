package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/cart/group-order")
@RequiredArgsConstructor
public class CartGroupController {
    @Autowired
    private CartGroupService cartGroupService;
    @Autowired
    private CartItemGroupService cartItemGroupService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private JwtService jwtService;



    @GetMapping(value = "/list-cartItem/{cartId}")
    public ResponseEntity<?> listAllCartItem(
            @PathVariable Integer cartId,
            @RequestParam Language language

            ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("cartId", cartId);
        if (validation != null) return validation;
        return cartGroupService.getAllItemCart(cartId,language);
    }

    @DeleteMapping(value = "/delete-allItem/{id}")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid DeleteAllCartItemReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.deleteAllCartItem(req);
    }

    @GetMapping("/list-cart/{userId}")
    public  ResponseEntity<?> getAllCartUser(@PathVariable Integer userId,@RequestParam Integer memberId){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return cartGroupService.getAllCartGroupFromUserAndMemberId(userId,memberId);
    }
}