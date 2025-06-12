package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Status_Order;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.CancelReasonReq;
import com.hmdrinks.Service.GenerateInvoiceService;
import com.hmdrinks.Service.OrdersService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/orders")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private GenerateInvoiceService generateInvoiceService;


    @PostMapping(value = "/create")
    public ResponseEntity<?> createVoucher(@RequestBody @Valid  CreateOrdersReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(ordersService.addOrder(req));
    }

    @PostMapping(value = "/pause_order")
    public ResponseEntity<?> pauseOrder(@RequestBody @Valid  AddItemOrderConfirmRequest req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(ordersService.restoreAddItemOrder(req));
    }

    @PostMapping(value = "/restore")
    public ResponseEntity<?> restoreOrder(@RequestBody @Valid  ConfirmCancelOrderReq  req,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ordersService.restoreOrder(req.getOrderId());
    }

    @PostMapping(value = "/confirm")
    public ResponseEntity<?> createVoucher1(@RequestBody @Valid  ConfirmCancelOrderReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(ordersService.confirmOrder(req.getOrderId()));
    }

    @PostMapping(value = "/confirm_order_pause")
    public ResponseEntity<?> confirm_order_pause(@RequestBody @Valid  ConfirmCancelOrderReq req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(ordersService.confirmTemporarilyPauseOrder(req.getOrderId()));
    }

    @PostMapping(value = "/confirm-cancel")
    public ResponseEntity<?> cancelOrder(@RequestBody @Valid  ConfirmCancelOrderReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(ordersService.confirmCancelOrder(req.getOrderId()));
    }

    @GetMapping("/info-payment")
    public ResponseEntity<?> infoPayment(@RequestParam int orderId){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("orderId", orderId);
        if (validation != null) return validation;
        return  ordersService.getInformationPayment(orderId);
    }
    @GetMapping("/info-payment-language")
    public ResponseEntity<?> getInformationPaymentLanguage(@RequestParam int orderId, @RequestParam Language language){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("orderId", orderId);
        if (validation != null) return validation;
        return  ordersService.getInformationPaymentLanguage(orderId, language);
    }

    @GetMapping("/pdf/invoice")
    public ResponseEntity<?> infoPayment1(@RequestParam int orderId) throws IOException {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("orderId", orderId);
        if (validation != null) return validation;
        return  generateInvoiceService.createInvoice(orderId);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> historyOrder(@PathVariable int userId,@RequestParam Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.listHistoryOrder(userId,language);
    }

    @GetMapping("/view/confirmed/{userId}")
    public ResponseEntity<?> historyOrder1(@PathVariable int userId,@RequestParam Language language,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.listOrderConfirmed(userId,language);
    }

    @GetMapping("/view/order-cancel/payment-refund")
    public ResponseEntity<?> orderCancelAndPaymentRefund(@RequestParam Language language,HttpServletRequest httpRequest) {

        return  ordersService.listOrderCancelAndPaymentRefund(language);
    }

    @GetMapping("/view/order-cancel/payment-refund-user/{userId}")
    public ResponseEntity<?> orderCancelAndPaymentRefundUser(@PathVariable int userId,@RequestParam Language language,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.listOrderCancelAndPaymentRefundUser(userId,language);
    }

    @GetMapping("/view/order-cancel/payment-not/{userId}")
    public ResponseEntity<?> orderCancelAndPaymentNot(@PathVariable int userId,@RequestParam Language language,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.listOrderCancelnotPayment(userId,language);
    }

    @GetMapping("/view/order-cancel/payment-have/{userId}")
    public ResponseEntity<?> orderCancelAndPaymentHave(@PathVariable int userId,@RequestParam Language language,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.listOrderCancelHavetPayment(userId,language);
    }

    @GetMapping("/view/fetchOrdersAwaiting/{userId}")
    public ResponseEntity<?> fetchOrdersAwaitingPayment(@PathVariable int userId, @RequestParam Language language,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  ordersService.fetchOrdersAwaitingPayment(userId,language);
    }

    @GetMapping("/view/{userId}")
    public ResponseEntity<?> getAllPaymentByUserId(@RequestParam(name = "page") String page,
                                                   @RequestParam(name = "limit") String limit,
                                                   @PathVariable int userId,
                                                   @RequestParam(name = "language") Language language,HttpServletRequest httpRequest) throws IOException {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ordersService.getAllOrderByUserId(page,limit,userId,language);
    }

    @GetMapping("/view/{userId}/status")
    public ResponseEntity<?> getAllPaymentByUserIdAndStatus(@RequestParam(name = "page") String page,
                                                            @RequestParam(name = "limit") String limit,
                                                            @RequestParam(name = "status")Status_Order statusOrder,
                                                            @RequestParam(name = "language") Language language,
                                                            @PathVariable int userId,HttpServletRequest httpRequest) throws IOException {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ordersService.getAllOrderByUserIdAndStatus(page,limit,userId,statusOrder,language);
    }

    @PutMapping("/cancel-order")
    public ResponseEntity<?> cancelOrder(@RequestBody @Valid  CreatePaymentReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ordersService.cancelOrder(req.getOrderId(), req.getUserId());
    }

    @GetMapping("/detail-item/{orderId}")
    public ResponseEntity<?> detailItem(@PathVariable int orderId, @RequestParam Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("orderId", orderId);
        if (validation != null) return validation;
        return  ordersService.detailItemOrders(orderId,language);
    }

    @GetMapping("/detail/{orderId}")
    public ResponseEntity<?> detailOrder(@PathVariable int orderId,@RequestParam Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("orderId", orderId);
        if (validation != null) return validation;
        return  ordersService.getDetailOrder(orderId,language);
    }

    @PostMapping("/reason-cancel")
    public ResponseEntity<?> ReasonCancel(@RequestBody @Valid  CancelReasonReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  ordersService.CancelReason(req);
    }

    @GetMapping("/list-cancel-reason")
    public ResponseEntity<?> detailItem1( HttpServletRequest httpRequest) {
        return  ordersService.listAllCancelReasonAwait();
    }


    @GetMapping("/detail_order_pause")
    public ResponseEntity<?> detailItem1(@RequestParam Integer cartId,@RequestParam Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("cartId", cartId);
        if (validation != null) return validation;
        return  ordersService.getOrderFromCartPause(cartId,language);
    }

    @PostMapping("/reason-cancel/accept")
    public ResponseEntity<?> AcceptReasonCancel(@RequestBody @Valid  IdReq req) {
        return  ordersService.acceptCancelReason(req.getId());
    }

    @PostMapping("/reason-cancel/reject")
    public ResponseEntity<?> RejectReasonCancel(@RequestBody @Valid IdReq req) {
        return  ordersService.rejectCancelReason(req.getId());
    }

    @PostMapping("/order_pause/add_voucher")
    public ResponseEntity<?> RejectReasonCancel1(@RequestBody @Valid  AddVoucherPause req,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  ordersService.addVoucherPauseOrder(req.getOrderId(),req.getVoucherId());
    }

    @PostMapping("/order_pause/add_coin")
    public ResponseEntity<?> AddCoinPause(@RequestBody @Valid  AddCoinPause req,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  ordersService.addUserCoinOrderPause(req.getOrderId(),req.getPointCoinUse());
    }

    @GetMapping("/check-time")
    public ResponseEntity<?> checkTimeOrders()
    {
        return ordersService.checkTimeOrders();
    }

    @GetMapping("/{orderId}/time-remain")
    public ResponseEntity<?> checkTimeOrderRemain(@PathVariable Integer orderId) {
        return ordersService.checkTimeOrderRemain(orderId);
    }




}
