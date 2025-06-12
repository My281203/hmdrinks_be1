package com.hmdrinks.Controller;
import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Type_Post;
import com.hmdrinks.Request.*;
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
@RequestMapping("/api/absence-request")
@RequiredArgsConstructor
public class AbsenceRequestController {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private  ShipmentService shipmentService;
    @Autowired
    private UserVoucherService userVoucherService;
    @Autowired
    private  AbsenceRequestService absenceRequestService;
    @Autowired
    private  PaymentService paymentService;
    @Autowired
    private SupportFunction supportFunction;




    @PostMapping(value = "/create-absence")
    public ResponseEntity<?> createAbsence(@RequestBody @Valid  CreateNewAbsence req,HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return absenceRequestService.createRequestAbsence(req);
    }


    @GetMapping(value ="/view/{requestId}")
    public ResponseEntity<?> getOneAbsence(
            @PathVariable Integer requestId,
            @RequestParam Integer userId

    ){
        return absenceRequestService.getOneAbsence(requestId,userId);
    }



    @GetMapping(value = "/view/all/{userId}")
    public ResponseEntity<?> getAllAbsence(@PathVariable Integer userId,
                                           @RequestParam(name = "page") String page,
                                           @RequestParam(name = "limit") String limit){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;  // return luôn nếu có lỗi
        }
        return absenceRequestService.getListAbsenceByUserId(userId,page,limit);
    }

    @GetMapping(value = "/view/status/{userId}")
    public ResponseEntity<?> getAllAbsence(@PathVariable Integer userId,
                                           @RequestParam(name = "status")LeaveStatus status,
                                           @RequestParam(name = "page") String page,
                                           @RequestParam(name = "limit") String limit){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return absenceRequestService.getListAbsenceByUserIdAndStatus(userId,page,limit,status);
    }


    @GetMapping(value = "/view/all/listByStatus")
    public ResponseEntity<?> getAllAbsenceByStatus(@RequestParam(name = "status")LeaveStatus status,
                                                   @RequestParam(name = "page") String page,
                                                   @RequestParam(name = "limit") String limit){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return absenceRequestService.getListAbsenceByStatus(status,page,limit);
    }

    @GetMapping(value = "/view/all")
    public ResponseEntity<?> getAllAbsence(
            @RequestParam(name = "page") String page,
            @RequestParam(name = "limit") String limit){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;  // return luôn nếu có lỗi
        }
        return absenceRequestService.getListAbsence(page,limit);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateAbsence(@RequestBody @Valid  UpdateAbsenceReq req){
        return absenceRequestService.updateStatusAbsence(req);
    }


    @GetMapping("/check-time")
    public ResponseEntity<?> getListShipment()
    {
        return absenceRequestService.checkTimeAbsence();
    }





}
