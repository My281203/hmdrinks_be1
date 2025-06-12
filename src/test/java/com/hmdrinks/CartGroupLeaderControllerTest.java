package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartGroupController;
import com.hmdrinks.Controller.CartGroupLeaderController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.DeleteAllCartItemLeaderReq;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(CartGroupLeaderController.class)
class CartGroupLeaderControllerTest {
    private static final String endPointPath="/api/cart/group-order/leader";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @MockBean
    private ProductRepository productRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private CartItemService cartItemService;

    @MockBean
    private UserService userService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private  ProductService productService;
    @MockBean
    private ProductVarService productVarService;
    @MockBean
    private SupportFunction supportFunction;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserInfoService myUserDetailsService;
    @MockBean
    private  ElasticsearchSyncService elasticsearchSyncService;
    @MockBean
    private TokenRepository tokenRepository;
    @MockBean
    private PriceHistoryRepository priceHistoryRepository;
    @MockBean
    private CartService cartService;
    @MockBean
    private CartGroupService cartGroupService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;

    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private AdminService adminService;
    @MockBean
    private ShipmentService shipmentService;
    @MockBean
    private ZaloPayService zaloPayService;
    @MockBean
    private AbsenceRequestRepository absenceRequestRepository;
    @MockBean
    private AbsenceRequestService absenceRequestService;
    @MockBean
    private PaymentService paymentService;
    @MockBean
    private PaymentGroupService paymentGroupService;
    @MockBean
    private ProductTranslationRepository productTranslationRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PaymentGroupRepository paymentGroupRepository;
    @MockBean
    private ReviewRepository reviewRepository;
    @MockBean
    private UserVoucherService userVoucherService;
    @MockBean
    private VoucherService voucherService;
    @MockBean
    private CartItemGroupService cartItemGroupService;
    @MockBean
    private CartItemGroupLeaderService cartItemGroupLeaderService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void deleteAllItem_Success() throws Exception {
        DeleteAllCartItemLeaderReq req = new DeleteAllCartItemLeaderReq();
        req.setGroupOrderId(10);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartId(101);

        DeleteCartItemResponse response = new DeleteCartItemResponse("Delete all item success");

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(1)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupLeaderService.deleteAllCartItem(any(DeleteAllCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(delete("/api/cart/group-order/leader/delete-allItem/{id}", req.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete all item success"));
    }

    @Test
    void deleteAllItem_UserLeaderNotFound() throws Exception {
        DeleteAllCartItemLeaderReq req = new DeleteAllCartItemLeaderReq();
        req.setGroupOrderId(10);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartId(101);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(1)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupLeaderService.deleteAllCartItem(any(DeleteAllCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserLeader Not Found"));

        mockMvc.perform(delete("/api/cart/group-order/leader/delete-allItem/{id}", req.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("UserLeader Not Found"));
    }

    @Test
    void deleteAllItem_GroupOrderExpired() throws Exception {
        DeleteAllCartItemLeaderReq req = new DeleteAllCartItemLeaderReq();
        req.setGroupOrderId(10);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartId(101);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(1)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupLeaderService.deleteAllCartItem(any(DeleteAllCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(delete("/api/cart/group-order/leader/delete-allItem/{id}", req.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

    @Test
    void deleteAllItem_GroupOrderAlreadyCompleted() throws Exception {
        DeleteAllCartItemLeaderReq req = new DeleteAllCartItemLeaderReq();
        req.setGroupOrderId(10);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartId(101);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(1)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupLeaderService.deleteAllCartItem(any(DeleteAllCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Group Order Completed or Canceled"));

        mockMvc.perform(delete("/api/cart/group-order/leader/delete-allItem/{id}", req.getCartId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Group Order Completed or Canceled"));
    }

}
