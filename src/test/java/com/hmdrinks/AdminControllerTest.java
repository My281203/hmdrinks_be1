package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.AbsenceRequestController;
import com.hmdrinks.Controller.AdminController;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(AdminController.class)
class AdminControllerTest {
    private static final String endPointPath = "/api/admin";
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
    void testGetListImage_Success() throws Exception {
        int proId = 1;
        List<ProductImageResponse> images = List.of(
                new ProductImageResponse(1, "image1.jpg"),
                new ProductImageResponse(2, "image2.jpg")
        );
        ListProductImageResponse response = new ListProductImageResponse(proId, 2, images);

        Mockito.when(adminService.getAllProductImages(proId))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        String url = "/api/admin/list-image/" + proId;
        String response1 = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        System.out.println(">>> JSON Response: " + response1); // In response để kiểm tra cấu trúc

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.productId").value(proId)) // Thêm .body vào path
                .andExpect(jsonPath("$.body.total").value(2))
                .andExpect(jsonPath("$.body.productImageResponseList[0].id").value(1))
                .andExpect(jsonPath("$.body.productImageResponseList[0].linkImage").value("image1.jpg"))
                .andExpect(jsonPath("$.body.productImageResponseList[1].id").value(2))
                .andExpect(jsonPath("$.body.productImageResponseList[1].linkImage").value("image2.jpg"));
    }
    @Test
    void testGetListImage_EmptyImages() throws Exception {
        int proId = 2;
        List<ProductImageResponse> emptyImages = List.of();
        ListProductImageResponse response = new ListProductImageResponse(proId, 0, emptyImages);

        Mockito.when(adminService.getAllProductImages(proId))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        String url = "/api/admin/list-image/" + proId;
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.productId").value(proId))
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.productImageResponseList").isEmpty());
    }
    @Test
    void testListAllUser_Success() throws Exception {
        String page = "1";
        String limit = "2";

        LocalDate localDate = LocalDate.of(1990, 1, 1);
        Date dateFromLocalDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LocalDateTime localDateTime = LocalDateTime.now();
        Date dateFromLocalDateTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        List<DetailUserResponse> userResponses = List.of(
                new DetailUserResponse(1, "user1", "User One", "avatar1.jpg", dateFromLocalDate,
                        "Street,WardDistrict,City", "user1@example.com", "123456789", "MALE",
                        "NORMAL", false, null, null, dateFromLocalDateTime, "USER"),
                new DetailUserResponse(2, "user2", "User Two", "avatar2.jpg", dateFromLocalDate,
                        "Street2,Ward2District2,City2", "user2@example.com", "987654321", "FEMALE",
                        "NORMAL", false, null, null, dateFromLocalDateTime, "USER")
        );

        ListAllUserResponse response = new ListAllUserResponse(1, 5, 2, 10, userResponses);

        Mockito.when(userService.getListAllUser(page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        // Nếu response được bọc trong "body", thêm .body vào jsonPath
        mockMvc.perform(get("/api/admin/listUser")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPage").value(5))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.detailUserResponseList[0].userId").value(1))
                .andExpect(jsonPath("$.detailUserResponseList[0].userName").value("user1"));
    }
    @Test
    void testListAllUser_ExceedMaxLimit() throws Exception {
        String page = "1";
        String limit = "200";

        ListAllUserResponse response = new ListAllUserResponse(1, 3, 100, 250, new ArrayList<>());

        Mockito.when(userService.getListAllUser(page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/admin/listUser")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(100));
    }
    @Test
    void testListAllUser_MissingPageParam() throws Exception {
        mockMvc.perform(get("/api/admin/listUser")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListAllUser_MissingLimitParam() throws Exception {
        mockMvc.perform(get("/api/admin/listUser")
                        .param("page", "1"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testListAllUserByRole_Success() throws Exception {
        String page = "1";
        String limit = "2";
        Role role = Role.CUSTOMER;

        List<DetailUserResponse> responses = List.of(
                new DetailUserResponse(1, "user1", "User One", "avatar1.jpg", new Date(),
                        "Street,WardDistrict,City", "user1@example.com", "123456789", "MALE",
                        "NORMAL", false, null, null, new Date(), "USER")
        );

        ListAllUserResponse response = new ListAllUserResponse(1, 1, 2, 1, responses);

        Mockito.when(userService.getListAllUserByRole(page, limit, role))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/admin/listUser-role")
                        .param("page", page)
                        .param("limit", limit)
                        .param("role", role.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.detailUserResponseList[0].userName").value("user1"));
    }
    @Test
    void testListAllUserByRole_ExceedMaxLimit() throws Exception {
        String page = "1";
        String limit = "150";
        Role role = Role.ADMIN;

        ListAllUserResponse response = new ListAllUserResponse(1, 1, 100, 1, new ArrayList<>());

        Mockito.when(userService.getListAllUserByRole(page, limit, role))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get("/api/admin/listUser-role")
                        .param("page", page)
                        .param("limit", limit)
                        .param("role", role.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(100));
    }
    @Test
    void testListAllUserByRole_MissingRole() throws Exception {
        mockMvc.perform(get("/api/admin/listUser-role")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testListAllUserByRole_InvalidRoleEnum() throws Exception {
        mockMvc.perform(get("/api/admin/listUser-role")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testCreateAccount_Success() throws Exception {
        String requestBody = """
        {
          "fullName": "Nguyen Van A",
          "userName": "nguyenvana",
          "email": "vana@example.com",
          "password": "password123",
          "role": "CUSTOMER",
          "phone": "0123456789"
        }
        """;

        Mockito.when(userRepository.findByUserNameAndIsDeletedFalse("nguyenvana"))
                .thenReturn(Optional.empty());
        Mockito.when(supportFunction.checkRole("CUSTOMER"))
                .thenReturn(true);
        Mockito.when(userRepository.findByEmail("vana@example.com"))
                .thenReturn(null);

        User savedUser = new User();
        savedUser.setUserId(1);
        savedUser.setUserName("nguyenvana");
        savedUser.setFullName("Nguyen Van A");
        savedUser.setEmail("vana@example.com");
        savedUser.setPassword("hashedPassword");
        savedUser.setRole(Role.CUSTOMER);
        savedUser.setType(TypeLogin.BASIC);
        savedUser.setSex(Sex.OTHER);
        savedUser.setIsDeleted(false);
        Date date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        savedUser.setDateCreated(date);

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(savedUser);
        Mockito.when(userRepository.findByUserNameAndIsDeletedFalse("nguyenvana"))
                .thenReturn(Optional.of(savedUser));

        mockMvc.perform(post("/api/admin/create-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }
    @Test
    void testSearchUser_Success() throws Exception {
        String keyword = "john";
        String page = "1";
        String limit = "2";

        List<DetailUserResponse> responses = List.of(
                new DetailUserResponse(1, "john123", "John Doe", "avatar.jpg", new Date(),
                        "Street,Ward,District,City", "john@example.com", "0987654321", "MALE",
                        "NORMAL", false, null, null, new Date(), "USER")
        );

        TotalSearchUserResponse mockResponse = new TotalSearchUserResponse(1, 1, 2, 1, responses);

        Mockito.when(userService.totalSearchUser(keyword, page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/search-user")
                        .param("keyword", keyword)
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.detailUserResponseList[0].userName").value("john123"));
    }
    @Test
    void testSearchUser_LimitGreaterThan100() throws Exception {
        String keyword = "john";
        String page = "1";
        String limit = "150";

        List<DetailUserResponse> responses = List.of(); // không cần dữ liệu thật
        TotalSearchUserResponse mockResponse = new TotalSearchUserResponse(1, 0, 100, 0, responses);

        Mockito.when(userService.totalSearchUser(keyword, page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/search-user")
                        .param("keyword", keyword)
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100));
    }
    @Test
    void testSearchUser_EmptyResult() throws Exception {
        String keyword = "nothing";
        String page = "1";
        String limit = "10";

        TotalSearchUserResponse mockResponse = new TotalSearchUserResponse(1, 0, 10, 0, new ArrayList<>());

        Mockito.when(userService.totalSearchUser(keyword, page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/search-user")
                        .param("keyword", keyword)
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.detailUserResponseList").isEmpty());
    }
    @Test
    void testUpdateAccount_Success() throws Exception {
        String json = """
            {
              "userId": 1,
              "fullName": "Updated Name",
              "userName": "updatedUser",
              "email": "updated@example.com",
              "password": "newPassword",
              "role": "CUSTOMER",
              "phoneNumber": "0123456789",
              "isDeleted": false
            }
            """;

        mockMvc.perform(put("/api/admin/update-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

    }
    @Test
    void testUpdateAccount_InvalidEmailFormat() throws Exception {
        UpdateAccountUserReq req = new UpdateAccountUserReq();
        req.setUserId(1);
        req.setEmail("not-an-email"); // Sai format @Email

        mockMvc.perform(put("/api/admin/update-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testUpdateAccount_RecoverDeletedUser() throws Exception {
        String json = """
    {
      "userId": 1,
      "isDeleted": false
    }
    """;

        User existingUser = new User();
        existingUser.setUserId(1);
        existingUser.setIsDeleted(true);
        existingUser.setUserName("recoverUser");
        existingUser.setEmail("recover@example.com");
        existingUser.setStreet("S");
        existingUser.setWard("W");
        existingUser.setDistrict("D");
        existingUser.setCity("C");
        existingUser.setRole(Role.CUSTOMER);

        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(existingUser));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(existingUser);

        mockMvc.perform(put("/api/admin/update-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
    @Test
    void testDeleteReview_Success() throws Exception {
        String json = """
    {
        "id": 1
    }
    """;

        Mockito.when(adminService.deleteOneReview(1)).thenReturn("Review deleted");

        mockMvc.perform(delete("/api/admin/product/review/deleteOne")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Review deleted"));
    }
    @Test
    void testDeleteReview_ReviewNotFound() throws Exception {
        String json = """
    {
        "id": 999
    }
    """;

        Mockito.when(adminService.deleteOneReview(999))
                .thenThrow(new BadRequestException("Review not found"));

        mockMvc.perform(delete("/api/admin/product/review/deleteOne")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(result -> Assertions.assertTrue(
                        result.getResolvedException() instanceof BadRequestException))
                .andExpect(result -> Assertions.assertEquals(
                        "Review not found", result.getResolvedException().getMessage()));
    }
    @Test
    void testDeleteReview_InvalidJson() throws Exception {
        String invalidJson = """
    {
        "id": "notAnInteger"
    }
    """;

        mockMvc.perform(delete("/api/admin/product/review/deleteOne")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testDeleteAllReviews_Success() throws Exception {
        String json = """
    {
        "id": 1
    }
    """;

        Mockito.when(adminService.deleteALlReviewProduct(1)).thenReturn("All review product deleted");

        mockMvc.perform(delete("/api/admin/product/review/deleteAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("All review product deleted"));
    }
    @Test
    void testDeleteAllReviews_NoReviewsFound() throws Exception {
        String json = """
    {
        "id": 999
    }
    """;

        Mockito.when(adminService.deleteALlReviewProduct(999)).thenReturn("All review product deleted");

        mockMvc.perform(delete("/api/admin/product/review/deleteAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("All review product deleted"));
    }
    @Test
    void testDeleteAllReviews_InvalidJson() throws Exception {
        String json = """
    {
        "id": "abc"
    }
    """;

        mockMvc.perform(delete("/api/admin/product/review/deleteAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListAllProduct_Success() throws Exception {
        ListProductResponse mockResponse = new ListProductResponse(
                1, 1, 10, 1, List.of()
        );
        ResponseEntity<ListProductResponse> responseEntity = ResponseEntity.ok(mockResponse);

        Mockito.when(adminService.listProduct("1", "10", Language.EN))
                .thenReturn((ResponseEntity) responseEntity);


        mockMvc.perform(get("/api/admin/list-product")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(10))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.productResponses").isArray());
    }
    @Test
    void testListAllProduct_EmptyResult() throws Exception {
        ListProductResponse mockResponse = new ListProductResponse(
                1, 1, 10, 0, List.of()
        );
        ResponseEntity<ListProductResponse> responseEntity = ResponseEntity.ok(mockResponse);
        Mockito.when(adminService.listProduct("1", "10", Language.EN)).thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/list-product")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.productResponses").isEmpty());
    }

    @Test
    void testListAllProduct_LimitGreaterThanMax() throws Exception {
        ListProductResponse mockResponse = new ListProductResponse(
                1, 5, 100, 200, List.of()
        );
        ResponseEntity<ListProductResponse> responseEntity = ResponseEntity.ok(mockResponse);
        Mockito.when(adminService.listProduct("1", "150", Language.EN)).thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/list-product")
                        .param("page", "1")
                        .param("limit", "150")  // limit quá mức
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(5))
                .andExpect(jsonPath("$.body.limit").value(100))  // Kiểm tra rằng nó bị giới hạn về 100
                .andExpect(jsonPath("$.body.total").value(200))
                .andExpect(jsonPath("$.body.productResponses").isArray());
    }

    @Test
    void testSearchProduct_VN_Success() throws Exception {
        TotalSearchProductResponse mockResponse = new TotalSearchProductResponse(
                1, 1, 10, 1, List.of()
        );

        ResponseEntity<TotalSearchProductResponse> responseEntity = ResponseEntity.ok(mockResponse);

        Mockito.when(adminService.totalSearchProduct("trà", "1", "10", Language.VN))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/search-product")
                        .param("keyword", "trà")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "VN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(10))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.productResponseList").isArray());
    }

    @Test
    void testSearchProduct_EN_Success() throws Exception {
        TotalSearchProductResponse mockResponse = new TotalSearchProductResponse(
                1, 2, 10, 15, List.of()
        );

        ResponseEntity<TotalSearchProductResponse> responseEntity = ResponseEntity.ok(mockResponse);

        Mockito.when(adminService.totalSearchProduct("tea", "1", "10", Language.EN))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/search-product")
                        .param("keyword", "tea")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "EN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(2))
                .andExpect(jsonPath("$.body.limit").value(10))
                .andExpect(jsonPath("$.body.total").value(15))
                .andExpect(jsonPath("$.body.productResponseList").isArray());
    }

    @Test
    void testSearchProduct_LimitGreaterThan100() throws Exception {
        TotalSearchProductResponse mockResponse = new TotalSearchProductResponse(
                1, 1, 100, 200, List.of()
        );

        ResponseEntity<TotalSearchProductResponse> responseEntity = ResponseEntity.ok(mockResponse);

        Mockito.when(adminService.totalSearchProduct("milk", "1", "150", Language.VN))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/search-product")
                        .param("keyword", "milk")
                        .param("page", "1")
                        .param("limit", "150")
                        .param("language", "VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100));
    }

    @Test
    void testSearchProduct_NotFound() throws Exception {
        TotalSearchProductResponse mockResponse = new TotalSearchProductResponse(
                1, 0, 10, 0, List.of()
        );

        ResponseEntity<TotalSearchProductResponse> responseEntity = ResponseEntity.ok(mockResponse);

        Mockito.when(adminService.totalSearchProduct("khongtontai", "1", "10", Language.VN))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/admin/search-product")
                        .param("keyword", "khongtontai")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.productResponseList").isEmpty());
    }

    @Test
    void testSearchProduct_MissingKeyword() throws Exception {
        mockMvc.perform(get("/api/admin/search-product")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "VN"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testSearchProduct_InvalidLanguage() throws Exception {
        mockMvc.perform(get("/api/admin/search-product")
                        .param("keyword", "trà")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("language", "JP")) // Không nằm trong enum Language
                .andExpect(status().isBadRequest());
    }
    @Test
    void testGetProductVariants_Success() throws Exception {
        int productId = 1;

        List<CRUDProductVarResponse> variants = List.of(
                new CRUDProductVarResponse(1, productId, Size.S, 100.0, 10, false, null,
                        LocalDateTime.of(2025, 5, 24, 2, 45, 20),
                        LocalDateTime.of(2025, 5, 24, 2, 45, 20)),
                new CRUDProductVarResponse(2, productId, Size.L, 120.0, 5, false, null,
                        LocalDateTime.of(2025, 5, 24, 2, 45, 20),
                        LocalDateTime.of(2025, 5, 24, 2, 45, 20))
        );

        GetProductVariantFromProductIdResponse mockResponse =
                new GetProductVariantFromProductIdResponse(productId, variants);

        Mockito.when(adminService.getAllProductVariantFromProduct(productId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));


        mockMvc.perform(get("/api/admin/product/variants/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.responseList").isArray())
                .andExpect(jsonPath("$.body.responseList.length()").value(2))
                .andExpect(jsonPath("$.body.responseList[0].varId").value(1))
                .andExpect(jsonPath("$.body.responseList[0].size").value("S"))
                .andExpect(jsonPath("$.body.responseList[0].price").value(100.0))
                .andExpect(jsonPath("$.body.responseList[0].stock").value(10))
                .andExpect(jsonPath("$.body.responseList[0].deleted").value(false))
                .andExpect(jsonPath("$.body.responseList[1].varId").value(2))
                .andExpect(jsonPath("$.body.responseList[1].size").value("L"));
    }

    @Test
    void testGetProductVariants_ValidProductButNoVariants() throws Exception {
        int productId = 100;

        GetProductVariantFromProductIdResponse mockResponse =
                new GetProductVariantFromProductIdResponse(productId, List.of());

        Mockito.when(adminService.getAllProductVariantFromProduct(productId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/variants/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.responseList").isArray())
                .andExpect(jsonPath("$.body.responseList.length()").value(0));
    }


    @Test
    void testGetProductVariants_EmptyList() throws Exception {
        int productId = 2;

        GetProductVariantFromProductIdResponse mockResponse =
                new GetProductVariantFromProductIdResponse(productId, List.of());

        Mockito.when(adminService.getAllProductVariantFromProduct(productId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/variants/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.responseList").isArray())
                .andExpect(jsonPath("$.body.responseList.length()").value(0));
    }
    @Test
    void testGetProductVariants_SingleVariant() throws Exception {
        int productId = 3;

        List<CRUDProductVarResponse> variants = List.of(
                new CRUDProductVarResponse(10, productId, Size.M, 99.9, 15, false, null,
                        LocalDateTime.of(2025, 5, 24, 10, 0, 0),
                        LocalDateTime.of(2025, 5, 24, 10, 0, 0))
        );

        GetProductVariantFromProductIdResponse mockResponse =
                new GetProductVariantFromProductIdResponse(productId, variants);

        Mockito.when(adminService.getAllProductVariantFromProduct(productId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/variants/{id}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.responseList").isArray())
                .andExpect(jsonPath("$.body.responseList.length()").value(1))
                .andExpect(jsonPath("$.body.responseList[0].varId").value(10))
                .andExpect(jsonPath("$.body.responseList[0].size").value("M"))
                .andExpect(jsonPath("$.body.responseList[0].price").value(99.9));
    }

    @Test
    void testGetAllVoucher_SuccessWithVouchers() throws Exception {
        int userId = 1;

        List<GetVoucherResponse> vouchers = List.of(
                new GetVoucherResponse(1, userId, 101, "ACTIVE"),
                new GetVoucherResponse(2, userId, 102, "USED")
        );

        ListAllVoucherUserIdResponse mockResponse = new ListAllVoucherUserIdResponse(2, vouchers);

        Mockito.when(userVoucherService.listAllVoucherUserId(userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-voucher/{userId}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.getVoucherResponseList").isArray())
                .andExpect(jsonPath("$.getVoucherResponseList.length()").value(2));
    }


    @Test
    void testGetAllVoucher_SuccessButNoVouchers() throws Exception {
        int userId = 2;

        ListAllVoucherUserIdResponse mockResponse = new ListAllVoucherUserIdResponse(0, List.of());

        Mockito.when(userVoucherService.listAllVoucherUserId(userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-voucher/{userId}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.getVoucherResponseList").isArray())
                .andExpect(jsonPath("$.getVoucherResponseList.length()").value(0));
    }


    @Test
    void testGetAllVoucher_UserNotFound() throws Exception {
        int invalidUserId = 99;

        Mockito.when(userVoucherService.listAllVoucherUserId(invalidUserId))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user"));

        mockMvc.perform(get("/api/admin/list-voucher/{userId}", invalidUserId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProduct_EN_WithTranslation() throws Exception {
        int productId = 1;
        Language language = Language.EN;

        CRUDProductResponse mockResponse = new CRUDProductResponse(
                productId,
                10, // cateId
                "Translated Product Name",
                List.of(new ProductImageResponse(1, "http://img.com/1.jpg")),
                "Translated Description",
                false,
                null,
                LocalDateTime.of(2025, 5, 23, 3, 5, 23),
                LocalDateTime.of(2025, 5, 24, 3, 5, 23),
                List.of()
        );

        Mockito.when(adminService.getOneProduct(productId, language))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));


        mockMvc.perform(get("/api/admin/product/view/{id}", productId).param("language", "EN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.proName").value("Translated Product Name"))
                .andExpect(jsonPath("$.body.productImageResponseList[0].id").value(1))
                .andExpect(jsonPath("$.body.productImageResponseList[0].linkImage").value("http://img.com/1.jpg"));
    }

    @Test
    void testGetProduct_VN_DefaultLanguage() throws Exception {
        int productId = 2;
        Language language = Language.VN;

        CRUDProductResponse mockResponse = new CRUDProductResponse(
                productId,
                20,
                "Tên sản phẩm gốc",
                List.of(new ProductImageResponse(1, "http://img.com/vn.jpg")),
                "Mô tả sản phẩm gốc",
                false,
                null,
                LocalDateTime.of(2025, 1, 1, 12, 0, 0),
                LocalDateTime.of(2025, 1, 2, 12, 0, 0),
                List.of()
        );

        Mockito.when(adminService.getOneProduct(productId, language))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/view/{id}", productId).param("language", "VN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.proId").value(productId))
                .andExpect(jsonPath("$.body.proName").value("Tên sản phẩm gốc"))
                .andExpect(jsonPath("$.body.description").value("Mô tả sản phẩm gốc"));
    }

    @Test
    void testGetProduct_NotFound() throws Exception {
        int productId = 999;
        Language language = Language.EN;

        Mockito.when(adminService.getOneProduct(productId, language))
                .thenThrow(new BadRequestException("production id not exists"));

        mockMvc.perform(get("/api/admin/product/view/{id}", productId).param("language", "EN"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetProduct_WithMultipleImages() throws Exception {
        int productId = 3;
        Language language = Language.EN;

        CRUDProductResponse mockResponse = new CRUDProductResponse(
                productId,
                30,
                "Product with multiple images",
                List.of(
                        new ProductImageResponse(1, "http://img.com/1.jpg"),
                        new ProductImageResponse(2, "http://img.com/2.jpg")
                ),
                "Description here",
                false,
                null,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                List.of()
        );

        Mockito.when(adminService.getOneProduct(productId, language))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/view/{id}", productId).param("language", "EN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.productImageResponseList.length()").value(2))
                .andExpect(jsonPath("$.body.productImageResponseList[0].id").value(1))
                .andExpect(jsonPath("$.body.productImageResponseList[1].id").value(2));
    }

    @Test
    void testGetProduct_WithVariants() throws Exception {
        int productId = 4;
        Language language = Language.EN;

        LocalDateTime now = LocalDateTime.of(2025, 5, 30, 15, 49, 58);

        CRUDProductVarResponse var1 = new CRUDProductVarResponse(1, productId, Size.S, 100.0, 10, false, null, now, now);
        CRUDProductVarResponse var2 = new CRUDProductVarResponse(2, productId, Size.M, 150.0, 5, false, null, now, now);

        CRUDProductResponse mockResponse = new CRUDProductResponse(
                productId,
                40,
                "Product with variants",
                List.of(),
                "Some description",
                false,
                null,
                now,
                now,
                List.of(var1, var2)
        );

        Mockito.when(adminService.getOneProduct(productId, language))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/product/view/{id}", productId).param("language", "EN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.listProductVariants.length()").value(2))
                .andExpect(jsonPath("$.body.listProductVariants[0].size").value("S"))
                .andExpect(jsonPath("$.body.listProductVariants[1].size").value("M"));
    }

    @Test
    void testGetAllProductFromCategory_Success() throws Exception {
        int cateId = 10;
        String page = "1";
        String limit = "2";
        Language language = Language.EN;

        // Tạo dữ liệu giả cho response
        ProductImageResponse image1 = new ProductImageResponse(1, "http://image1.jpg");
        ProductImageResponse image2 = new ProductImageResponse(2, "http://image2.jpg");

        CRUDProductResponse productResponse1 = new CRUDProductResponse(
                1, cateId, "Pro 1",
                List.of(image1, image2), "Desc 1",
                false, null, LocalDateTime.now(), LocalDateTime.now(),
                List.of()
        );

        CRUDProductResponse productResponse2 = new CRUDProductResponse(
                2, cateId, "Pro 2",
                List.of(), "Desc 2",
                false, null, LocalDateTime.now(), LocalDateTime.now(),
                List.of()
        );

        GetViewProductCategoryResponse mockResponse = new GetViewProductCategoryResponse(
                1, // currentPage
                1, // totalPage
                2, // limit
                2, // total
                List.of(productResponse1, productResponse2)
        );

        // Mock service
        Mockito.when(adminService.getAllProductFromCategory(
                        eq(cateId), eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        // Gọi API và kiểm tra phản hồi
        mockMvc.perform(get("/api/admin/cate/view/{id}/product", cateId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(2))
                .andExpect(jsonPath("$.body.responseList.length()").value(2));
    }

    @Test
    void testGetAllProductFromCategory_LimitGreaterThan100() throws Exception {
        int cateId = 10;
        String page = "1";
        String limit = "200"; // lớn hơn 100
        Language language = Language.EN;

        // Response mock: hệ thống giới hạn lại limit còn 100
        GetViewProductCategoryResponse mockResponse = new GetViewProductCategoryResponse(
                1, 1, 100, 0, List.of()
        );

        Mockito.when(adminService.getAllProductFromCategory(
                        eq(cateId), eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/cate/view/{id}/product", cateId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100));
    }

    @Test
    void testGetAllProductFromCategory_EmptyProductList() throws Exception {
        int cateId = 10;
        String page = "1";
        String limit = "10";
        Language language = Language.EN;

        GetViewProductCategoryResponse mockResponse = new GetViewProductCategoryResponse(
                1, 1, 10, 0, List.of()
        );

        Mockito.when(adminService.getAllProductFromCategory(
                        eq(cateId), eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/cate/view/{id}/product", cateId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.responseList.length()").value(0))
                .andExpect(jsonPath("$.body.total").value(0));
    }

    @Test
    void testGetAllProductFromCategory_InvalidLanguageParam() throws Exception {
        int cateId = 10;
        String page = "1";
        String limit = "5";
        String language = "INVALID";

        mockMvc.perform(get("/api/admin/cate/view/{id}/product", cateId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllPosts_Success() throws Exception {
        String page = "1";
        String limit = "2";
        Language language = Language.EN;

        LocalDateTime now = LocalDateTime.of(2025, 5, 31, 1, 0, 23);

        CRUDVoucherResponse voucherResponse = new CRUDVoucherResponse(
                1, "VOUCHERKEY", 100,
                now, now.plusDays(7),
                10.0,
                Status_Voucher.ACTIVE, 1
        );

        CRUDPostAndVoucherResponse postResponse1 = new CRUDPostAndVoucherResponse(
                1,
                Type_Post.NEW, "http://banner1.jpg",
                "Description EN 1", "Title EN 1", "Short Des EN 1",
                101, false, null, now,
                voucherResponse
        );

        CRUDPostAndVoucherResponse postResponse2 = new CRUDPostAndVoucherResponse(
                2,
                Type_Post.EVENT, "http://banner2.jpg",
                "Description EN 2", "Title EN 2", "Short Des EN 2",
                102, false, null, now,
                voucherResponse
        );

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                1,
                1,
                2,
                2,
                List.of(postResponse1, postResponse2)
        );

        Mockito.when(adminService.getAllPost(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPages").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(2))
                .andExpect(jsonPath("$.body.listPosts.length()").value(2))
                .andExpect(jsonPath("$.body.listPosts[0].title").value("Title EN 1"))
                .andExpect(jsonPath("$.body.listPosts[1].title").value("Title EN 2"));
    }

    @Test
    void testGetAllPosts_EmptyList() throws Exception {
        String page = "1";
        String limit = "10";
        Language language = Language.EN;

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                1,
                0,
                10,
                0,
                Collections.emptyList()
        );

        Mockito.when(adminService.getAllPost(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPages").value(0))
                .andExpect(jsonPath("$.body.limit").value(10))
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.listPosts").isEmpty());
    }

    @Test
    void testGetAllPosts_LanguageVN() throws Exception {
        String page = "1";
        String limit = "1";
        Language language = Language.VN;

        LocalDateTime now = LocalDateTime.of(2025, 5, 31, 1, 0, 23);

        CRUDVoucherResponse voucherResponse = new CRUDVoucherResponse(
                2, "MAGIAM", 50,
                now, now.plusDays(5),
                5.0,
                Status_Voucher.ACTIVE, 2
        );

        CRUDPostAndVoucherResponse postResponse = new CRUDPostAndVoucherResponse(
                3,
                Type_Post.NEW, "http://banner3.jpg",
                "Mô tả tiếng Việt", "Tiêu đề tiếng Việt", "Mô tả ngắn",
                103, false, null, now,
                voucherResponse
        );

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                1,
                1,
                1,
                1,
                List.of(postResponse)
        );

        Mockito.when(adminService.getAllPost(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPages").value(1))
                .andExpect(jsonPath("$.body.limit").value(1))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.listPosts.length()").value(1))
                .andExpect(jsonPath("$.body.listPosts[0].title").value("Tiêu đề tiếng Việt"));
    }

    @Test
    void testGetAllPosts_PageGreaterThanTotalPages() throws Exception {
        String page = "5";
        String limit = "2";
        Language language = Language.EN;

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                5,
                3,
                2,
                6,
                Collections.emptyList()
        );

        Mockito.when(adminService.getAllPost(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(5))
                .andExpect(jsonPath("$.body.totalPages").value(3))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(6))
                .andExpect(jsonPath("$.body.listPosts").isEmpty());
    }

    @Test
    void testGetAllPostsByType_Success() throws Exception {
        String page = "1";
        String limit = "2";
        Type_Post type = Type_Post.NEW;
        Language language = Language.EN;

        CRUDVoucherResponse voucherResponse = new CRUDVoucherResponse(
                1, "VOUCHERKEY", 100,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                10.0, Status_Voucher.ACTIVE, 1
        );

        CRUDPostAndVoucherResponse post1 = new CRUDPostAndVoucherResponse(
                1, type, "http://banner.jpg",
                "Description EN", "Title EN", "Short Des EN",
                100, false, null, LocalDateTime.now(),
                voucherResponse
        );

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                1, 1, 2, 1,
                List.of(post1)
        );

        Mockito.when(adminService.getAllPostByType(eq(page), eq(limit), eq(type), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/type/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("type", type.name())
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPages").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.listPosts.length()").value(1))
                .andExpect(jsonPath("$.body.listPosts[0].title").value("Title EN"));
    }

    @Test
    void testGetAllPostsByType_LimitGreaterThan100() throws Exception {
        String page = "1";
        String limit = "150";
        Type_Post type = Type_Post.EVENT;
        Language language = Language.EN;

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                1, 1, 100, 0, Collections.emptyList()
        );

        Mockito.when(adminService.getAllPostByType(eq(page), eq(limit), eq(type), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/type/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("type", type.name())
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100))
                .andExpect(jsonPath("$.body.listPosts").isEmpty());
    }

    @Test
    void testGetAllPostsByType_PageGreaterThanTotalPages() throws Exception {
        String page = "5";
        String limit = "2";
        Type_Post type = Type_Post.EVENT;
        Language language = Language.VN;

        ListAllPostResponse mockResponse = new ListAllPostResponse(
                5, 3, 2, 6, Collections.emptyList()
        );

        Mockito.when(adminService.getAllPostByType(eq(page), eq(limit), eq(type), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/post/view/type/all")
                        .param("page", page)
                        .param("limit", limit)
                        .param("type", type.name())
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(5))
                .andExpect(jsonPath("$.body.totalPages").value(3))
                .andExpect(jsonPath("$.body.listPosts").isEmpty());
    }

    @Test
    void testGetAllPostsByType_MissingTypeParam() throws Exception {
        mockMvc.perform(get("/api/admin/post/view/type/all")
                        .param("page", "1")
                        .param("limit", "2")
                        .param("language", "EN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListAllCategory_Success() throws Exception {
        String page = "1";
        String limit = "2";
        Language language = Language.EN;

        CRUDCategoryResponse category = new CRUDCategoryResponse(
                1, "Drinks", "http://image.jpg", false,
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        ListCategoryResponse mockResponse = new ListCategoryResponse(
                1, 1, 2, 1, List.of(category)
        );

        Mockito.when(adminService.listCategory(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-category")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(1))
                .andExpect(jsonPath("$.body.categoryResponseList[0].cateName").value("Drinks"));
    }

    @Test
    void testListAllCategory_LanguageVN() throws Exception {
        String page = "1";
        String limit = "2";
        Language language = Language.VN;

        CRUDCategoryResponse category = new CRUDCategoryResponse(
                1, "Đồ uống", "http://image.jpg", false,
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        ListCategoryResponse mockResponse = new ListCategoryResponse(
                1, 1, 2, 1, List.of(category)
        );

        Mockito.when(adminService.listCategory(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-category")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(1))
                .andExpect(jsonPath("$.body.categoryResponseList[0].cateName").value("Đồ uống"));
    }

    @Test
    void testListAllCategory_EmptyList() throws Exception {
        String page = "1";
        String limit = "5";
        Language language = Language.EN;

        ListCategoryResponse mockResponse = new ListCategoryResponse(
                1, 1, 5, 0, List.of()
        );

        Mockito.when(adminService.listCategory(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-category")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(0));
    }

    @Test
    void testListAllCategory_MissingParams() throws Exception {
        mockMvc.perform(get("/api/admin/list-category"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListAllCategory_LimitGreaterThan100() throws Exception {
        String page = "1";
        String limit = "150";
        Language language = Language.EN;

        CRUDCategoryResponse category = new CRUDCategoryResponse(
                1, "Drinks", "http://image.jpg", false,
                LocalDateTime.now(), LocalDateTime.now(), null
        );

        ListCategoryResponse mockResponse = new ListCategoryResponse(
                1, 1, 100, 1, List.of(category)  // limit trả về là 100, đã bị giới hạn
        );

        Mockito.when(adminService.listCategory(eq(page), eq(limit), eq(language)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/list-category")
                        .param("page", page)
                        .param("limit", limit)
                        .param("language", language.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100))
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(1))
                .andExpect(jsonPath("$.body.categoryResponseList[0].cateName").value("Drinks"));
    }

    @Test
    void testActivateShipment_Success() throws Exception {
        AdminActivateShipmentReq request = new AdminActivateShipmentReq();
        request.setShipmentId(1);
        request.setStatus(Status_Shipment.SUCCESS);

        CRUDShipmentResponse responseBody = new CRUDShipmentResponse(
                1, "Shipper A", LocalDateTime.of(2025, 5, 31, 1, 45, 43), null, null,
                LocalDateTime.of(2025, 5, 31, 1, 45, 43), null, false, Status_Shipment.SUCCESS, "Note",
                10, 2, "Customer A", 5, "123 Street", "0987654321", "cus@example.com", 999
        );

        Mockito.when(shipmentService.activate_Admin(eq(1), eq(Status_Shipment.SUCCESS)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/shipment/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.nameShipper").value("Shipper A")) // ✅ sửa ở đây
                .andExpect(jsonPath("$.customerName").value("Customer A"))
                .andExpect(jsonPath("$.email").value("cus@example.com"));
    }

    @Test
    void testActivateShipment_ShipmentNotFound() throws Exception {
        AdminActivateShipmentReq request = new AdminActivateShipmentReq();
        request.setShipmentId(999); // không tồn tại
        request.setStatus(Status_Shipment.CANCELLED);

        Mockito.when(shipmentService.activate_Admin(eq(999), eq(Status_Shipment.CANCELLED)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Shipment Not Found"));

        mockMvc.perform(post("/api/admin/shipment/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("Shipment Not Found"));
    }

    @Test
    void testActivateShipment_Cancelled() throws Exception {
        AdminActivateShipmentReq request = new AdminActivateShipmentReq();
        request.setShipmentId(2);
        request.setStatus(Status_Shipment.CANCELLED);

        CRUDShipmentResponse responseBody = new CRUDShipmentResponse(
                2, "Shipper Cancel", LocalDateTime.of(2025, 5, 30, 12, 0), null, null,
                null, LocalDateTime.of(2025, 5, 30, 13, 0), false, Status_Shipment.CANCELLED, "Canceled manually",
                20, 4, "Customer Cancel", 6, "456 Cancel Street", "0123456789", "cancel@example.com", 888
        );

        Mockito.when(shipmentService.activate_Admin(eq(2), eq(Status_Shipment.CANCELLED)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/shipment/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(2))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.nameShipper").value("Shipper Cancel"))
                .andExpect(jsonPath("$.customerName").value("Customer Cancel"))
                .andExpect(jsonPath("$.email").value("cancel@example.com"));
    }

    @Test
    void testActivateShipment_Success_Cash_AddCoin() throws Exception {
        AdminActivateShipmentReq request = new AdminActivateShipmentReq();
        request.setShipmentId(3);
        request.setStatus(Status_Shipment.SUCCESS);

        CRUDShipmentResponse responseBody = new CRUDShipmentResponse(
                3, "Shipper Coin", LocalDateTime.now(), null, null,
                LocalDateTime.now(), null, false, Status_Shipment.SUCCESS, "Delivered & Cash",
                30, 3, "Customer Coin", 7, "Coin Street", "0112233445", "coin@example.com", 777
        );

        Mockito.when(shipmentService.activate_Admin(eq(3), eq(Status_Shipment.SUCCESS)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/shipment/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(3))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.nameShipper").value("Shipper Coin"))
                .andExpect(jsonPath("$.customerName").value("Customer Coin"));
    }

    @Test
    void testListPaymentRefund_Success() throws Exception {
        String page = "1";
        String limit = "2";

        CRUDPaymentResponse paymentResponse = new CRUDPaymentResponse(
                1,
                100.0,
                LocalDateTime.of(2025, 5, 30, 10, 0),
                null,
                LocalDateTime.of(2025, 5, 31, 10, 0),
                false,
                Payment_Method.CREDIT,
                Status_Payment.REFUND,
                123,
                true,
                "http://link.example"
        );

        ListAllPaymentResponse responseBody = new ListAllPaymentResponse(
                1,
                1,
                2,
                1,
                List.of(paymentResponse)
        );

        Mockito.when(paymentService.listAllPaymentRefund(eq(page), eq(limit)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/list-payment-refund")
                        .param("page", page)
                        .param("limit", limit))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPage").value(1))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listPayments.length()").value(1))
                .andExpect(jsonPath("$.listPayments[0].paymentId").value(1))
                .andExpect(jsonPath("$.listPayments[0].amount").value(100.0))
                .andExpect(jsonPath("$.listPayments[0].orderId").value(123))
                .andExpect(jsonPath("$.listPayments[0].statusPayment").value("REFUND"));
    }

    @Test
    void testListPaymentRefund_LimitExceedsMaximum() throws Exception {
        String page = "1";
        String limit = "150";

        Mockito.when(paymentService.listAllPaymentRefund(eq(page), eq(limit)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(new ListAllPaymentResponse(
                        1, 1, 100, 0, List.of()
                )));

        mockMvc.perform(get("/api/admin/list-payment-refund")
                        .param("page", page)
                        .param("limit", limit))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.listPayments").isEmpty());
    }

    @Test
    void testListPaymentRefund_EmptyList() throws Exception {
        String page = "1";
        String limit = "10";

        Mockito.when(paymentService.listAllPaymentRefund(eq(page), eq(limit)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(new ListAllPaymentResponse(
                        1, 0, 10, 0, List.of()
                )));

        mockMvc.perform(get("/api/admin/list-payment-refund")
                        .param("page", page)
                        .param("limit", limit))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPage").value(0))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.listPayments").isEmpty());
    }

    @Test
    void testActivateRefund_Success() throws Exception {
        int paymentId = 1;

        Mockito.when(paymentService.activateRefund(paymentId,""))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Refund activated"));

        mockMvc.perform(put("/api/admin/activate/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Refund activated"));
    }

    @Test
    void testActivateRefund_PaymentNotFound() throws Exception {
        int paymentId = 99;

        Mockito.when(paymentService.activateRefund(paymentId,""))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment"));

        mockMvc.perform(put("/api/admin/activate/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 99}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found payment"));
    }

    @Test
    void testListGroupPaymentRefund_Success() throws Exception {
        String page = "1";
        String limit = "2";

        CRUDPaymentGroupResponse paymentResponse = new CRUDPaymentGroupResponse(
                1, // paymentId
                500.0,
                LocalDateTime.of(2025, 5, 30, 9, 0),
                null,
                LocalDateTime.of(2025, 5, 31, 10, 0),
                false,
                Payment_Method.CREDIT,
                Status_Payment.REFUND,
                101,
                true,
                "http://refund-link.com"
        );

        ListAllPaymentGroupResponse mockResponse = new ListAllPaymentGroupResponse(
                1,
                1,
                2,
                1,
                List.of(paymentResponse)
        );

        Mockito.when(paymentGroupService.listAllGroupPaymentRefund(page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/group/list-payment-refund")
                        .param("page", page)
                        .param("limit", limit))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].paymentId").value(1))
                .andExpect(jsonPath("$.data[0].amount").value(500.0))
                .andExpect(jsonPath("$.data[0].groupOrderId").value(101))
                .andExpect(jsonPath("$.data[0].statusPayment").value("REFUND"));
    }

    @Test
    void testListGroupPaymentRefund_EmptyResult() throws Exception {
        String page = "1";
        String limit = "10";

        ListAllPaymentGroupResponse mockResponse = new ListAllPaymentGroupResponse(
                1, 0, 10, 0, Collections.emptyList()
        );

        Mockito.when(paymentGroupService.listAllGroupPaymentRefund(page, limit))
                .thenReturn((ResponseEntity) ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/admin/group/list-payment-refund")
                        .param("page", page)
                        .param("limit", limit))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testActivateGroupRefund_Success() throws Exception {
        Mockito.when(paymentGroupService.activateRefundGroup(1,""))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Group refund activated"));

        mockMvc.perform(put("/api/admin/group/activate/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Group refund activated"));
    }

    @Test
    void testActivateGroupRefund_NotFound() throws Exception {
        Mockito.when(paymentGroupService.activateRefundGroup(1,""))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found payment group"));

        mockMvc.perform(put("/api/admin/group/activate/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found payment group"));
    }

}
