package com.hmdrinks.Controller;

import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.VoucherService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/voucher")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private SupportFunction supportFunction;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createVoucher(@RequestBody @Valid CreateVoucherReq req){
        return ResponseEntity.ok(voucherService.createVoucher(req));
    }

    @GetMapping(value ="/view/{id}")
    public ResponseEntity<?> getOneVoucher(
            @PathVariable Integer id,HttpServletRequest httpRequest
    ){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping(value ="/user/key")
    public ResponseEntity<?> getOneVoucher1(
            @RequestBody @Valid  GetVoucherKeyReq req, HttpServletRequest httpRequest
    ){
        return voucherService.getVoucherByKey(req.getKeyVoucher(), req.getUserId());
    }

    @GetMapping(value = "/view/all")
    public ResponseEntity<?> getAllVouchers(){
        return  ResponseEntity.ok(voucherService.listAllVoucher());
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updatePost(
            @RequestBody @Valid CrudVoucherReq req, HttpServletRequest httpRequest
    )
    {
        return ResponseEntity.ok(voucherService.updateVoucher(req));
    }

    @PutMapping(value = "/enable")
    public ResponseEntity<?> enableVoucher(@RequestBody  @Valid IdReq req, HttpServletRequest httpRequest) {
        return  voucherService.enableVoucher(req.getId());
    }

    @PutMapping(value = "/check_status")
    public ResponseEntity<?> disableVoucher1() {
        return  voucherService.checkTimeVoucher();
    }

    @PutMapping(value = "/disable")
    public ResponseEntity<?> disableVoucher(@RequestBody @Valid IdReq req, HttpServletRequest httpRequest) {
        return  voucherService.disableVoucher(req.getId());
    }
}