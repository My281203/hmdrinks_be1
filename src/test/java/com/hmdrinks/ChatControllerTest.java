package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.AuthenticationController;
import com.hmdrinks.Controller.ChatController;
import com.hmdrinks.Entity.Message;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CRUDCategoryRequest;
import com.hmdrinks.Request.LoginBasicReq;
import com.hmdrinks.Request.SendMessageRequest;
import com.hmdrinks.Request.UserCreateReq;
import com.hmdrinks.Response.AuthenticationResponse;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(controllers = ChatController.class)
class ChatControllerTest {
    private static final String endPointPath = "/api/chat";
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
    private AuthenticationService authenticationService;

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
    private ChatService chatService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    @Test
    void sendMessage_ValidRequest_ReturnsMessage() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(1);
        request.setReceiverId(2);
        request.setShipmentId(123);
        request.setMessage("Hello");
        request.setMessageType("TEXT");
        request.setAttachments(List.of("file1.jpg", "file2.png"));

        Message message = Message.builder()
                .senderId("1")
                .receiverId("2")
                .shipmentId("123")
                .conversationId("conv-abc")
                .message("Hello")
                .messageType("TEXT")
                .attachments(List.of("file1.jpg", "file2.png"))
                .read(false)
                .createdAt(Instant.now().toEpochMilli())
                .updatedAt(null)
                ._destroy(false)
                .build();

        when(chatService.sendMessage(any(SendMessageRequest.class))).thenReturn(message);

        mockMvc.perform(MockMvcRequestBuilders.post(endPointPath + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value("1"))
                .andExpect(jsonPath("$.receiverId").value("2"))
                .andExpect(jsonPath("$.message").value("Hello"))
                .andExpect(jsonPath("$.messageType").value("TEXT"))
                .andExpect(jsonPath("$.attachments[0]").value("file1.jpg"));
    }

    @Test
    void sendMessage_InternalError_ThrowsRuntimeException() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(1);
        request.setReceiverId(2);
        request.setShipmentId(123);
        request.setMessage("Hello");
        request.setMessageType("TEXT");

        when(chatService.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(ServletException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.post(endPointPath + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        );
    }

    @Test
    void getMessagesByShipmentId_ValidShipmentId_ReturnsMessages() throws Exception {
        String shipmentId = "123";

        Message message = Message.builder()
                .senderId("1")
                .receiverId("2")
                .shipmentId(shipmentId)
                .conversationId("conv-1")
                .message("Test message")
                .messageType("TEXT")
                .attachments(List.of())
                .read(false)
                .createdAt(Instant.now().toEpochMilli())
                .updatedAt(null)
                ._destroy(false)
                .build();

        when(chatService.getMessagesByShipmentId(shipmentId))
                .thenReturn(List.of(message));

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/messages/shipment/" + shipmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderId").value("1"))
                .andExpect(jsonPath("$[0].message").value("Test message"));
    }

    @Test
    void getMessagesByShipmentId_NoMessages_ReturnsEmptyList() throws Exception {
        String shipmentId = "999";

        when(chatService.getMessagesByShipmentId(shipmentId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/messages/shipment/" + shipmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMessagesByShipmentId_InternalError_ThrowsException() throws Exception {
        String shipmentId = "999";

        when(chatService.getMessagesByShipmentId(shipmentId))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(ServletException.class, () ->
                mockMvc.perform(MockMvcRequestBuilders.get(endPointPath + "/messages/shipment/" + shipmentId)
                        .contentType(MediaType.APPLICATION_JSON))
        );
    }

}
