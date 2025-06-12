package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.FavouriteController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.CreateNewFavourite;
import com.hmdrinks.Request.DeleteAllCartItemReq;
import com.hmdrinks.Request.DeleteAllFavouriteItemReq;
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

import java.time.LocalDateTime;
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
@WebMvcTest(FavouriteController.class)
class FavouriteControllerTest {
    private static final String endPointPath="/api/fav";
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
    private  FavouriteService favouriteService;
    @MockBean
    private  FavouriteItemService favouriteItemService;

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
    private UserRepository userRepository;
    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;
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
    private PaymentGroupRepository paymentGroupRepository;
    @MockBean
    private ReviewRepository reviewRepository;
    @MockBean
    private UserVoucherService userVoucherService;
    @MockBean
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createFavourite_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        CreateNewFavourite req = new CreateNewFavourite(1);
        CreateNewFavouriteResponse resp = new CreateNewFavouriteResponse(
                1,
                1,
                Boolean.FALSE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );


        when(favouriteService.createFavourite(any(CreateNewFavourite.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(resp));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(200))
                .andExpect(jsonPath("$.body.userId").value(1))
                .andDo(print());
    }

    @Test
    void createFav_UserIdNotFound() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);

        CreateNewFavourite req = new CreateNewFavourite(1);

        when(favouriteService.createFavourite(any(CreateNewFavourite.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not exists"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(404))
                .andExpect(jsonPath("$.body").value("UserId not exists"))
                .andDo(print());
    }

    @Test
    void createFav_Conflict() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);

        CreateNewFavourite req = new CreateNewFavourite(1);

        when(favouriteService.createFavourite(any(CreateNewFavourite.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("Favourite already exists"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(409))
                .andExpect(jsonPath("$.body").value("Favourite already exists"))
                .andDo(print());
    }

    @Test
    void getAllFavFromUser_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        CRUDFavouriteItemResponse response1 = new CRUDFavouriteItemResponse(
                1,
                1,
                1,
                Size.S
        );

        CRUDFavouriteItemResponse response2 = new CRUDFavouriteItemResponse(
                2,
                1,
                1,
                Size.M
        );
        ListItemFavouriteResponse listItemFavouriteResponse = new ListItemFavouriteResponse(
                1,
                2,
                Arrays.asList(response1,response2)
        );

        when(favouriteService.getAllItemFavourite(anyInt(),any(Language.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(listItemFavouriteResponse));
        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-favItem/1")
                        .param("language", String.valueOf(Language.VN)))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.favId").value(1))
                 .andExpect(jsonPath("$.total").value(2))
                 .andExpect(jsonPath("$.favouriteItemResponseList[1].size").value(String.valueOf(Size.M)))
                .andDo(print());
    }

    @Test
    void getFavfromFavId_NotFound() throws Exception {

        when(favouriteService.getAllItemFavourite(anyInt(),any(Language.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favourite not found"));
        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-favItem/1")
                        .param("language", String.valueOf(Language.VN)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Favourite not found"))
                .andDo(print());
    }

    @Test
    void  getFavByUserId_Success() throws Exception {
        when(supportFunction.checkUserAuthorizationUpgrade(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(supportFunction.validatePositiveId(eq("id"), eq(1)))
                .thenReturn(null);
        CreateNewFavourite req = new CreateNewFavourite(1);
        CreateNewFavouriteResponse resp = new CreateNewFavouriteResponse(
                1,
                1,
                Boolean.FALSE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );


        when(favouriteService.getFavoriteById(anyInt())).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(resp));
        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-fav/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andDo(print());
    }



    @Test
    void  getFavByUserId_FavouriteNotFound() throws Exception {
        when(supportFunction.checkUserAuthorizationUpgrade(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(supportFunction.validatePositiveId(eq("id"), eq(1)))
                .thenReturn(null);
        when(favouriteService.getFavoriteById(anyInt())).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favorite not found"));
        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-fav/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Favorite not found"))
                .andDo(print());
    }

    @Test
    void  getFavByUserId_UserNotFound() throws Exception {
        when(supportFunction.checkUserAuthorizationUpgrade(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(supportFunction.validatePositiveId(eq("id"), eq(1)))
                .thenReturn(null);
        when(favouriteService.getFavoriteById(anyInt())).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-fav/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"))
                .andDo(print());
    }

    @Test
    void deleteAllFavourite_Success() throws Exception {
        DeleteAllFavouriteItemReq req2 = new DeleteAllFavouriteItemReq(2,2);
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        DeleteFavouriteItemResponse response = new DeleteFavouriteItemResponse(
                "Delete all item success"
        );
        when(favouriteItemService.deleteAllFavouriteItem(any(DeleteAllFavouriteItemReq.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));
        String requestBody = objectMapper.writeValueAsString(req2);
        mockMvc.perform(delete(endPointPath + "/delete-allItem/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete all item success"))
                .andDo(print());
    }

    @Test
    void deleteAllFavItem_NotFound() throws Exception {
        DeleteAllFavouriteItemReq req2 = new DeleteAllFavouriteItemReq(2,2);
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse);
        when(favouriteItemService.deleteAllFavouriteItem(any(DeleteAllFavouriteItemReq.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favourite not found"));
        String requestBody = objectMapper.writeValueAsString(req2);
        mockMvc.perform(delete(endPointPath + "/delete-allItem/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Favourite not found"))
                .andDo(print());
    }

}
