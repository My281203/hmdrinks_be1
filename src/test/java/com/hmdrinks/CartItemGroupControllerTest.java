package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartGroupController;
import com.hmdrinks.Controller.CartItemGroupController;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.Status_Type_Time_Group;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(CartItemGroupController.class)
class CartItemGroupControllerTest {
    private static final String endPointPath="/api/cart-item/group-order";
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void insertCartItem_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        InsertItemToCart req = new InsertItemToCart(1, 1, 1, Size.S, 5, Language.VN);
        CRUDCartItemResponse expectedResponse = new CRUDCartItemResponse(
                10, 1, "Sản phẩm A", 1, Size.S, 250.0, 5, "https://img.com/abc.jpg"
        );
        when(cartItemGroupService.insertCartItem(any(InsertItemToCart.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(post(endPointPath+ "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(10))
                .andExpect(jsonPath("$.proName").value("Sản phẩm A"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void insertCartItem_ProductNotFound() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        InsertItemToCart req = new InsertItemToCart(1, 1, 999, Size.S, 2, Language.VN);
        when(cartItemGroupService.insertCartItem(any(InsertItemToCart.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product Not Found"));

        mockMvc.perform(post(endPointPath+ "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Product Not Found"));
    }

    @Test
    void insertCartItem_QuantityNegative() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)mockAuthResponse);

        InsertItemToCart req = new InsertItemToCart(1, 1, 1, Size.S, -1, Language.VN);
        when(cartItemGroupService.insertCartItem(any(InsertItemToCart.class)))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Quantity is less than 0"));

        mockMvc.perform(post(endPointPath+ "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Quantity is less than 0"));
    }

    @Test
    void increaseItemQuantity_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(2);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(3, 450.0);

        when(cartItemGroupService.increaseCartItemGroupQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath+ "/increase")  // Giả định endpoint là /cart/increase
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(450.0));
    }

    @Test
    void increaseItemQuantity_AuthFail() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(2);

        mockMvc.perform(put(endPointPath+ "/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("Forbidden"));
    }

    @Test
    void increaseItemQuantity_Timeout() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(2);

        when(cartItemGroupService.increaseCartItemGroupQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(put(endPointPath+ "/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

    @Test
    void decreaseItemQuantity_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(3);

        IncreaseDecreaseItemQuantityResponse expectedResponse =
                new IncreaseDecreaseItemQuantityResponse(2, 100.0);

        when(cartItemGroupService.decreaseCartItemQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath+ "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(100.0));
    }

    @Test
    void decreaseItemQuantity_Timeout() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(2);

        when(cartItemGroupService.decreaseCartItemQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(put(endPointPath+ "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

    @Test
    void decreaseItemQuantity_AuthFail() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setQuantity(1);

        mockMvc.perform(put(endPointPath+ "/decrease")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void changeSizeCartItem_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        ChangeSizeItemReq req = new ChangeSizeItemReq();
        req.setUserId(1);
        req.setCartItemId(101);
        req.setSize(Size.L);

        ChangeSizeItemResponse expectedResponse = new ChangeSizeItemResponse(Size.L, 2, 200.0);

        when(cartItemGroupService.changeSizeCartItemQuantity(any(ChangeSizeItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath+ "/change-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value("L"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.0));
    }

    @Test
    void changeSizeCartItem_CartItemNotFound() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        ChangeSizeItemReq req = new ChangeSizeItemReq();
        req.setUserId(1);
        req.setCartItemId(9999);
        req.setSize(Size.M);

        when(cartItemGroupService.changeSizeCartItemQuantity(any(ChangeSizeItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found"));

        mockMvc.perform(put(endPointPath+ "/change-size")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("CartItem Not Found"));
    }

    @Test
    void updateCartItemQuantity_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(100);
        req.setQuantity(3);

        IncreaseDecreaseItemQuantityResponse expectedResponse = new IncreaseDecreaseItemQuantityResponse(3, 150.0);

        when(cartItemGroupService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(expectedResponse));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(150.0));
    }

    @Test
    void updateCartItemQuantity_CartItemNotFound() throws Exception {
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(9999);
        req.setQuantity(2);

        when(cartItemGroupService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("CartItem Not Found"));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("CartItem Not Found"));
    }

    @Test
    void updateCartItemQuantity_QuantityLessThanOrEqualZero() throws Exception {
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        IncreaseDecreaseItemQuantityReq req = new IncreaseDecreaseItemQuantityReq();
        req.setUserId(1);
        req.setCartItemId(100);
        req.setQuantity(0);

        when(cartItemGroupService.updateCartItemQuantity(any(IncreaseDecreaseItemQuantityReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity is less than 0"));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Quantity is less than 0"));
    }

    @Test
    void deleteOneItem_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        DeleteOneCartItemReq req = new DeleteOneCartItemReq();
        req.setUserId(1);
        req.setCartItemId(100);

        DeleteCartItemResponse response = new DeleteCartItemResponse("Delete item success");

        when(cartItemGroupService.deleteOneItem(any(DeleteOneCartItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(delete(endPointPath + "/delete/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete item success"));
    }

    @Test
    void deleteOneItem_GroupOrderCompletedOrCanceled() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        DeleteOneCartItemReq req = new DeleteOneCartItemReq();
        req.setUserId(1);
        req.setCartItemId(123);

        when(cartItemGroupService.deleteOneItem(any(DeleteOneCartItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled"));

        mockMvc.perform(delete(endPointPath + "/delete/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(content().string("Group Order Completed or Canceled"));
    }

    @Test
    void deleteOneItem_TimeoutGroupOrder() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        DeleteOneCartItemReq req = new DeleteOneCartItemReq();
        req.setUserId(1);
        req.setCartItemId(200);

        when(cartItemGroupService.deleteOneItem(any(DeleteOneCartItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(delete(endPointPath + "/delete/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

}
