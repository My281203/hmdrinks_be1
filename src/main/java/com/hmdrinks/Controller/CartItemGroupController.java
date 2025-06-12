package com.hmdrinks.Controller;

import com.hmdrinks.Request.ChangeSizeItemReq;
import com.hmdrinks.Request.DeleteOneCartItemReq;
import com.hmdrinks.Request.IncreaseDecreaseItemQuantityReq;
import com.hmdrinks.Request.InsertItemToCart;
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
@RequestMapping("/api/cart-item/group-order")
@RequiredArgsConstructor
public class CartItemGroupController {
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private CartItemGroupService cartItemGroupService;

    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/insert")
    public ResponseEntity<?> createCartItem(@RequestBody @Valid InsertItemToCart req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.insertCartItem(req);
    }

    @PutMapping(value = "/increase")
    public ResponseEntity<?> increaseItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.increaseCartItemGroupQuantity(req);
    }

    @PutMapping(value = "/decrease")
    public ResponseEntity<?> decreaseItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  cartItemGroupService.decreaseCartItemQuantity(req);
    }

    @PutMapping(value = "/change-size")
    public ResponseEntity<?> changeSizeCartItem(@RequestBody @Valid ChangeSizeItemReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.changeSizeCartItemQuantity(req);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateItemQuantityResponseResponseEntity(@RequestBody @Valid IncreaseDecreaseItemQuantityReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.updateCartItemQuantity(req);
    }

    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<?> deleteOneItem(@RequestBody @Valid DeleteOneCartItemReq req, HttpServletRequest httpRequest){

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.deleteOneItem(req);
    }
}