package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.CartGroupController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(CartGroupController.class)
class CartGroupControllerTest {
    private static final String endPointPath="/api/cart/group-order";
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void listAllCartItem_Success() throws Exception {
        int cartId = 1;
        Language language = Language.VN;

        List<CRUDCartItemGroupResponse> itemList = List.of(
                new CRUDCartItemGroupResponse(1, 101, "Sản phẩm A", cartId, Size.M, 100.0, 100.0, 1, "image1.jpg"),
                new CRUDCartItemGroupResponse(2, 102, "Sản phẩm B", cartId, Size.L, 200.0, 200.0, 1, "image2.jpg")
        );

        ListItemCartGroupResponse response = new ListItemCartGroupResponse(cartId, itemList.size(), itemList);

        when(cartGroupService.getAllItemCart(eq(cartId), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/cart/group-order/list-cartItem/{cartId}", cartId)
                        .param("language", "VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cartId))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.listCartItemResponses[0].proId").value(101))
                .andDo(print());
    }

    @Test
    void listAllCartItem_NotFound() throws Exception {
        int cartId = 999;
        Language language = Language.EN;

        when(cartGroupService.getAllItemCart(eq(cartId), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found cart"));

        mockMvc.perform(get("/api/cart/group-order/list-cartItem/{cartId}", cartId)
                        .param("language", "EN"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found cart"))
                .andDo(print());
    }

    @Test
    void listAllCartItem_WithTranslation_EN() throws Exception {
        int cartId = 2;
        Language language = Language.EN;

        List<CRUDCartItemGroupResponse> itemList = List.of(
                new CRUDCartItemGroupResponse(3, 201, "Product A", cartId, Size.S, 50.0, 50.0, 1, "image3.jpg")
        );

        ListItemCartGroupResponse response = new ListItemCartGroupResponse(cartId, itemList.size(), itemList);

        when(cartGroupService.getAllItemCart(eq(cartId), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/cart/group-order/list-cartItem/{cartId}", cartId)
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listCartItemResponses[0].proName").value("Product A"))
                .andDo(print());
    }

    @Test
    void deleteAllItem_Success() throws Exception {
        int userId = 1;
        int cartId = 101;

        DeleteAllCartItemReq req = new DeleteAllCartItemReq();
        req.setUserId(userId);
        req.setCartId(cartId);

        DeleteCartItemResponse response = new DeleteCartItemResponse("Delete all item success");

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(userId)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupService.deleteAllCartItem(any(DeleteAllCartItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(delete("/api/cart/group-order/delete-allItem/{id}", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete all item success"))
                .andDo(print());
    }

    @Test
    void deleteAllItem_GroupCompleted() throws Exception {
        int userId = 1;
        int cartId = 101;

        DeleteAllCartItemReq req = new DeleteAllCartItemReq();
        req.setUserId(userId);
        req.setCartId(cartId);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(userId)))
                .thenReturn(ResponseEntity.ok().build());

        when(cartItemGroupService.deleteAllCartItem(any(DeleteAllCartItemReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("Group Order Completed or Canceled"));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(delete("/api/cart/group-order/delete-allItem/{id}", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().string("Group Order Completed or Canceled"))
                .andDo(print());
    }

    @Test
    void getAllCartUser_Success() throws Exception {
        int userId = 1;
        int memberId = 10;

        List<CreateNewCartResponse> cartList = List.of(
                new CreateNewCartResponse(101, 150.0, 3, userId),
                new CreateNewCartResponse(102, 200.0, 5, userId)
        );

        ListAllCartUserResponse response = new ListAllCartUserResponse(userId, cartList.size(), cartList);

        when(cartGroupService.getAllCartGroupFromUserAndMemberId(eq(userId), eq(memberId)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/cart/group-order/list-cart/{userId}", userId)
                        .param("memberId", String.valueOf(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.listCart[0].cartId").value(101))
                .andExpect(jsonPath("$.listCart[0].price").value(150.0))
                .andExpect(jsonPath("$.listCart[0].totalProduct").value(3))
                .andDo(print());
    }

    @Test
    void getAllCartUser_UserNotFound() throws Exception {
        int userId = 99;
        int memberId = 10;

        when(cartGroupService.getAllCartGroupFromUserAndMemberId(eq(userId), eq(memberId)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found"));

        mockMvc.perform(get("/api/cart/group-order/list-cart/{userId}", userId)
                        .param("memberId", String.valueOf(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("UserId not found"))
                .andDo(print());
    }
}
