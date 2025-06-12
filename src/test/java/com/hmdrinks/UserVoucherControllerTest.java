package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.UserVoucherController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Enum.Status_UserVoucher;
import com.hmdrinks.Repository.CategoryRepository;
import com.hmdrinks.Repository.PriceHistoryRepository;
import com.hmdrinks.Repository.ProductRepository;
import com.hmdrinks.Repository.TokenRepository;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Request.GetVoucherReq;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(UserVoucherController.class)
class UserVoucherControllerTest {
    private static final String endPointPath="/api/user-voucher";
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
    private  UserVoucherService userVoucherService;
    @MockBean
    private  VoucherService voucherService;
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
    void getVoucher_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        GetVoucherReq req = new GetVoucherReq(
                1,
                1
        );
        GetVoucherResponse response = new GetVoucherResponse(
                1,
                1,
                1,
                String.valueOf(Status_UserVoucher.INACTIVE)
        );

        when(userVoucherService.getVoucher(any(GetVoucherReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/get-voucher")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andDo(print());
    }


    @Test
    void getVoucher_NotFoundUser() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        GetVoucherReq req = new GetVoucherReq(
                1,
                1
        );

        when(userVoucherService.getVoucher(any(GetVoucherReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/get-voucher")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }

    @Test
    void getVoucher_NotFoundVoucher() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        GetVoucherReq req = new GetVoucherReq(
                1,
                1
        );

        when(userVoucherService.getVoucher(any(GetVoucherReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found voucher for Post"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/get-voucher")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found voucher for Post"))
                .andDo(print());
    }



    @Test
    void getVoucher_Exists() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        GetVoucherReq req = new GetVoucherReq(
                1,
                1
        );


        when(userVoucherService.getVoucher(any(GetVoucherReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("Voucher already exists"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/get-voucher")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().string("Voucher already exists"))
                .andDo(print());
    }

    @Test
    void getVoucher_NotValid() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        GetVoucherReq req = new GetVoucherReq(
                1,
                1
        );


        when(userVoucherService.getVoucher(any(GetVoucherReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The current date is not within the valid period."));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/get-voucher")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The current date is not within the valid period."))
                .andDo(print());
    }


    @Test
    void GetAllVouchers_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);

        when(supportFunction.validatePositiveId(eq("id"), eq(1)))
                .thenReturn(null);

        GetVoucherResponse response = new GetVoucherResponse(
                1,
                1,
                1,
                String.valueOf(Status_UserVoucher.INACTIVE)
        );
        GetVoucherResponse response1 = new GetVoucherResponse(
                2,
                1,
                2,
                String.valueOf(Status_UserVoucher.EXPIRED)
        );

        ListAllVoucherUserIdResponse listAllVoucherUserIdResponse = new ListAllVoucherUserIdResponse(
                2,
                Arrays.asList(response, response1)
        );

        when(userVoucherService.listAllVoucherUserId(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(listAllVoucherUserIdResponse));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/view-all/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.getVoucherResponseList[1].voucherId").value(2))
                .andDo(print());
    }



    @Test
    void GetAllVouchers_NotFound() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        when(supportFunction.validatePositiveId(eq("id"), eq(1)))
                .thenReturn(null);
        when(userVoucherService.listAllVoucherUserId(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user"));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/view-all/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }

}
