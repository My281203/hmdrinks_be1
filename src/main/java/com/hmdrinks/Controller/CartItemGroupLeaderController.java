package com.hmdrinks.Controller;

import com.hmdrinks.Request.*;
import com.hmdrinks.Service.CartItemGroupLeaderService;
import com.hmdrinks.Service.CartItemGroupService;
import com.hmdrinks.Service.CartItemService;
import com.hmdrinks.Service.JwtService;
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
@RequestMapping("/api/cart-item/group-order/leader")
@RequiredArgsConstructor
public class CartItemGroupLeaderController {
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private CartItemGroupLeaderService cartItemGroupService;

    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/insert")
    public ResponseEntity<?> createCartItem(@RequestBody @Valid InsertItemToCartLeader req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.insertCartItemLeader(req);
    }

    @PutMapping(value = "/increase")
    public ResponseEntity<?> increaseItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityLeaderReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.increaseCartItemGroupQuantityLeader(req);
    }

    @PutMapping(value = "/decrease")
    public ResponseEntity<?> decreaseItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityLeaderReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  cartItemGroupService.decreaseCartItemQuantity(req);
    }

    @PutMapping(value = "/change-size")
    public ResponseEntity<?> changeSizeCartItem(@RequestBody @Valid ChangeSizeItemLeaderReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.changeSizeCartItemQuantity(req);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityLeaderReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.updateCartItemQuantity(req);
    }

    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<?> deleteOneItem(@RequestBody @Valid DeleteOneCartItemLeaderReq req, HttpServletRequest httpRequest){

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.deleteOneItem(req);
    }
}