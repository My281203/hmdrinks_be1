package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.AdminController;
import com.hmdrinks.Controller.AuthenticationController;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.LoginBasicReq;
import com.hmdrinks.Request.UserCreateReq;
import com.hmdrinks.Response.AuthenticationResponse;
import com.hmdrinks.Response.ListProductImageResponse;
import com.hmdrinks.Response.ProductImageResponse;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(controllers = AuthenticationController.class)
class AuthenticationControllerTest {
    private static final String endPointPath = "/api/v1/auth";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @MockBean
    private ProductRepository productRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;

    @MockBean
    private ZaloPayService zaloPayService;

    @MockBean
    private CartItemService cartItemService;
    @MockBean
    private AbsenceRequestRepository absenceRequestRepository;
    @MockBean
    private UserService userService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private AbsenceRequestService absenceRequestService;
    @MockBean
    private ProductService productService;
    @MockBean
    private PaymentService paymentService;
    @MockBean
    private PaymentGroupService paymentGroupService;
    @MockBean
    private ProductVarService productVarService;
    @MockBean
    private SupportFunction supportFunction;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private ProductTranslationRepository productTranslationRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PaymentGroupRepository paymentGroupRepository;
    @MockBean
    private PaymentRepository paymentRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserInfoService myUserDetailsService;
    @MockBean
    private ElasticsearchSyncService elasticsearchSyncService;
    @MockBean
    private TokenRepository tokenRepository;
    @MockBean
    private ReviewRepository reviewRepository;
    @MockBean
    private PriceHistoryRepository priceHistoryRepository;
    @MockBean
    private CartService cartService;
    @MockBean
    private UserVoucherService userVoucherService;
    @MockBean
    private VoucherService voucherService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testSecured_ReturnsHello() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    @Test
    void testSecured_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2")
                        .header("Authorization", "InvalidToken"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    @Test
    void testAuthenticate_Success() throws Exception {
        LoginBasicReq request = new LoginBasicReq();
        request.setUserName("john");
        request.setPassword("password");

        AuthenticationResponse response = new AuthenticationResponse();
        response.setAccessToken("access-token");
        response.setRefreshToken("refresh-token");

        Mockito.when(authenticationService.authenticate(Mockito.any(LoginBasicReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));
    }

    @Test
    void testAuthenticate_UserNotFound() throws Exception {
        LoginBasicReq request = new LoginBasicReq();
        request.setUserName("unknown");
        request.setPassword("password");

        Mockito.when(authenticationService.authenticate(Mockito.any(LoginBasicReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user name"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user name"));
    }

    @Test
    void testRegister_Success() throws Exception {
        UserCreateReq req = new UserCreateReq();
        req.setUserName("newuser");
        req.setPassword("securePass123");
        req.setFullName("New User");
        req.setEmail("newuser@example.com");

        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setAccessToken("mocked-access-token");
        authResponse.setRefreshToken("mocked-refresh-token");

        Mockito.when(authenticationService.register(Mockito.any(UserCreateReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(authResponse));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("mocked-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("mocked-refresh-token"));
    }

    @Test
    void testRegister_UsernameAlreadyExists() throws Exception {
        UserCreateReq req = new UserCreateReq();
        req.setUserName("existingUser");
        req.setPassword("somePassword");
        req.setFullName("Already Exists");
        req.setEmail("exists@example.com");

        Mockito.when(authenticationService.register(Mockito.any(UserCreateReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("User name already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(content().string("User name already exists"));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        String fakeRefreshToken = "valid-refresh-token";
        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setAccessToken("new-access-token");
        authResponse.setRefreshToken(fakeRefreshToken);

        Mockito.when(authenticationService.refreshToken(
                        Mockito.any(HttpServletRequest.class),
                        Mockito.any(HttpServletResponse.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(authResponse));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.refresh_token").value(fakeRefreshToken));
    }

    @Test
    void testRefreshToken_UnauthorizedHeader() throws Exception {
        Mockito.when(authenticationService.refreshToken(
                        Mockito.any(HttpServletRequest.class),
                        Mockito.any(HttpServletResponse.class)))
                .thenReturn((ResponseEntity) ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("header is null or didnt not start with Beare"));

        mockMvc.perform(post("/api/v1/auth/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("header is null or didnt not start with Beare"));
    }

    @Test
    void testHandleGoogleCallback_Success() throws Exception {
        String code = "sample-auth-code";
        String decodedEmail = "user@example.com";
        AuthenticationResponse response = new AuthenticationResponse();
        response.setAccessToken("access-token");
        response.setRefreshToken("refresh-token");

        Mockito.when(userService.googleLogin(Mockito.eq(code))).thenReturn(decodedEmail);
        Mockito.when(authenticationService.authenticateGoogle(Mockito.eq(decodedEmail)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/v1/auth/oauth2/callback")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));
    }

    @Test
    void testHandleGoogleCallback_GoogleLoginFailed() throws Exception {
        String code = "invalid-code";

        Mockito.when(userService.googleLogin(Mockito.eq(code))).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/oauth2/callback")
                        .param("code", code))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to login"));
    }

    @Test
    void testHandleGoogleCallback1_Success() throws Exception {
        String googleOAuthUrl = "https://accounts.google.com/o/oauth2/auth?client_id=abc&redirect_uri=xyz";

        Mockito.when(authenticationService.generateGoogleOAuthURL())
                .thenReturn(googleOAuthUrl);

        mockMvc.perform(get("/api/v1/auth/social-login/google"))
                .andExpect(status().isOk())
                .andExpect(content().string(googleOAuthUrl));
    }

    @Test
    void testHandleGoogleCallback1_Error() throws Exception {
        Mockito.when(authenticationService.generateGoogleOAuthURL())
                .thenThrow(new UnsupportedEncodingException("Encoding error"));

        mockMvc.perform(get("/api/v1/auth/social-login/google"))
                .andExpect(status().isInternalServerError());
    }

}
