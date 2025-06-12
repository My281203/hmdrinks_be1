package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Status_Shipment;
import com.hmdrinks.Request.AllocationShipmentReq;
import com.hmdrinks.Request.CRUDShipmentReq;
import com.hmdrinks.Request.UpdateNoteShipment;
import com.hmdrinks.Request.UpdateTimeShipmentReq;
import com.hmdrinks.Service.ShipmentGroupService;
import com.hmdrinks.Service.ShipmentService;
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
@RequestMapping("/api/shipment-group")
@RequiredArgsConstructor
public class ShipmentGroupController {
    @Autowired
    private ShipmentGroupService shipmentService;
    @Autowired
    private SupportFunction supportFunction;


    @GetMapping("/view/group-order/{groupOrderId}")
    public ResponseEntity<?> getOneShipmentByGroupOrderId(@PathVariable int groupOrderId,@RequestParam Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("groupOrderId", groupOrderId);
        if (validation != null) return validation;
        return shipmentService.getInfoShipmentByGroupOrderId(groupOrderId, language);
    }

    @PostMapping("/activate/shipping")
    public ResponseEntity<?> activeShip(@RequestBody @Valid  AllocationShipmentReq req,HttpServletRequest request) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(request, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return shipmentService.activateShipment(req.getShipmentId(),req.getUserId());
    }

    @PostMapping("/activate/receiving")
    public ResponseEntity<?> activeReceiving(@RequestBody @Valid  AllocationShipmentReq req,HttpServletRequest request) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(request, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return shipmentService.ActivateReceiving(req.getShipmentId(),req.getUserId());
    }

    @PostMapping("/activate/success")
    public ResponseEntity<?> activeSuccess(@RequestBody @Valid  AllocationShipmentReq req,HttpServletRequest httpRequest) {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return shipmentService.successShipment(req.getShipmentId(),req.getUserId());
    }

    @PostMapping("/activate/cancel")
    public ResponseEntity<?> activeCancel(@RequestBody @Valid  AllocationShipmentReq req) {
        return shipmentService.cancelShipment(req.getShipmentId());
    }

    @GetMapping("/shipper/listShippment")
    public ResponseEntity<?> getListShipmentStatusByShipper(@RequestParam(name = "page") String page,
                                                            @RequestParam(name = "limit") String limit,
                                                            @RequestParam(name = "userId") Integer userId,
                                                            @RequestParam(name = "status")Status_Shipment statusShipment, HttpServletRequest httpRequest)
    {
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
        return  shipmentService.getListShipmentStatusByShipper(page,limit,userId,statusShipment);
    }

    @GetMapping("/shipper/listShippments")
    public ResponseEntity<?> getListShipmentsByShipper(@RequestParam(name = "page") String page,
                                                            @RequestParam(name = "limit") String limit,
                                                            @RequestParam(name = "userId") Integer userId,HttpServletRequest httpRequest)

    {
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
        return  shipmentService.getListAllShipmentByShipper(page,limit,userId);
    }


    @GetMapping("/check-time")
    public ResponseEntity<?> getListShipment()
    {
        return shipmentService.checkTimeDelivery();
    }

    @PostMapping("/update-note")
    public ResponseEntity<?> updateNote(@RequestBody @Valid  UpdateNoteShipment req) {
        return shipmentService.updateNote(req.getShipmentId(), req.getNote());
    }

    @GetMapping("/view/list-All")
    public ResponseEntity<?> getListShipment(@RequestParam(name = "page") String page,
                                             @RequestParam(name = "limit") String limit
                                                           )
    {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  shipmentService.getListAllShipment(page,limit);
    }

    @GetMapping("/view/listByStatus")
    public ResponseEntity<?> getListShipmentByStatus(@RequestParam(name = "page") String page,
                                                     @RequestParam(name = "limit") String limit,
                                                     @RequestParam(name = "status")Status_Shipment statusShipment
    )
    {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  shipmentService.getListAllShipmentByStatus(page,limit,statusShipment);
    }

    @GetMapping("/view/list-waiting/{userId}")
    public ResponseEntity<?> getListShipmentByUserId(@PathVariable int userId,HttpServletRequest httpRequest
    )
    {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorizationUpgrade(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  shipmentService.getListShipmentStatusWaitingByUserId(userId);
    }

    @PutMapping("/update-time")
    public ResponseEntity<?> updateTimeShipment(@RequestBody @Valid  UpdateTimeShipmentReq req)
    {
        return shipmentService.updateTimeShipment(req);
    }

    @GetMapping("/view/{shipmentId}")
    public ResponseEntity<?> getOneShipment(@PathVariable int shipmentId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("shipmentId", shipmentId);
        if (validation != null) return validation;
        return shipmentService.getOneShipment(shipmentId);
    }

//    @GetMapping("/view/order/{orderId}")
//    public ResponseEntity<?> getOneShipmentByOrderId(@PathVariable int orderId) {
//        return shipmentService.getInfoShipmentByOrderId(orderId);
//    }

    @GetMapping(value = "/search-shipment")
    public ResponseEntity<?> searchShipment(@RequestParam(name = "keyword") String keyword,
                                            @RequestParam(name = "page") String page,
                                            @RequestParam(name = "limit") String limit) {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return ResponseEntity.ok(shipmentService.searchShipment(keyword, page, limit));
    }

    @GetMapping("/view/map_direction/{shipmentId}")
    public ResponseEntity<?> getOneShipmentByOrderId1(@PathVariable int shipmentId) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("shipmentId", shipmentId);
        if (validation != null) return validation;
        return shipmentService.getMapdirection(shipmentId);
    }





}
