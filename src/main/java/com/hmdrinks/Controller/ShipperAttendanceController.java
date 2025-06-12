package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Status_Shipment;
import com.hmdrinks.Request.*;
import com.hmdrinks.Service.ShipmentService;
import com.hmdrinks.Service.ShipperAttendanceService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/shipper-attendance")
@RequiredArgsConstructor
public class ShipperAttendanceController {
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private ShipperAttendanceService shipperAttendanceService;
    @Autowired
    private SupportFunction supportFunction;

    @PostMapping("/checkin")
    public ResponseEntity<?> shipmentAllocation(@RequestBody @Valid  IdReq req) {
        return shipperAttendanceService.checkIn(req.getId());
    }

    @PostMapping("/activate-available")
    public ResponseEntity<?> shipmentAllocation1(@RequestBody @Valid  IdReq req) {
        return shipperAttendanceService.activeStatus(req.getId());
    }

    @PostMapping("/update-note")
    public ResponseEntity<?> activeShip(@RequestBody @Valid  UpdateNoteShipperAttendance req) {
        return shipperAttendanceService.updateNote(req.getNote(), req.getId(), req.getUserId());
    }

    @GetMapping("/attendance/full-month")
    public ResponseEntity<?> getFullMonthlyShipperAttendance(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  shipperAttendanceService.getFullMonthlyShipperAttendance(userId, month, year);
    }

    @GetMapping("/get-status")
    public ResponseEntity<?> getFullMonthlyShipperAttendance1(
            @RequestParam Integer userId
     ) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return shipperAttendanceService.getCurrentStatusShipper(userId);
    }

}