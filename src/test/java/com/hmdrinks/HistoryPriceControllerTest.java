package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.HistoryPriceController;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(HistoryPriceController.class)
class HistoryPriceControllerTest {
    private static final String endPointPath="/api/price-history";
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
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private UserService userService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private  ProductService productService;
    @MockBean
    private ProductVarService productVarService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;
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
    private PriceHistoryService priceHistoryService;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createPost_ShouldReturnSuccess_WhenValidParams() throws Exception {
        String page = "1";
        String limit = "10";

        Mockito.when(supportFunction.validatePaginationParams(page, limit)).thenReturn(null);

        ListAllPriceHistoryByVarIdResponse response = new ListAllPriceHistoryByVarIdResponse();
        response.setCurrentPage(1);
        response.setTotalPage(5L);
        response.setLimit(10);
        response.setTotal(50);
        response.setPriceHistoryResponses(Collections.emptyList());

        Mockito.when(priceHistoryService.getListAllHistoryPrice(page, limit))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get("/api/price-history/view/all")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10));
    }

    @Test
    void createPost_ShouldReturnBadRequest_WhenPageInvalid() throws Exception {
        String page = "-1";
        String limit = "10";

        Mockito.when(supportFunction.validatePaginationParams(page, limit))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid page"));

        mockMvc.perform(get("/api/price-history/view/all")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid page"));
    }

    @Test
    void getOneProductVar_ShouldReturnSuccess_WhenValidParams() throws Exception {
        int proVarId = 1;
        String page = "1";
        String limit = "10";

        Mockito.when(supportFunction.validatePositiveId("proVarId", proVarId)).thenReturn(null);
        Mockito.when(supportFunction.validatePaginationParams(page, limit)).thenReturn(null);

        ListAllPriceHistoryByVarIdResponse response = new ListAllPriceHistoryByVarIdResponse();
        response.setCurrentPage(1);
        response.setTotalPage(3L);
        response.setLimit(10);
        response.setTotal(25);
        response.setPriceHistoryResponses(Collections.emptyList());

        Mockito.when(priceHistoryService.getListAllHistoryPriceByProductVarId(page, limit, proVarId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get("/api/price-history/view/productVar")
                        .param("page", page)
                        .param("limit", limit)
                        .param("proVarId", String.valueOf(proVarId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10));
    }

    @Test
    void getOneProductVar_ShouldReturnNotFound_WhenInvalidProVarId() throws Exception {
        int proVarId = -1;
        String page = "1";
        String limit = "10";

        Mockito.when(supportFunction.validatePositiveId("proVarId", proVarId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid proVarId"));

        mockMvc.perform(get("/api/price-history/view/productVar")
                        .param("page", page)
                        .param("limit", limit)
                        .param("proVarId", String.valueOf(proVarId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid proVarId"));
    }
}
