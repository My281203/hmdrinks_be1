package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartItemGroupController;
import com.hmdrinks.Controller.CartItemGroupLeaderController;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Type_Time_Group;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.CRUDCartItemResponse;
import com.hmdrinks.Response.ChangeSizeItemResponse;
import com.hmdrinks.Response.DeleteCartItemResponse;
import com.hmdrinks.Response.IncreaseDecreaseItemQuantityResponse;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(CartItemGroupLeaderController.class)
class CartItemGroupLeaderControllerTest {
    private static final String endPointPath="/api/cart-item/group-order/leader";
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
    private CartItemGroupRepository cartItemGroupRepository;
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
    private CartGroupRepository cartGroupRepository;
    @MockBean
    private ProductVariantsRepository productVariantsRepository;
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
    @MockBean
    private GroupOrdersRepository groupOrdersRepository;
    @MockBean
    private GroupOrderMembersRepository groupOrderMembersRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testCreateCartItem_Success() throws Exception {
        // Arrange
        InsertItemToCartLeader request = new InsertItemToCartLeader();
        request.setGroupOrderId(1);
        request.setUserIdLeader(1);
        request.setUserIdMember(2);
        request.setCartId(3);
        request.setProId(4);
        request.setSize(Size.M); // enum
        request.setQuantity(2);
        request.setLanguage(Language.VN);

        CRUDCartItemResponse response = new CRUDCartItemResponse(
                10, 4, "Sản phẩm A", 3, Size.M, 100.0, 2, "http://image.jpg"
        );

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(cartItemGroupLeaderService.insertCartItemLeader(any(InsertItemToCartLeader.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        // Act & Assert
        mockMvc.perform(post(endPointPath + "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(10))
                .andExpect(jsonPath("$.proName").value("Sản phẩm A"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void testCreateCartItem_Unauthorized() throws Exception {
        InsertItemToCartLeader request = new InsertItemToCartLeader();
        request.setGroupOrderId(1);
        request.setUserIdLeader(1);
        request.setUserIdMember(2);
        request.setCartId(3);
        request.setProId(4);
        request.setSize(Size.M);
        request.setQuantity(2);
        request.setLanguage(Language.VN);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authorized"));

        mockMvc.perform(post(endPointPath + "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authorized"));
    }

    @Test
    void increaseCartItemQuantity_ExistingItem_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(4);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(5, 500.0);
        when(cartItemGroupLeaderService.increaseCartItemGroupQuantityLeader(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath + "/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(500.0));
    }

    @Test
    void increaseCartItemQuantity_Success_ENLanguage() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(2);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(3, 300.0);
        when(cartItemGroupLeaderService.increaseCartItemGroupQuantityLeader(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath + "/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(300.0));
    }

    @Test
    void increaseCartItemQuantity_UserNotLeader_Fail() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(99); // không phải leader
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(2);

        when(cartItemGroupLeaderService.increaseCartItemGroupQuantityLeader(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.badRequest().body("Leader not found"));

        mockMvc.perform(put(endPointPath + "/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Leader not found"));
    }

    @Test
    void decreaseCartItemQuantity_ExistingItem_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(3);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(2, 200.0);
        when(cartItemGroupLeaderService.decreaseCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath + "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.0));
    }

    @Test
    void decreaseCartItemQuantity_Success_ENLanguage() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(1);
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(5);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(4, 400.0);
        when(cartItemGroupLeaderService.decreaseCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath + "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.totalPrice").value(400.0));
    }

    @Test
    void decreaseCartItemQuantity_UserNotLeader_Fail() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(99);
        req.setUserIdMember(2);
        req.setCartItemId(100);
        req.setQuantity(2);

        when(cartItemGroupLeaderService.decreaseCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.badRequest().body("Leader not found"));

        mockMvc.perform(put(endPointPath + "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Leader not found"));
    }

    @Test
    void changeSizeCartItem_Success() throws Exception {
        ChangeSizeItemLeaderReq req = new ChangeSizeItemLeaderReq(1, 2, 3, 4, Size.L);
        ChangeSizeItemResponse response = new ChangeSizeItemResponse(Size.L, 5, 250000.0);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.changeSizeCartItemQuantity(any(ChangeSizeItemLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(put(endPointPath + "/change-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value("L"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(250000.0));
    }

    @Test
    void changeSizeCartItem_InvalidLeader() throws Exception {
        ChangeSizeItemLeaderReq req = new ChangeSizeItemLeaderReq(1, 2, 3, 4, Size.L);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.changeSizeCartItemQuantity(any(ChangeSizeItemLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found"));

        mockMvc.perform(put(endPointPath + "/change-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Leader not found"));
    }

    @Test
    void updateCartItemQuantity_Success() throws Exception {
        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(2);
        req.setUserIdMember(3);
        req.setCartItemId(4);
        req.setQuantity(5);

        IncreaseDecreaseItemQuantityResponse response = new IncreaseDecreaseItemQuantityResponse(5, 150000.0);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(150000.0));
    }

    @Test
    void updateCartItemQuantity_InvalidLeader() throws Exception {
        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(1);
        req.setUserIdLeader(2);
        req.setUserIdMember(3);
        req.setCartItemId(4);
        req.setQuantity(5);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found"));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Leader not found"));
    }

    @Test
    void updateCartItemQuantity_GroupOrderNotFound() throws Exception {
        IncreaseDecreaseItemQuantityLeaderReq req = new IncreaseDecreaseItemQuantityLeaderReq();
        req.setGroupOrderId(9999);
        req.setUserIdLeader(2);
        req.setUserIdMember(3);
        req.setCartItemId(4);
        req.setQuantity(5);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found"));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
    }
    @Test
    void deleteOneItem_Success() throws Exception {
        DeleteOneCartItemLeaderReq req = new DeleteOneCartItemLeaderReq(1, 2, 3, 4);
        DeleteCartItemResponse response = new DeleteCartItemResponse("Delete item success");

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(cartItemGroupLeaderService.deleteOneItem(any(DeleteOneCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(delete(endPointPath + "/delete/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete item success"));
    }

    @Test
    void deleteOneItem_InvalidLeader_Returns400() throws Exception {
        DeleteOneCartItemLeaderReq req = new DeleteOneCartItemLeaderReq(1, 2, 3, 4);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)mockAuthResponse);

        when(cartItemGroupLeaderService.deleteOneItem(any(DeleteOneCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found"));

        mockMvc.perform(delete(endPointPath + "/delete/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Leader not found"));
    }

    @Test
    void deleteOneItem_GroupOrderNotFound_Returns404() throws Exception {
        DeleteOneCartItemLeaderReq req = new DeleteOneCartItemLeaderReq(999, 2, 3, 4);

        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)mockAuthResponse);

        when(cartItemGroupLeaderService.deleteOneItem(any(DeleteOneCartItemLeaderReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found"));

        mockMvc.perform(delete(endPointPath + "/delete/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
    }

}
