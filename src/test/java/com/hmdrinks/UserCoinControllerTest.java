package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.UserCoinController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.CategoryRepository;
import com.hmdrinks.Repository.PriceHistoryRepository;
import com.hmdrinks.Repository.ProductRepository;
import com.hmdrinks.Repository.TokenRepository;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Request.IdReq;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(UserCoinController.class)
class UserCoinControllerTest {
    private static final String endPointPath="/api/user-coin";
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
    private  UserCoinService userCoinService;
    @MockBean
    private  UserVoucherService userVoucherService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;
    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private ZaloPayService zaloPayService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void GetCoin_Success() throws Exception {
        IdReq idReq = new IdReq();
        idReq.setId(1);
        CRUDUserCoinResponse response = new CRUDUserCoinResponse(
                1,
                500,
                1
        );
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);

        when(userCoinService.getInfoPointCoin(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));
        String requestBody = objectMapper.writeValueAsString(idReq);
        mockMvc.perform(post(endPointPath + "/get-coin")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.userId").value(1))
                .andExpect(jsonPath("$.body.pointCoin").value(500))
                .andDo(print());
    }


    @Test
    void GetCoin_NotFoundCoin() throws Exception {
        IdReq idReq = new IdReq();
        idReq.setId(1);
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);

        when(userCoinService.getInfoPointCoin(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user Coin"));
        String requestBody = objectMapper.writeValueAsString(idReq);
        mockMvc.perform(post(endPointPath + "/get-coin")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(404))
                .andExpect(jsonPath("$.body").value("Not found user Coin"))
                .andDo(print());
    }

    @Test
    void GetCoin_NotFoundUser() throws Exception {
        IdReq idReq = new IdReq();
        idReq.setId(1);
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);

        when(userCoinService.getInfoPointCoin(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found User"));
        String requestBody = objectMapper.writeValueAsString(idReq);
        mockMvc.perform(post(endPointPath + "/get-coin")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(404))
                .andExpect(jsonPath("$.body").value("Not found User"))
                .andDo(print());
    }

    }





