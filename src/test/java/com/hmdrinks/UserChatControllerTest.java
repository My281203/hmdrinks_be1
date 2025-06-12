package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartController;
import com.hmdrinks.Controller.UserChatController;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(UserChatController.class)
class UserChatControllerTest {
    private static final String endPointPath="/api/user-chat";
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
    private UserChatRepository userChatRepository;
    @MockBean
    private UserChatService userChatService;
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
    void createChat_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse); // Mock cho hàm kiểm tra quyền

        IdReq req = new IdReq();
        req.setId(1);

        String token = "Bearer test_token"; // Cung cấp token nếu cần thiết
        CRUDChatHistoryResponse response = new CRUDChatHistoryResponse(
                1,
                1,
                "789648ehdj",
                "Test",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Mock lại service trả về ResponseEntity chứa CRUDChatHistoryResponse
        when(userChatService.createNewChat(any(Integer.class), anyString()))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", token)
                        .content(requestBody))
                .andExpect(jsonPath("$.statusCodeValue").value(200))
                .andExpect(jsonPath("$.body.userId").value(1))
                .andExpect(jsonPath("$.body.chatName").value("Test"))// Kiểm tra mã trạng thái
                .andDo(print())  // In ra để kiểm tra
                .andReturn();  // Trả về kết quả
    }

    @Test
    void updateChat_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse); // Mock cho hàm kiểm tra quyền
        UpdateNameChatRequest req = new UpdateNameChatRequest(1,1,"Test1");
        String token = "Bearer test_token"; // Cung cấp token nếu cần thiết
        CRUDChatHistoryResponse response = new CRUDChatHistoryResponse(
                1,
                1,
                "789648ehdj",
                "Test1",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Mock lại service trả về ResponseEntity chứa CRUDChatHistoryResponse
        when(userChatService.updateNameChat(any(UpdateNameChatRequest.class), anyString()))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", token)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.chatName").value("Test1"))// Kiểm tra mã trạng thái
                .andDo(print())  // In ra để kiểm tra
                .andReturn();  // Trả về kết quả
    }


    @Test
    void deleteChat_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse); // Mock cho hàm kiểm tra quyền
        DeleteChatRequest req = new DeleteChatRequest(1,1);
        String token = "Bearer test_token"; // Cung cấp token nếu cần thiết
        CRUDChatHistoryResponse response = new CRUDChatHistoryResponse(
                1,
                1,
                "789648ehdj",
                "Test1",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Mock lại service trả về ResponseEntity chứa CRUDChatHistoryResponse
        when(userChatService.deleteChat(any(DeleteChatRequest.class), anyString()))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(response));

        String requestBody = objectMapper.writeValueAsString(req);

        mockMvc.perform(delete(endPointPath + "/delete")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", token)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.isDelete").value(true))// Kiểm tra mã trạng thái
                .andDo(print())  // In ra để kiểm tra
                .andReturn();  // Trả về kết quả
    }

    @Test
    void getListChat_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt())).thenReturn((ResponseEntity) mockAuthResponse); // Mock cho hàm kiểm tra quyền
        CRUDChatHistoryResponse response = new CRUDChatHistoryResponse(
                1,
                1,
                "789648ehdj",
                "Test1",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CRUDChatHistoryResponse response2 = new CRUDChatHistoryResponse(
                2,
                1,
                "789648ehdgf",
                "Test1",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        String token = "Bearer test_token"; // Cung cấp token nếu cần thiết
        ListAllChatHistoryResponse listAllChatHistoryResponse = new ListAllChatHistoryResponse(
                1,
                2,
                Arrays.asList(response,response2)
        );
        // Mock lại service trả về ResponseEntity chứa CRUDChatHistoryResponse
        when(userChatService.ListChatHistory(any(Integer.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.OK).body(listAllChatHistoryResponse));



        mockMvc.perform(get(endPointPath + "/list/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.userId").value(1))
                .andExpect(jsonPath("$.body.total").value(2))
                .andExpect(jsonPath("$.body.listChat[0].isDelete").value(true))// Kiểm tra mã trạng thái
                .andDo(print())  // In ra để kiểm tra
                .andReturn();  // Trả về kết quả
    }





}
