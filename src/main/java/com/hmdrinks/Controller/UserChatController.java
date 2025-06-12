package com.hmdrinks.Controller;

import com.hmdrinks.Request.DeleteChatRequest;
import com.hmdrinks.Request.IdReq;
import com.hmdrinks.Request.QuestionChatRequest;
import com.hmdrinks.Request.UpdateNameChatRequest;
import com.hmdrinks.Service.UserChatService;
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
@RequestMapping("/api/user-chat")
public class UserChatController {
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private UserChatService userChatService;

    @PostMapping(value = "/create")
    public ResponseEntity<?> getInfoCoin(@RequestBody @Valid  IdReq idReq, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, idReq.getId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return ResponseEntity.ok(userChatService.createNewChat(idReq.getId(), token));
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateNameChat(@RequestBody @Valid  UpdateNameChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.updateNameChat(req, token);
    }

    @DeleteMapping(value = "/delete")
    public ResponseEntity<?> deleteChat(@RequestBody @Valid  DeleteChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.deleteChat(req, token);
    }

    @PutMapping(value = "/stop")
    public ResponseEntity<?> stopChat(@RequestBody @Valid  DeleteChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");

        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.stopChat(req.getChatId(),token,req.getUserId());
    }


    @PostMapping(value = "/detail_chats")
    public ResponseEntity<?> deleteChat11(@RequestBody @Valid  DeleteChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.getDetailChat(req,token);
    }


    @GetMapping(value = "/list/{userId}")
    public ResponseEntity<?> listChat(@PathVariable Integer userId, HttpServletRequest httpRequest) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userid", userId);
        if (validation != null) return validation;
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, userId);
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return ResponseEntity.ok(userChatService.ListChatHistory(userId));
    }


    @PostMapping(value = "/question")
    public ResponseEntity<?> deleteChat1(@RequestBody @Valid  QuestionChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        token = token.replaceFirst("Bearer ", "").trim();
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.sendQuestion(token,req,req.getLanguage());
    }

    @PutMapping(value = "/regenerate")
    public ResponseEntity<?> Regenerate(@RequestBody @Valid  QuestionChatRequest req, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        token = token.replaceFirst("Bearer ", "").trim();
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }

        return userChatService.sendRegeneration(token,req,req.getLanguage());
    }


}