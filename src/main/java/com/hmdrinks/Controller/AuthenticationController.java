package com.hmdrinks.Controller;

import com.hmdrinks.Request.LoginBasicReq;
import com.hmdrinks.Request.UserCreateReq;
import com.hmdrinks.Response.AuthenticationResponse;
import com.hmdrinks.Service.AuthenticationService;
import com.hmdrinks.Service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@RestController
@SecurityRequirement(name = "bearerAuth") // Keep it only for the secured endpoint
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping("/oauth2")
    public String secured()
    {
        return "hello";
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @RequestBody @Valid LoginBasicReq request
    ) {
        return authenticationService.authenticate(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody @Validated UserCreateReq req
    ){
//        LoginBasicReq loginReq = new LoginBasicReq();
//        loginReq.setUserName(req.getUserName());
//        loginReq.setPassword(req.getPassword());
//
//        // Gọi phương thức authenticate() với đối tượng LoginBasicReq đã tạo
//        return this.authenticate(loginReq);
        return authenticationService.register(req);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return authenticationService.refreshToken(request, response);
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) throws UnsupportedEncodingException {
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body("Authorization code is missing");
        }
        String code1 = URLDecoder.decode(code, StandardCharsets.UTF_8.name());
        String email = userService.googleLogin(code1);
        if(email == null) {
            return ResponseEntity.badRequest().body("Failed to login");
        }
        return authenticationService.authenticateGoogle(email);
    }

    @GetMapping("/social-login/google")
    public ResponseEntity<?> handleGoogleCallback1() {
        try {
            String url = authenticationService.generateGoogleOAuthURL();
            return ResponseEntity.ok(url);
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Encoding error");
        }
    }


}
