package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.CreateProductReq;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Request.IdReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.CartItemService;
import com.hmdrinks.Service.CartService;
import com.hmdrinks.Service.JwtService;
import com.hmdrinks.Service.ProductService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createCart(@RequestBody @Valid CreateNewCart req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        };
        return cartService.createCart(req);
    }

    @PostMapping(value = "/createAI")
    public ResponseEntity<?> createCartAI(@RequestBody @Valid CreateNewCart req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        };
        return cartService.createCartAI(req);
    }

    @GetMapping(value = "/list-cartItem/{id}")
    public ResponseEntity<?> listAllCartItem(
            @PathVariable Integer id,
            @RequestParam Language language

            ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return cartService.getAllItemCart(id,language);
    }

    @DeleteMapping(value = "/delete-allItem/{id}")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid DeleteAllCartItemReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemService.deleteAllCartItem(req);
    }

    @GetMapping("/list-cart/{userId}")
    public  ResponseEntity<?> getAllCartUser(@PathVariable Integer userId){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return cartService.getAllCartFromUser(userId);
    }
}