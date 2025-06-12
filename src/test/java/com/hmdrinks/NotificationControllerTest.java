package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.ImageController;
import com.hmdrinks.Controller.NotificationController;
import com.hmdrinks.Entity.Notification;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.NotificationRequest;
import com.hmdrinks.Response.CRUDNotificationResponse;
import com.hmdrinks.Response.ImgResponse;
import com.hmdrinks.Response.ListNotificationResponse;
import com.hmdrinks.Response.NotificationResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {
    private static final String endPointPath="/api/notifications";
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
    private UserVoucherService userVoucherService;
    @MockBean
    private VoucherService voucherService;
    @MockBean
    private PriceHistoryService priceHistoryService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private NotificationRepository notificationRepository;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void sendNotification_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1);
        request.setShipmentId(100);
        request.setMessage("Test notification");

        NotificationResponse response = new NotificationResponse(1, 100, "Test notification", LocalDateTime.now(), false);

        Mockito.when(notificationService.sendNotification(1, 100, "Test notification")).thenReturn(response);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": 1,
                                "shipmentId": 100,
                                "message": "Test notification"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.shipmentId").value(100))
                .andExpect(jsonPath("$.message").value("Test notification"))
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void sendNotification_ShouldReturnUnsupportedMediaType_WhenNotJson() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid raw text"))
                .andExpect(status().isUnsupportedMediaType());
    }


    @Test
    void sendNotificationJoinGroup_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1);
        request.setGroupOrderId(200);
        request.setMessage("Join group notification");

        NotificationResponse response = new NotificationResponse(1, 200, "Join group notification", LocalDateTime.now(), false);

        Mockito.when(notificationService.sendGroupJoinNotification(1, 200, "Join group notification")).thenReturn(response);

        mockMvc.perform(post("/api/notifications/send-join-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": 1,
                                "groupOrderId": 200,
                                "message": "Join group notification"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.shipmentId").value(200))
                .andExpect(jsonPath("$.message").value("Join group notification"))
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void sendNotificationJoinGroup_ShouldReturnBadRequest_WhenLeaderIdIsNull() throws Exception {
        Mockito.when(notificationService.sendGroupJoinNotification(null, 200, "Join group notification"))
                .thenThrow(new IllegalArgumentException("Leader ID không được null"));

        mockMvc.perform(post("/api/notifications/send-join-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "userId": null,
                                "groupOrderId": 200,
                                "message": "Join group notification"
                            }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendNotificationMemberLeaveGroup_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        NotificationResponse response = new NotificationResponse(1, 200, "Member left group", LocalDateTime.now(), false);

        Mockito.when(notificationService.sendMemberGroupLeaveNotification(1, 200, "Member left group"))
                .thenReturn(response);

        mockMvc.perform(post("/api/notifications/send-member-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "userId": 1,
                            "groupOrderId": 200,
                            "message": "Member left group"
                        }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.shipmentId").value(200))
                .andExpect(jsonPath("$.message").value("Member left group"))
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void sendNotificationMemberLeaveGroup_ShouldReturnUnsupportedMediaType_WhenRequestIsNotJson() throws Exception {
        mockMvc.perform(post("/api/notifications/send-member-leave")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("userId=1&groupOrderId=200&message=NotJsonFormat"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getNotifications_ShouldReturnSuccess_WhenUserIdIsValid() throws Exception {
        List<CRUDNotificationResponse> notifications = List.of(
                new CRUDNotificationResponse(1, 1, 100, "Test message", LocalDateTime.now(), false)
        );
        ListNotificationResponse response = new ListNotificationResponse(1, notifications);

        Mockito.when(notificationService.getNotificationsByUser(1))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get("/api/notifications/user/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.total").value(1))
                .andExpect(jsonPath("$.body.notifications[0].userId").value(1))
                .andExpect(jsonPath("$.body.notifications[0].message").value("Test message"));
    }
    @Test
    void getNotifications_ShouldReturnEmptyList_WhenUserHasNoNotifications() throws Exception {
        ListNotificationResponse emptyResponse = new ListNotificationResponse(0, List.of());

        Mockito.when(notificationService.getNotificationsByUser(999))
                .thenReturn((ResponseEntity)ResponseEntity.ok(emptyResponse));

        mockMvc.perform(get("/api/notifications/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.total").value(0))
                .andExpect(jsonPath("$.body.notifications").isEmpty());
    }


    @Test
    void markNotificationAsRead_ShouldReturnSuccess_WhenNotificationExists() throws Exception {
        Notification notification = new Notification();
        notification.setId(1);
        notification.setIsRead(false);

        Mockito.when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        Mockito.when(notificationRepository.save(notification)).thenReturn(notification);

        mockMvc.perform(put("/api/notifications/read/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Thông báo đã được đánh dấu là đã đọc."));
    }


    @Test
    void markNotificationAsRead_ShouldReturnBadRequest_WhenNotificationIdIsInvalid() throws Exception {
        mockMvc.perform(put("/api/notifications/read/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void markAllNotificationsAsRead_ShouldReturnSuccess_WhenUserIdIsValid() throws Exception {
        Mockito.doNothing().when(notificationRepository).markAllNotificationsAsRead(1);

        mockMvc.perform(put("/api/notifications/read/all/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Tất cả thông báo đã được đánh dấu là đã đọc."));
    }

    @Test
    void markAllNotificationsAsRead_ShouldReturnBadRequest_WhenUserIdIsInvalid() throws Exception {
        mockMvc.perform(put("/api/notifications/read/all/abc"))
                .andExpect(status().isBadRequest());
    }

}
