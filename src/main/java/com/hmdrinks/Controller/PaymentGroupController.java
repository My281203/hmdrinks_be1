package com.hmdrinks.Controller;

import com.hmdrinks.Entity.PaymentGroup;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
import com.hmdrinks.Repository.PaymentGroupRepository;
import com.hmdrinks.Repository.PaymentRepository;
import com.hmdrinks.Request.CreatePaymentGroupReq;
import com.hmdrinks.Request.CreatePaymentReq;
import com.hmdrinks.Request.CreatePaymentVNPayGroupReq;
import com.hmdrinks.Request.CreatePaymentVNPayReq;
import com.hmdrinks.Response.IpnResponse;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/payment-group")
@RequiredArgsConstructor
public class PaymentGroupController {

    @Autowired
    private PaymentGroupService paymentService;
    @Autowired
    private  PaymentService paymentService_og;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private VNPayIpnHandler vnPayIpnHandler;
    @Autowired
    private ZaloPayService zaloPayService;



    public static String getIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            String remoteAddr = request.getRemoteAddr();
            if (remoteAddr == null) {
                remoteAddr = "127.0.0.1";
            }
            return remoteAddr;
        }
        return xForwardedForHeader.split(",")[0].trim();
    }

    @PostMapping("/create/credit/vnPay")
    public ResponseEntity<?> createPayment(@RequestBody @Valid  CreatePaymentVNPayGroupReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getLeaderUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        var ipAddress = getIpAddress(httpRequest);
        req.setIpAddress(ipAddress);
        return paymentService.createVNPay(req);
    }

    @PostMapping("/create/credit/zaloPay")
    public ResponseEntity<?> createPaymentZaloPay(@RequestBody @Valid  CreatePaymentGroupReq req, HttpServletRequest httpRequest) throws Exception {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getLeaderUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return paymentService.createZaloPay(req);
    }

    @GetMapping("/vnpay_ipn")
    IpnResponse processIpn(@RequestParam Map<String, String> params) {
        var txnRef = params.get(VNPayParams.TXN_REF);
        PaymentGroup payment = paymentGroupRepository.findByOrderIdPayment(txnRef);
        if(payment != null)
        {
            return vnPayIpnHandler.processGroup(params);
        }
        else {
            return  vnPayIpnHandler.process(params);
        }

    }

    @PostMapping("/create/credit/momo")
    public ResponseEntity<?> createPaymentMomoPay(@RequestBody @Valid  CreatePaymentGroupReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getLeaderUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return paymentService.createPaymentMomo(req.getGroupOrderId(),req.getType(),req.getLeaderUserId(),req.getLanguage());
    }

    @PostMapping("/create/credit/payOs")
    public ResponseEntity<?> createPaymentPayOs(@RequestBody @Valid CreatePaymentGroupReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getLeaderUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return paymentService.createPaymentATM(req.getGroupOrderId(),req.getType(),req.getLeaderUserId(),req.getLanguage());
    }

    @PostMapping("/create/cash")
    public ResponseEntity<?> createPaymentCash(@RequestBody @Valid  CreatePaymentGroupReq req, HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getLeaderUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return paymentService.createPaymentCash(req.getGroupOrderId(), req.getLeaderUserId(),req.getLanguage());
    }

    @GetMapping("/zalo/callback")
    public ResponseEntity<?> handleCallbackZalo(
            @RequestParam String app_trans_id)
    {
        return  zaloPayService.handleCallBackGroup(app_trans_id);
    }

    @GetMapping("/payOS/callback")
    public ResponseEntity<?> handleCallbackPayOS(
            @RequestParam int orderCode) throws Exception {

        return  paymentService.handleCallBackPayOS(orderCode);
    }

    @GetMapping("/momo/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam String orderId,
            @RequestParam String resultCode)
    {


        return  paymentService.callBack(resultCode,orderId);
    }

    @GetMapping("/check-status-payment")
    public ResponseEntity<?> checkStatusPayment(
            @RequestParam int paymentId
    )
    {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("paymentId", paymentId);
        if (validation != null) return validation;
        return paymentService.checkStatusPayment(paymentId);
    }

//    @GetMapping("/listAll")
//    public ResponseEntity<?> listAll(@RequestParam(name = "page") String page,
//                                     @RequestParam(name = "limit") String limit) {
//        return paymentService.getAllPayment(page, limit);
//    }
//
//    @GetMapping("/listAll-method")
//    public ResponseEntity<?> listAllMethod(@RequestParam(name = "page") String page,
//                                           @RequestParam(name = "limit") String limit,
//                                           @RequestParam(name = "method") Payment_Method method)
//
//    {
//        return paymentService.getAllPaymentMethod(page, limit,method);
//    }

    @GetMapping("/info/payOs/{paymentId}")
    public ResponseEntity<?> getInformationPayOS(@PathVariable int paymentId) throws Exception {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("paymentId", paymentId);
        if (validation != null) return validation;
        return paymentService.getInformationPayOs(paymentId);
    }

//    @GetMapping("/listAll-status")
//    public ResponseEntity<?> listAllByStatus(@RequestParam(name = "page") String page,
//                                             @RequestParam(name = "limit") String limit,
//                                             @RequestParam(name = "status") Status_Payment status)
//
//    {
//        return paymentService.getAllPaymentStatus(page, limit, status);
//    }

    @GetMapping("/view/{paymentId}")
    public ResponseEntity<?> viewPayment(@PathVariable int paymentId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("paymentId", paymentId);
        if (validation != null) return validation;
        return paymentService.getOnePayment(paymentId);
    }

    @GetMapping("/check-time")
    public ResponseEntity<?> getListShipment()
    {
        return paymentService.checkTimePaymentGroup();
    }


}