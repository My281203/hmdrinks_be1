package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.FavouriteController;
import com.hmdrinks.Controller.FavouriteItemController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewFavourite;
import com.hmdrinks.Request.DeleteAllFavouriteItemReq;
import com.hmdrinks.Request.DeleteOneFavouriteItemReq;
import com.hmdrinks.Request.InsertItemToFavourite;
import com.hmdrinks.Response.*;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(FavouriteItemController.class)
class FavouriteItemControllerTest {
    private static final String endPointPath="/api/fav-item";
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
    @MockBean
    private InsertItemToFavourite validRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testCreateFavouriteItem_Success() throws Exception {
        InsertItemToFavourite request = new InsertItemToFavourite();
        request.setUserId(1);
        request.setFavId(2);
        request.setProId(3);
        request.setSize(Size.M);

        CRUDFavouriteItemResponse response = new CRUDFavouriteItemResponse();
        response.setFavItemId(10);
        response.setFavId(2);
        response.setProId(3);
        response.setSize(Size.M);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(favouriteItemService.insertFavouriteItem(any(InsertItemToFavourite.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(post(endPointPath +"/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favItemId").value(10))
                .andExpect(jsonPath("$.favId").value(2))
                .andExpect(jsonPath("$.proId").value(3))
                .andExpect(jsonPath("$.size").value("M"));
    }

    @Test
    void testCreateFavouriteItem_Unauthorized() throws Exception {
        InsertItemToFavourite request = new InsertItemToFavourite();
        request.setUserId(1);
        request.setFavId(2);
        request.setProId(3);
        request.setSize(Size.M);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(post(endPointPath + "/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void listFavourite_EN_Success() throws Exception {
        List<TotalCountFavorite> mockData = List.of(
                new TotalCountFavorite(100, "T-shirt", 3),
                new TotalCountFavorite(101, "Jacket", 2)
        );
        ListAllTotalCountFavorite response = new ListAllTotalCountFavorite(mockData.size(), mockData);

        when(favouriteItemService.listAllTotalFavouriteByProId(Language.EN))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/list")
                        .param("language", "EN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].proId").value(100))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].proName").value("T-shirt"))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].totalCount").value(3));

    }

    @Test
    void listFavourite_VI_Success() throws Exception {
        List<TotalCountFavorite> mockData = List.of(
                new TotalCountFavorite(102, "Áo sơ mi", 5)
        );
        ListAllTotalCountFavorite response = new ListAllTotalCountFavorite(mockData.size(), mockData);

        when(favouriteItemService.listAllTotalFavouriteByProId(Language.VN))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/list")
                        .param("language", "VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].proId").value(102))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].proName").value("Áo sơ mi"))
                .andExpect(jsonPath("$.totalCountFavoriteList[0].totalCount").value(5))
                .andDo(print());
    }

    @Test
    void listFavourite_NoData_ReturnsEmptyList() throws Exception {
        ListAllTotalCountFavorite response = new ListAllTotalCountFavorite(0, List.of());

        when(favouriteItemService.listAllTotalFavouriteByProId(Language.EN))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/list")
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.totalCountFavoriteList").isEmpty())
                .andDo(print());
    }

    @Test
    void deleteOneFavouriteItem_Success() throws Exception {
        DeleteOneFavouriteItemReq request = new DeleteOneFavouriteItemReq();
        request.setUserId(1);
        request.setFavItemId(100);

        DeleteFavouriteItemResponse response = new DeleteFavouriteItemResponse("Delete item success");

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(favouriteItemService.deleteOneItem(any(DeleteOneFavouriteItemReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(delete(endPointPath + "/delete/{id}", 100)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete item success"))
                .andDo(print());
    }

    @Test
    void deleteOneFavouriteItem_Unauthorized() throws Exception {
        DeleteOneFavouriteItemReq request = new DeleteOneFavouriteItemReq();
        request.setUserId(1);
        request.setFavItemId(100);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(delete(endPointPath + "/delete/{id}", 100)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"))
                .andDo(print());
    }

    @Test
    void deleteOneFavouriteItem_ItemNotFound() throws Exception {
        DeleteOneFavouriteItemReq request = new DeleteOneFavouriteItemReq();
        request.setUserId(1);
        request.setFavItemId(999);

        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(favouriteItemService.deleteOneItem(any(DeleteOneFavouriteItemReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favourite item not found"));

        mockMvc.perform(delete(endPointPath + "/delete/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Favourite item not found"))
                .andDo(print());
    }

}
