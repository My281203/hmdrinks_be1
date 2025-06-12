package com.hmdrinks.Controller;

import com.hmdrinks.Request.ForgetPasswordReq;
import com.hmdrinks.Request.ForgetPasswordSendReq;
import com.hmdrinks.Service.ElasticsearchSyncService;
import com.hmdrinks.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
//@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    @Autowired
    private UserService userService;
    @Autowired
    private ElasticsearchSyncService elasticsearchSyncService;


    @PostMapping("/password/forget/send")
    public CompletableFuture<ResponseEntity<?>> getPassword(@RequestBody  ForgetPasswordReq forgetPasswordReq) {
        return userService.sendEmail(forgetPasswordReq.getEmail());
    }

    @PostMapping("/password/acceptOtp")
    public CompletableFuture<ResponseEntity<?>> getSendPassword(@RequestBody   ForgetPasswordSendReq Req) {
        return userService.AcceptOTP(Req.getEmail(), Req.getOtp());
    }

    @GetMapping("/test-sync-search")
    public String testSyncAndSearch() {
        elasticsearchSyncService.testSyncAndSearch();
        return "✅ Test đồng bộ & tìm kiếm đã hoàn tất! Kiểm tra logs.";
    }
}