package com.hmdrinks.Controller;

import com.hmdrinks.Request.GetVoucherReq;
import com.hmdrinks.Request.IdReq;
import com.hmdrinks.Service.UserCoinService;
import com.hmdrinks.Service.UserVoucherService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/user-coin")
public class UserCoinController {
    @Autowired
    private UserVoucherService userVoucherService;
    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private SupportFunction supportFunction;

    @PostMapping(value = "/get-coin")
    public ResponseEntity<?> getInfoCoin(@RequestBody @Valid IdReq idReq, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, idReq.getId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return ResponseEntity.ok(userCoinService.getInfoPointCoin(idReq.getId()));
    }

}