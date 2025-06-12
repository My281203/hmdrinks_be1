package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.HistoryPriceController;
import com.hmdrinks.Controller.ImageController;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Response.ImgResponse;
import com.hmdrinks.Response.ListAllPriceHistoryByVarIdResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(ImageController.class)
class ImageControllerTest {
    private static final String endPointPath="/api/image";
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
    @MockBean
    private ImgService imgService;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void handleUploadProductImagesFull_ShouldReturnSuccess_WhenValidParams() throws Exception {
        Integer proId = 1;

        Mockito.when(supportFunction.validatePositiveId("proId", proId)).thenReturn(null);

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, "image data 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "file2.jpg", MediaType.IMAGE_JPEG_VALUE, "image data 2".getBytes()
        );

        ImgResponse imgResponse1 = new ImgResponse();
        imgResponse1.setUrl("url1");
        ImgResponse imgResponse2 = new ImgResponse();
        imgResponse2.setUrl("url2");

        Mockito.when(imgService.uploadImgListProduct(file1, proId)).thenReturn(imgResponse1);
        Mockito.when(imgService.uploadImgListProduct(file2, proId)).thenReturn(imgResponse2);

        mockMvc.perform(multipart("/api/image/product-image/upload")
                        .file(file1)
                        .file(file2)
                        .param("proId", String.valueOf(proId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("url1"))
                .andExpect(jsonPath("$[1].url").value("url2"));
    }

    @Test
    void handleUploadProductImagesFull_ShouldReturnBadRequest_WhenInvalidProId() throws Exception {
        Integer invalidProId = -1;

        Mockito.when(supportFunction.validatePositiveId("proId", invalidProId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid proId"));

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/image/product-image/upload")
                        .file(file1)
                        .param("proId", String.valueOf(invalidProId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid proId"));
    }

    @Test
    void handleUploadUserImage_ShouldReturnSuccess_WhenValidParams() throws Exception {
        Integer userId = 1;

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn(null);
        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(HttpServletRequest.class), Mockito.eq(userId)))
                .thenReturn(ResponseEntity.ok().build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        ImgResponse imgResponse = new ImgResponse();
        imgResponse.setUrl("http://image.url/avatar.jpg");

        Mockito.when(imgService.uploadImgUser(file, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(imgResponse));

        mockMvc.perform(multipart("/api/image/user/upload")
                        .file(file)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://image.url/avatar.jpg"));
    }

    @Test
    void handleUploadUserImage_ShouldReturnBadRequest_WhenInvalidUserId() throws Exception {
        Integer invalidUserId = -1;

        Mockito.when(supportFunction.validatePositiveId("userId", invalidUserId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid userId"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/image/user/upload")
                        .file(file)
                        .param("userId", String.valueOf(invalidUserId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void handleUploadCategoryImage_ShouldReturnSuccess_WhenValidParams() throws Exception {
        Integer cateId = 1;

        Mockito.when(supportFunction.validatePositiveId("cateId", cateId)).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file", "category.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        ImgResponse imgResponse = new ImgResponse();
        imgResponse.setUrl("http://image.url/category.jpg");

        Mockito.when(imgService.uploadImgCategory(file, cateId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(imgResponse));

        mockMvc.perform(multipart("/api/image/cate/upload")
                        .file(file)
                        .param("cateId", String.valueOf(cateId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://image.url/category.jpg"));
    }

    @Test
    void handleUploadCategoryImage_ShouldReturnBadRequest_WhenInvalidCateId() throws Exception {
        Integer invalidCateId = -1;

        Mockito.when(supportFunction.validatePositiveId("cateId", invalidCateId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid cateId"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "category.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/image/cate/upload")
                        .file(file)
                        .param("cateId", String.valueOf(invalidCateId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid cateId"));
    }

    @Test
    void handleUploadPostImage_ShouldReturnSuccess_WhenValidParams() throws Exception {
        Integer postId = 1;

        Mockito.when(supportFunction.validatePositiveId("postId", postId)).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file", "post.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        ImgResponse imgResponse = new ImgResponse();
        imgResponse.setUrl("http://image.url/post.jpg");

        Mockito.when(imgService.uploadImgPost(file, postId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(imgResponse));

        mockMvc.perform(multipart("/api/image/post/upload")
                        .file(file)
                        .param("postId", String.valueOf(postId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://image.url/post.jpg"));
    }

    @Test
    void handleUploadPostImage_ShouldReturnBadRequest_WhenInvalidPostId() throws Exception {
        Integer invalidPostId = -1;

        Mockito.when(supportFunction.validatePositiveId("postId", invalidPostId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid postId"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "post.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes()
        );

        mockMvc.perform(multipart("/api/image/post/upload")
                        .file(file)
                        .param("postId", String.valueOf(invalidPostId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid postId"));
    }
}
