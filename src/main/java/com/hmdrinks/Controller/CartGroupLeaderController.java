package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Request.DeleteAllCartItemLeaderReq;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Service.CartGroupService;
import com.hmdrinks.Service.CartItemGroupLeaderService;
import com.hmdrinks.Service.CartItemGroupService;
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
@RequestMapping("/api/cart/group-order/leader")
@RequiredArgsConstructor
public class CartGroupLeaderController {
    @Autowired
    private CartGroupService cartGroupService;
    @Autowired
    private CartItemGroupLeaderService cartItemGroupService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private JwtService jwtService;





    @DeleteMapping(value = "/delete-allItem/{id}")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid DeleteAllCartItemLeaderReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserIdLeader());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return cartItemGroupService.deleteAllCartItem(req);
    }

}