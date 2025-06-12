package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CategoryController;
import com.hmdrinks.Controller.UserController;
import com.hmdrinks.Entity.Category;
import com.hmdrinks.Entity.Product;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static java.nio.file.Paths.get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {
    private static final String endPointPath="/api/cate";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @MockBean
    private  ElasticsearchSyncService elasticsearchSyncService;
    @MockBean
    private ProductRepository productRepository;
    @Autowired
    private ObjectMapper objectMapper;

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
    private TokenRepository tokenRepository;
    @MockBean
    private ReviewService reviewService;

    @MockBean
    private CartItemService cartItemService;
    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;
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
    private CategoryTranslationRepository categoryTranslationRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void listCategory_Success() throws Exception {
        CRUDCategoryResponse response1 = new CRUDCategoryResponse(
                1,
                "New Category1",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CRUDCategoryResponse response2 = new CRUDCategoryResponse(
                2,
                "New Category2",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ListCategoryResponse listCategoryResponse = new ListCategoryResponse(
                1,
                2,
                1,
                2,
                Arrays.asList(response1, response2)
        );

        when(categoryService.listCategory(anyString(), anyString(),ArgumentMatchers.any(Language.class))).thenReturn(listCategoryResponse);

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-category?page=1&limit=1&language=VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryResponseList.length()").value(2))
                .andDo(print());
    }

    @Test
    void listCategory_LimitGreaterThan100_ApplyLimit100() throws Exception {
        CRUDCategoryResponse response1 = new CRUDCategoryResponse(
                1,
                "Category A",
                "image.png",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        ListCategoryResponse response = new ListCategoryResponse(
                1,
                1,
                100,
                1,
                Collections.singletonList(response1)
        );

        when(categoryService.listCategory(eq("1"), eq("200"), any(Language.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/list-category?page=1&limit=200&language=VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.categoryResponseList.length()").value(1));
    }

    @Test
    void createCategory_Success() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("New Category", "category_img.jpg",Language.VN);
        CRUDCategoryResponse response = new CRUDCategoryResponse(
                1,
                "New Category",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(categoryService.crateCategory(any(CreateCategoryRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(post(endPointPath + "/create-category")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cateName").value(req.getCateName()))
                .andDo(print());
    }

    @Test
    void createCategory_Conflict() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Existing Category", "category_img.jpg",Language.VN);
        when(categoryService.crateCategory(any(CreateCategoryRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("cateName exists"));
        String requestBody = objectMapper.writeValueAsString(req);
        mockMvc.perform(post(endPointPath + "/create-category")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().string("cateName exists"))
                .andDo(print());
    }

    @Test
    void getOneCategory_NotFound() throws Exception {
        int cateId = 1;
        when(categoryService.getOneCategory(anyInt(),ArgumentMatchers.any(Language.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/cate/view/{id}?&language=VN", 1)) // Sử dụng HTTP GET thay vì POST
                .andExpect(status().isNotFound())
                .andExpect(content().string("category not found"))
                .andDo(print());
    }

    @Test
    void getOneCategory_Success() throws Exception {
        int cateId = 1;
        CRUDCategoryResponse response = new CRUDCategoryResponse(
                1,
                "New Category",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(categoryService.getOneCategory(anyInt(),ArgumentMatchers.any(Language.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cate/view/{id}?language=VN", 1)) // Sử dụng HTTP GET thay vì POST
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cateName").value("New Category"))
                .andDo(print());
    }

    @Test
    void updateCategory_Success() throws Exception {
        CRUDCategoryRequest request = new CRUDCategoryRequest(
                1,
                "Yahana",
                "",
                Language.VN
        );
        CRUDCategoryResponse response = new CRUDCategoryResponse(
                1,
                "New Category1",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(categoryService.updateCategory(any(CRUDCategoryRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cateName").value(response.getCateName()))
                .andDo(print());
    }

    @Test
    void updateCategory_NotFound() throws Exception {
        CRUDCategoryRequest request = new CRUDCategoryRequest(
                1,
                "Yahana",
                "",
                Language.VN
        );
        when(categoryService.updateCategory(any(CRUDCategoryRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found"));
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().string("category not found"))
                .andDo(print());
    }

    @Test
    void updateCategory_Conflict() throws Exception {
        CRUDCategoryRequest request = new CRUDCategoryRequest(
                1,
                "Yahana",
                "",
                Language.VN
        );
        when(categoryService.updateCategory(any(CRUDCategoryRequest.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.CONFLICT).body("category already exists"));
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().string("category already exists"))
                .andDo(print());
    }

    @Test
    void getAllProductFromCategory_Success() throws Exception {
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        CRUDProductResponse response1 = new CRUDProductResponse(
                1,
                1,
                "Product1",
                productImageResponses,
                "",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                new ArrayList<>()

        );
        CRUDProductResponse response2 = new CRUDProductResponse(
                2,
                1,
                "Product2",
                productImageResponses,
                "",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                new ArrayList<>()

        );
        GetViewProductCategoryResponse listCategoryResponse = new GetViewProductCategoryResponse(
                1,
                2,
                1,
                2,
                Arrays.asList(response1, response2)
        );

        when(categoryService.getAllProductFromCategory(anyInt(),anyString(), anyString(), ArgumentMatchers.any(Language.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(listCategoryResponse));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/view/1/product?page=1&limit=1&language=VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseList.length()").value(2))
                .andExpect(jsonPath("$.responseList[0].proId").value(1))
                .andExpect(jsonPath("$.responseList[0].proName").value("Product1"))
                .andDo(print());

    }

    @Test
    void getAllProductFromCategory_Notfound() throws Exception {
        when(categoryService.getAllProductFromCategory(anyInt(),anyString(), anyString(),ArgumentMatchers.any(Language.class))).thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found"));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/view/1/product?page=1&limit=1&language=VN"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("category not found"))
                .andDo(print());
    }

    @Test
    void TotalSearchCategory_Success() throws Exception {
        CRUDCategoryResponse response1 = new CRUDCategoryResponse(
                1,
                "New Category1",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CRUDCategoryResponse response2 = new CRUDCategoryResponse(
                2,
                "New Category2",
                "category_img.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        TotalSearchCategoryResponse listCategoryResponse = new TotalSearchCategoryResponse(
                1,
                2,
                1,
                2,
                Arrays.asList(response1, response2)
        );

        when(categoryService.totalSearchCategory(anyString(),anyInt(),anyInt(), ArgumentMatchers.any(Language.class))).thenReturn((ResponseEntity)ResponseEntity.ok(listCategoryResponse));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/search?keyword=a&page=1&limit=1&language=VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(2)) // Kiểm tra độ dài danh sách categoryResponseList
                .andExpect(jsonPath("$.body.categoryResponseList[0].cateId").value(1)) // Kiểm tra cateId của phần tử đầu tiên
                .andExpect(jsonPath("$.body.categoryResponseList[1].cateName").value("New Category2")) // Kiểm tra proName của phần tử thứ hai
                .andDo(print());
    }

    @Test
    void TotalSearchCategory_LimitGreaterThan100_ApplyLimit100() throws Exception {
        TotalSearchCategoryResponse response = new TotalSearchCategoryResponse(
                1,
                1,
                100,
                1,
                List.of(new CRUDCategoryResponse(1, "Sample", "img.jpg", false,
                        LocalDateTime.now(), LocalDateTime.now(), null))
        );

        when(categoryService.totalSearchCategory(eq("abc"), eq(1), eq(200), any(Language.class))).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/search?keyword=abc&page=1&limit=200&language=VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100))
                .andExpect(jsonPath("$.body.categoryResponseList.length()").value(1));
    }

    @Test
    void enableCategory_ValidRequest_ReturnsOk() throws Exception {
        CRUDCategoryResponse response = new CRUDCategoryResponse(
                1,
                "Sample Category",
                "image.jpg",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(categoryService.enableCategory(eq(1))).thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cateId").value(1))
                .andExpect(jsonPath("$.cateName").value("Sample Category"))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    void enableCategory_CategoryNotFound_ReturnsNotFound() throws Exception {
        when(categoryService.enableCategory(eq(999))).thenReturn( (ResponseEntity)
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found")
        );

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void enableCategory_AlreadyEnabled_ReturnsBadRequest() throws Exception {
        when(categoryService.enableCategory(eq(2))).thenReturn( (ResponseEntity)
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category already enabled")
        );

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Category already enabled"));
    }

    @Test
    void disableCategory_ValidRequest_ReturnsOk() throws Exception {
        CRUDCategoryResponse response = new CRUDCategoryResponse(
                1,
                "Sample Category",
                "image.jpg",
                true,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(categoryService.disableCategory(eq(1))).thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cateId").value(1))
                .andExpect(jsonPath("$.cateName").value("Sample Category"))
                .andExpect(jsonPath("$.isDeleted").value(true));
    }

    @Test
    void disableCategory_CategoryNotFound_ReturnsNotFound() throws Exception {
        when(categoryService.disableCategory(eq(999))).thenReturn((ResponseEntity)
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found")
        );

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void disableCategory_AlreadyDisabled_ReturnsBadRequest() throws Exception {
        when(categoryService.disableCategory(eq(2))).thenReturn((ResponseEntity)
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category already disabled")
        );

        mockMvc.perform(MockMvcRequestBuilders.put(endPointPath + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Category already disabled"));
    }

}
