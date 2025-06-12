package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.CreateNewFavourite;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Request.DeleteAllFavouriteItemReq;
import com.hmdrinks.Response.*;
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
@RequestMapping("/api/fav")
@RequiredArgsConstructor
public class FavouriteController {
    @Autowired
    private FavouriteService favouriteService;
    @Autowired
    private FavouriteItemService favouriteItemService;
    @Autowired
    private SupportFunction supportFunction;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createFavourite(@RequestBody @Valid  CreateNewFavourite req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(favouriteService.createFavourite(req));
    }

    @GetMapping(value = "/list-favItem/{id}")
    public ResponseEntity<?> listAllFavouriteItem(
            @PathVariable Integer id,
            @RequestParam Language language


            ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return favouriteService.getAllItemFavourite(id,language);
    }

    @DeleteMapping(value = "/delete-allItem/{id}")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid  DeleteAllFavouriteItemReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return favouriteItemService.deleteAllFavouriteItem(req);
    }

    @GetMapping(value = "/list-fav/{userId}")
    public ResponseEntity<?> getFavoriteByUserId(
            @PathVariable Integer userId,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return favouriteService.getFavoriteById(userId);
    }
}