package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Request.*;
import com.hmdrinks.Service.CartItemService;
import com.hmdrinks.Service.CartService;
import com.hmdrinks.Service.GroupOrderService;
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

import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/group-order")
@RequiredArgsConstructor
public class GroupOrderController {
    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private GroupOrderService groupOrderService;
    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createCart(@RequestBody @Valid  CreateNewGroupReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        };
        return groupOrderService.createGroup(req);
    }

    @PostMapping(value = "/join-group")
    public ResponseEntity<?> joinGroup(@RequestBody @Valid  JoinGroupReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        };
        return groupOrderService.joinGroup(req);
    }

    @GetMapping(value = "/detail-group/{groupId}")
    public ResponseEntity<?> listAllCartItem(
            @PathVariable Integer groupId,
            @RequestParam Language language,
            HttpServletRequest httpRequest
            ) {

        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        String authHeader = httpRequest.getHeader("Authorization");

        String jwt = authHeader.substring(7);

        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        return groupOrderService.getDetailOneGroup(groupId,language,Integer.parseInt(userIdFromTokenStr));
    }



    // Xóa nhóm do trưởng nhóm thực hiện
    @DeleteMapping(value = "/delete/{groupOrderId}/{leaderUserId}")
    public ResponseEntity<?> deleteGroup(@PathVariable("groupOrderId") int groupOrderId,
                                         @PathVariable("leaderUserId") int leaderUserId,
                                         HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, leaderUserId);
        ResponseEntity<?> validation = supportFunction.validatePositiveId("leaderUserId", leaderUserId);
        if (validation != null) return validation;
        ResponseEntity<?> validation1 = supportFunction.validatePositiveId("groupOrderId", groupOrderId);
        if (validation1 != null) return validation1;
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.deleteGroupByLeader(groupOrderId, leaderUserId);
    }

    // Trưởng nhóm xóa thành viên
    @DeleteMapping(value = "/delete-member/{groupOrderId}/{leaderUserId}/{memberUserId}")
    public ResponseEntity<?> deleteMember(@PathVariable("groupOrderId") int groupOrderId,
                                          @PathVariable("leaderUserId") int leaderUserId,
                                          @PathVariable("memberUserId") int memberUserId,
                                          HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, leaderUserId);

        ResponseEntity<?> validation = supportFunction.validatePositiveId("leaderUserId", leaderUserId);
        if (validation != null) return validation;
        ResponseEntity<?> validation2 = supportFunction.validatePositiveId("memberUserId", memberUserId);
        if (validation2 != null) return validation2;
        ResponseEntity<?> validation1 = supportFunction.validatePositiveId("groupOrderId", groupOrderId);
        if (validation1 != null) return validation1;
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.deleteMemberByLeader(groupOrderId, leaderUserId, memberUserId);
    }

    // Thành viên rời khỏi nhóm
    @PutMapping(value = "/leave/{groupOrderId}/{userId}")
    public ResponseEntity<?> leaveGroup(@PathVariable("groupOrderId") int groupOrderId,
                                        @PathVariable("userId") int userId,
                                        HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, userId);

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        ResponseEntity<?> validation1 = supportFunction.validatePositiveId("groupOrderId", groupOrderId);
        if (validation1 != null) return validation1;
        return groupOrderService.leaveGroup(groupOrderId, userId);
    }

    @GetMapping(value = "/get-id-group/{userId}")
    public ResponseEntity<?> getIdGroup(@PathVariable int userId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return groupOrderService.getIdGroup(userId);
    }

    @GetMapping(value = "/get-id-cart-group/{userId}")
    public ResponseEntity<?> getIdCartGroup(@PathVariable int userId,@RequestParam int groupOrderId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return groupOrderService.getIdCartGroup(userId,groupOrderId);
    }

    @GetMapping(value = "/get-group-activate/{userId}")
    public ResponseEntity<?> getIdCartGroup(@PathVariable int userId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return groupOrderService.getActiveGroupIds(userId);
    }

    @PutMapping("/change-type-group")
    public ResponseEntity<?> changeTypeGroup(@RequestBody @Valid  ChangeTypeGroupReq request,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.changeTypeGroup(
                request.getTypeGroupOrder(),
                request.getGroupId(),
                request.getLeaderId()
        );
    }

    @PutMapping("/update-type-payment-main")
    public ResponseEntity<?> updateTypePaymentGroupMain(@RequestBody @Valid  UpdateTypePaymentGroupReq request,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.updateTypePaymentGroupMain(
                request.getTypePayment(),
                request.getGroupId(),
                request.getLeaderId()
        );
    }

    @PutMapping("/update-type-payment-member")
    public ResponseEntity<?> updateTypePaymentGroupMember(@RequestBody @Valid  UpdateTypePaymentGroupReq request,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.updateTypePaymentGroupMember(
                request.getTypePayment(),
                request.getGroupId(),
                request.getLeaderId()
        );
    }

    @PutMapping("/update-address")
    public ResponseEntity<?> updateAddress(@RequestBody @Valid  UpdateAddressGroupReq request,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.updateAddress(
                request.getAddress(),
                request.getGroupId(),
                request.getLeaderId()
        );

    }

    @PutMapping("/update-name")
    public ResponseEntity<?> updateName(@RequestBody @Valid  UpdateNameGroupReq request,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.updateName(request.getNameGroup(), request.getGroupId(), request.getLeaderId());

    }

    @GetMapping("/preview")
    public ResponseEntity<?> previewPayment(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("language") Language language,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);
        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        return groupOrderService.previewPayment(groupId, language,Integer.parseInt(userIdFromTokenStr));
    }

    @GetMapping("/get-link-group")
    public ResponseEntity<?> getLink(
            @RequestParam("groupId") Integer groupId,

            HttpServletRequest httpRequest
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);

        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        return groupOrderService.getLinkGroup(groupId,Integer.parseInt(userIdFromTokenStr));
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("leaderId") Integer leaderId,
            @RequestParam("language") Language language,
            HttpServletRequest httpRequest
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, leaderId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.confirmPayment(groupId, leaderId,language);
    }

    @GetMapping("/{groupId}/time-remaining")
    public ResponseEntity<?> getGroupRemainingTime(@PathVariable Integer groupId,HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);
        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        return  groupOrderService.getGroupRemainingTime(groupId,Integer.parseInt(userIdFromTokenStr));
    }

    @GetMapping("/fetch-all/{groupId}")
    public ResponseEntity<?> fetchAllInformation(@PathVariable Integer groupId,@RequestParam Language language,HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);

        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        return  groupOrderService.fetchAllInformationGroupOrder(groupId,language,Integer.parseInt(userIdFromTokenStr));
    }

    @GetMapping("/link-payment/{groupId}")
    public ResponseEntity<?> getLinkPayment(@PathVariable Integer groupId,HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);
        String userIdFromTokenStr = jwtService.extractUserId(jwt);

        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupId", groupId);
        if (validation != null) return validation;
        return groupOrderService.getLinkPayment(groupId,Integer.parseInt(userIdFromTokenStr));
    }

    @GetMapping("/check-time")
    public ResponseEntity<?> checkTimeOrders()
    {
        return groupOrderService.checkTimeOrders();
    }

    @GetMapping("/list-success/{userId}")
    public ResponseEntity<?> listCompletedGroupOrders(
            @RequestParam int page,
            @RequestParam int size,
            @PathVariable int userId
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        ResponseEntity<?> check = supportFunction.validatePositiveIntegers(
                Map.of("page", page, "limit", size)
        );
        if (check != null) return check;
        return groupOrderService.listHistoryGroupOrderCompleted(page, size, userId);
    }

    @GetMapping("/list-cancelled/{userId}")
    public ResponseEntity<?> listCompletedGroupOrders1(
            @RequestParam int page,
            @RequestParam int size,
            @PathVariable int userId
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        ResponseEntity<?> check = supportFunction.validatePositiveIntegers(
                Map.of("page", page, "limit", size)
        );
        if (check != null) return check;
        return groupOrderService.listHistoryGroupOrderCancelled(page, size, userId);
    }

    @GetMapping("/list-refund/{userId}")
    public ResponseEntity<?> listRefundGroupOrdersByLeader(
            @RequestParam int page,
            @RequestParam int size,
            @PathVariable int userId
    ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        ResponseEntity<?> check = supportFunction.validatePositiveIntegers(
                Map.of("page", page, "limit", size)
        );
        if (check != null) return check;
        return groupOrderService.listAllRefundGroupOrderByLeader(page, size, userId);
    }

    @GetMapping("/list-refund/all")
    public ResponseEntity<?> listRefundGroupOrders(
            @RequestParam int page,
            @RequestParam int size
    ) {
        ResponseEntity<?> check = supportFunction.validatePositiveIntegers(
                Map.of("page", page, "limit", size)
        );
        if (check != null) return check;
        return groupOrderService.listAllRefundGroupOrder(page, size);
    }


    @PutMapping("/activate-blacklist")
    public ResponseEntity<?> activateBlackList(
            @RequestBody @Valid  CreateBlacklistGroupReq request,
            HttpServletRequest httpRequest) {

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.activateBlackList(request.getGroupOrderId(), request.getLeaderId(), request.getUserId());
    }


    @PutMapping("/restore-blacklist")
    public ResponseEntity<?> restoreBlackList(
            @RequestBody @Valid  CreateBlacklistGroupReq request,
            HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, request.getLeaderId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return groupOrderService.restoreBlackList(request.getGroupOrderId(), request.getLeaderId(), request.getUserId());
    }


    @GetMapping("/blacklist")
    public ResponseEntity<?> getListAllBlackListFormGroupOrderId(
            @RequestParam Integer groupOrderId,
            @RequestParam Language language,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        String jwt = authHeader.substring(7);

        String userIdFromTokenStr = jwtService.extractUserId(jwt);
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupOrderId", groupOrderId);
        if (validation != null) return validation;
        return groupOrderService.getListAllBlackListFormGroupOrderId(groupOrderId, language,Integer.parseInt(userIdFromTokenStr));
    }



}
