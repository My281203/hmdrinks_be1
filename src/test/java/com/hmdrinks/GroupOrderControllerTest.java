package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.CartGroupController;
import com.hmdrinks.Controller.GroupOrderController;
import com.hmdrinks.Entity.GroupOrderMember;
import com.hmdrinks.Entity.GroupOrders;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
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
import java.time.LocalTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(GroupOrderController.class)
class GroupOrderControllerTest {
    private static final String endPointPath="/api/group-order";
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
    private CartGroupService cartGroupService;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;

    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
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
    private GroupOrderService groupOrderService;
    @MockBean
    private CartItemGroupService cartItemGroupService;
    @MockBean
    private GroupOrderMembersRepository groupOrderMembersRepository;
    @MockBean
    private GroupOrdersRepository groupOrdersRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createCart_ShouldReturnCreated_WhenValidRequest() throws Exception {
        String json = """
        {
            "userId": 1,
            "name": "Test Group",
            "flexiblePayment": false,
            "datePayment": {
                "hour": 10,
                "minute": 0,
                "second": 0,
                "nano": 0
            },
            "type": "PAY_FOR_ALL",
            "typeTime": "TIME"
        }
        """;

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(1)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.createGroup(any(CreateNewGroupReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.CREATED).body("Group Created"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/group-order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().string("Group Created"));
    }

    @Test
    void createCart_ShouldReturnBadRequest_WhenDatePaymentWrongFormat() throws Exception {
        String json = """
        {
            "userId": 1,
            "name": "Test Group",
            "flexiblePayment": false,
            "datePayment": "10:00",
            "type": "PAY_FOR_ALL",
            "typeTime": "TIME"
        }
        """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/group-order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cannot invoke")));
    }

    @Test
    public void testJoinGroup_Success() throws Exception {
        JoinGroupReq req = new JoinGroupReq();
        req.setUserId(1);
        req.setCode("GROUP123");
        req.setTypePayment(TypePayment.CASH);

        // Mock authorize OK
        Mockito.when(supportFunction.checkUserAuthorization(any(), eq(1)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Mock repository trả về GroupOrders hợp lệ
        GroupOrders group = new GroupOrders();
        group.setGroupOrderId(123);
        group.setCode("GROUP123");
        group.setDatePaymentTime(LocalTime.now().plusMinutes(30));
        group.setTypeTime(Status_Type_Time_Group.TIME);
        group.setStatus(StatusGroupOrder.CREATED);
        group.setGroupOrderMembers(new ArrayList<>());
        group.setTypeGroupOrder(TypeGroupOrder.PAY_FOR_ALL);
        Mockito.when(groupOrdersRepository.findByCode("GROUP123")).thenReturn(group);

        User user = new User();
        user.setUserId(1);
        user.setFullName("Test User");
        Mockito.when(userRepository.findByUserIdAndIsDeletedFalse(1)).thenReturn(user);

        Mockito.when(groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserIdAndIsBlacklistTrue(anyInt(), anyInt()))
                .thenReturn(null);
        Mockito.when(groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserIdAndIsDeletedTrue(anyInt(), anyInt()))
                .thenReturn(null);

        mockMvc.perform(post("/api/group-order/join-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk());
    }
    @Test
    void testJoinGroup_UserIdIsInvalid() throws Exception {
        JoinGroupReq req = new JoinGroupReq();
        req.setUserId(0);
        req.setCode("GROUP123");
        req.setTypePayment(TypePayment.NONE);

        mockMvc.perform(post("/api/group-order/join-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListAllCartItem_WithValidRequest_ShouldReturn200() throws Exception {
        Integer groupId = 1;
        String token = "Bearer valid.jwt.token";
        String userId = "123";
        Language language = Language.VN;

        when(supportFunction.validatePositiveId("groupId", groupId)).thenReturn(null);
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(userId);
        when(groupOrderService.getDetailOneGroup(groupId, language, Integer.parseInt(userId)))
                .thenReturn((ResponseEntity)ResponseEntity.ok("success"));

        mockMvc.perform(get("/api/group-order/detail-group/{groupId}", groupId)
                        .param("language", language.name())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    void testListAllCartItem_InvalidGroupId_ShouldReturnBadRequest() throws Exception {
        Integer groupId = -1;
        String token = "Bearer token";
        Language language = Language.EN;

        ResponseEntity<?> badRequestResponse = ResponseEntity.badRequest().body("Invalid groupId");
        when(supportFunction.validatePositiveId("groupId", groupId)).thenReturn((ResponseEntity)badRequestResponse);

        mockMvc.perform(get("/api/group-order/detail-group/{groupId}", groupId)
                        .param("language", language.name())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid groupId"));
    }

    @Test
    void deleteGroup_ShouldReturnBadRequest_WhenGroupIdIsInvalid() throws Exception {
        int invalidGroupId = -1;
        int validLeaderId = 2;
        String url = "/api/group-order/delete/" + invalidGroupId + "/" + validLeaderId;

        Mockito.when(supportFunction.validatePositiveId("groupOrderId", invalidGroupId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid groupId"));

        mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid groupId"));
    }
    @Test
    void deleteGroup_ShouldReturnBadRequest_WhenLeaderIdIsInvalid() throws Exception {
        int validGroupId = 1;
        int invalidLeaderId = -1;
        String url = "/api/group-order/delete/" + validGroupId + "/" + invalidLeaderId;

        Mockito.when(supportFunction.validatePositiveId("leaderUserId", invalidLeaderId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid leaderId"));

        mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid leaderId"));
    }

    @Test
    void deleteGroup_ShouldReturnUnauthorized_WhenAuthFails() throws Exception {
        int validGroupId = 1;
        int validLeaderId = 2;
        String url = "/api/group-order/delete/" + validGroupId + "/" + validLeaderId;

        Mockito.when(supportFunction.validatePositiveId(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(null);
        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(), Mockito.eq(validLeaderId)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(delete(url))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void deleteMember_ShouldReturnBadRequest_WhenGroupIdIsInvalid() throws Exception {
        int invalidGroupId = -1;
        int leaderUserId = 2;
        int memberUserId = 3;
        String url = "/api/group-order/delete-member/" + invalidGroupId + "/" + leaderUserId + "/" + memberUserId;

        Mockito.when(supportFunction.validatePositiveId("groupOrderId", invalidGroupId))
                .thenReturn((ResponseEntity) ResponseEntity.badRequest().body("Invalid groupId"));

        mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid groupId"));
    }

    @Test
    void deleteMember_ShouldReturnBadRequest_WhenLeaderIdIsInvalid() throws Exception {
        int groupOrderId = 1;
        int invalidLeaderId = -1;
        int memberUserId = 3;
        String url = "/api/group-order/delete-member/" + groupOrderId + "/" + invalidLeaderId + "/" + memberUserId;

        Mockito.when(supportFunction.validatePositiveId("leaderUserId", invalidLeaderId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid leaderId"));

        mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid leaderId"));
    }

    @Test
    void deleteMember_ShouldReturnBadRequest_WhenMemberIdIsInvalid() throws Exception {
        int groupOrderId = 1;
        int leaderUserId = 2;
        int invalidMemberId = -1;
        String url = "/api/group-order/delete-member/" + groupOrderId + "/" + leaderUserId + "/" + invalidMemberId;

        Mockito.when(supportFunction.validatePositiveId("memberUserId", invalidMemberId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid memberId"));

        mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid memberId"));
    }

    @Test
    void leaveGroup_ShouldReturnOk_WhenLeaveSuccessful() throws Exception {
        int groupOrderId = 1;
        int userId = 2;
        String url = "/api/group-order/leave/" + groupOrderId + "/" + userId;

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(), Mockito.eq(userId)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(supportFunction.validatePositiveId("groupOrderId", groupOrderId))
                .thenReturn(null);

        Mockito.when(groupOrderService.leaveGroup(groupOrderId, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Successfully left the group"));

        mockMvc.perform(put(url))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully left the group"));
    }
    @Test
    void leaveGroup_ShouldReturnUnauthorized_WhenUserNotAuthorized() throws Exception {
        int groupOrderId = 1;
        int userId = 2;
        String url = "/api/group-order/leave/" + groupOrderId + "/" + userId;

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(), Mockito.eq(userId)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(put(url))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void leaveGroup_ShouldReturnBadRequest_WhenUserIdIsInvalid() throws Exception {
        int groupOrderId = 1;
        int invalidUserId = -1;
        String url = "/api/group-order/leave/" + groupOrderId + "/" + invalidUserId;

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(), Mockito.eq(invalidUserId)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(supportFunction.validatePositiveId("userId", invalidUserId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid userId"));

        mockMvc.perform(put(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void leaveGroup_ShouldReturnBadRequest_WhenGroupOrderIdIsInvalid() throws Exception {
        int invalidGroupOrderId = -1;
        int userId = 2;
        String url = "/api/group-order/leave/" + invalidGroupOrderId + "/" + userId;

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(), Mockito.eq(userId)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(supportFunction.validatePositiveId("groupOrderId", invalidGroupOrderId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid groupOrderId"));

        mockMvc.perform(put(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid groupOrderId"));
    }

    @Test
    void getIdGroup_ShouldReturnNotFound_WhenUserNotExist() throws Exception {
        int userId = 2;
        String url = "/api/group-order/get-id-group/" + userId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdGroup(userId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found"));

        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(content().string("UserId not found"));
    }

    @Test
    void getIdGroup_ShouldReturnZero_WhenNoActiveGroups() throws Exception {
        int userId = 3;
        String url = "/api/group-order/get-id-group/" + userId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdGroup(userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(0));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getIdGroup_ShouldReturnGroupOrderId_WhenUserHasActiveGroup() throws Exception {
        int userId = 4;
        int expectedGroupOrderId = 123;
        String url = "/api/group-order/get-id-group/" + userId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdGroup(userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(expectedGroupOrderId));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedGroupOrderId)));
    }

    @Test
    void getIdCartGroup_ShouldReturnBadRequest_WhenGroupStatusIsInvalid() throws Exception {
        int userId = 4;
        int groupOrderId = 12;
        String url = "/api/group-order/get-id-cart-group/" + userId + "?groupOrderId=" + groupOrderId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdCartGroup(userId, groupOrderId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status."));

        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group is not joinable at the current status."));
    }

    @Test
    void getIdCartGroup_ShouldReturnZero_WhenCartGroupIsNull() throws Exception {
        int userId = 5;
        int groupOrderId = 13;
        String url = "/api/group-order/get-id-cart-group/" + userId + "?groupOrderId=" + groupOrderId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdCartGroup(userId, groupOrderId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(0));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getIdCartGroup_ShouldReturnCartId_WhenCartGroupExists() throws Exception {
        int userId = 6;
        int groupOrderId = 14;
        int cartId = 99;
        String url = "/api/group-order/get-id-cart-group/" + userId + "?groupOrderId=" + groupOrderId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getIdCartGroup(userId, groupOrderId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(cartId));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(cartId)));
    }

    @Test
    void getActiveGroupIds_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        int invalidUserId = -1;
        String url = "/api/group-order/get-group-activate/" + invalidUserId;

        Mockito.when(supportFunction.validatePositiveId("userId", invalidUserId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Invalid userId"));

        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void getActiveGroupIds_ShouldReturnNotFound_WhenUserNotExist() throws Exception {
        int userId = 99;
        String url = "/api/group-order/get-group-activate/" + userId;

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getActiveGroupIds(userId))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found"));

        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(content().string("UserId not found"));
    }

    @Test
    void getActiveGroupIds_ShouldReturnListGroup_WhenSuccess() throws Exception {
        int userId = 10;
        String url = "/api/group-order/get-group-activate/" + userId;

        CRUDGroupOrderMemberResponse member1 = new CRUDGroupOrderMemberResponse();
        CRUDGroupOrderMemberResponse member2 = new CRUDGroupOrderMemberResponse();

        List<CRUDGroupOrderMemberResponse> list = Arrays.asList(member1, member2);
        ListAllGroupActiveResponse response = new ListAllGroupActiveResponse();
        response.setUserId(userId);
        response.setTotal(list.size());
        response.setList(list);

        Mockito.when(supportFunction.validatePositiveId("userId", userId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getActiveGroupIds(userId))
                .thenReturn((ResponseEntity) ResponseEntity.ok(response));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void changeTypeGroup_ShouldReturnUnauthorized_WhenAuthorizationFails() throws Exception {
        ChangeTypeGroupReq request = new ChangeTypeGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypeGroupOrder(TypeGroupOrder.PAY_FOR_ALL);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(put("/api/group-order/change-type-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void changeTypeGroup_ShouldReturnBadRequest_WhenGroupTimePassed() throws Exception {
        ChangeTypeGroupReq request = new ChangeTypeGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypeGroupOrder(TypeGroupOrder.SPLIT_BILL_WITH_ALL);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.changeTypeGroup(
                        request.getTypeGroupOrder(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity) ResponseEntity.badRequest().body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(put("/api/group-order/change-type-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

    @Test
    void changeTypeGroup_ShouldReturnOk_WhenSuccess() throws Exception {
        ChangeTypeGroupReq request = new ChangeTypeGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypeGroupOrder(TypeGroupOrder.SPLIT_BILL_WITH_ALL);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.changeTypeGroup(
                        request.getTypeGroupOrder(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Update success"));

        mockMvc.perform(put("/api/group-order/change-type-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Update success"));
    }

    @Test
    void updateTypePaymentGroupMain_ShouldReturnUnauthorized_WhenAuthFails() throws Exception {
        UpdateTypePaymentGroupReq request = new UpdateTypePaymentGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypePayment(TypePayment.CASH);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(put("/api/group-order/update-type-payment-main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void updateTypePaymentGroupMain_ShouldReturnBadRequest_WhenGroupTimeout() throws Exception {
        UpdateTypePaymentGroupReq request = new UpdateTypePaymentGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypePayment(TypePayment.MOMO);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateTypePaymentGroupMain(
                        request.getTypePayment(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity)ResponseEntity
                        .badRequest()
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));

        mockMvc.perform(put("/api/group-order/update-type-payment-main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác."));
    }

    @Test
    void updateTypePaymentGroupMain_ShouldReturnOk_WhenSuccess() throws Exception {
        UpdateTypePaymentGroupReq request = new UpdateTypePaymentGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypePayment(TypePayment.VNPAY);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateTypePaymentGroupMain(
                        request.getTypePayment(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Group payment type updated successfully"));

        mockMvc.perform(put("/api/group-order/update-type-payment-main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Group payment type updated successfully"));
    }

    @Test
    void updateTypePaymentGroupMember_ShouldReturnNotFound_WhenGroupNotExist() throws Exception {
        UpdateTypePaymentGroupReq request = new UpdateTypePaymentGroupReq();
        request.setGroupId(10);
        request.setLeaderId(2);
        request.setTypePayment(TypePayment.MOMO);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateTypePaymentGroupMember(
                        request.getTypePayment(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found"));

        mockMvc.perform(put("/api/group-order/update-type-payment-member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
    }

    @Test
    void updateTypePaymentGroupMember_ShouldReturnOk_WhenSuccess() throws Exception {
        UpdateTypePaymentGroupReq request = new UpdateTypePaymentGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setTypePayment(TypePayment.ZALO);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateTypePaymentGroupMember(
                        request.getTypePayment(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity) ResponseEntity.ok("Member payment type updated successfully"));

        mockMvc.perform(put("/api/group-order/update-type-payment-member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Member payment type updated successfully"));
    }


    @Test
    void updateAddress_ShouldReturnBadRequest_WhenAddressInvalid() throws Exception {
        UpdateAddressGroupReq request = new UpdateAddressGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setAddress("sai định dạng");

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateAddress(
                        request.getAddress(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Địa chỉ không đúng định dạng. Định dạng yêu cầu: 'số nhà, phường/xã , huyện/thị xã/thành phố, tỉnh/thành phố'"));

        mockMvc.perform(put("/api/group-order/update-address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Địa chỉ không đúng định dạng. Định dạng yêu cầu: 'số nhà, phường/xã , huyện/thị xã/thành phố, tỉnh/thành phố'"));
    }

    @Test
    void updateAddress_ShouldReturnOk_WhenSuccess() throws Exception {
        UpdateAddressGroupReq request = new UpdateAddressGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setAddress("123, Phường A, Quận B, TP.HCM");

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateAddress(
                        request.getAddress(),
                        request.getGroupId(),
                        request.getLeaderId()))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Successfully updated address"));

        mockMvc.perform(put("/api/group-order/update-address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully updated address"));
    }

    @Test
    void updateName_ShouldReturnNotFound_WhenGroupNotFound() throws Exception {
        UpdateNameGroupReq request = new UpdateNameGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setNameGroup("Nhóm A");

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateName("Nhóm A", 1, 2))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found"));

        mockMvc.perform(put("/api/group-order/update-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
    }

    @Test
    void updateName_ShouldReturnOk_WhenSuccess() throws Exception {
        UpdateNameGroupReq request = new UpdateNameGroupReq();
        request.setGroupId(1);
        request.setLeaderId(2);
        request.setNameGroup("Nhóm B");

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(2)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.updateName("Nhóm B", 1, 2))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Successfully updated name group"));

        mockMvc.perform(put("/api/group-order/update-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully updated name group"));
    }


    @Test
    void previewPayment_ShouldReturnBadRequest_WhenGroupIdIsInvalid() throws Exception {
        Integer groupId = -1;

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("groupId must be positive"));

        mockMvc.perform(get("/api/group-order/preview")
                        .param("groupId", groupId.toString())
                        .param("language", "VN")
                        .header("Authorization", "Bearer dummyToken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("groupId must be positive"));
    }

    @Test
    void previewPayment_ShouldReturnOk_WhenSuccess() throws Exception {
        Integer groupId = 1;
        Integer userId = 99;
        Language language = Language.VN;

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(jwtService.extractUserId("validToken"))
                .thenReturn(userId.toString());

        previewPaymentGroupOrderResponse mockResponse = new previewPaymentGroupOrderResponse();
        mockResponse.setGroupOrderId(groupId);
        mockResponse.setDeliveryFee(10000.0);
        mockResponse.setGroupMemberDiscount(5000.0);
        mockResponse.setSubtotal(100000.0);
        mockResponse.setTotalPrice(105000.0);
        mockResponse.setQuantity(5);
        mockResponse.setCrudCartGroupResponse(new ArrayList<>());

        Mockito.when(groupOrderService.previewPayment(groupId, language, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/preview")
                        .param("groupId", groupId.toString())
                        .param("language", language.name())
                        .header("Authorization", "Bearer validToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupOrderId").value(groupId))
                .andExpect(jsonPath("$.totalPrice").value(105000.0));
    }

    @Test
    void getLinkGroup_ShouldReturnUnauthorized_WhenUserNotInGroup() throws Exception {
        Integer groupId = 10;
        Integer userId = 88;

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(jwtService.extractUserId("invalidToken"))
                .thenReturn(userId.toString());

        Mockito.when(groupOrderService.getLinkGroup(groupId, userId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group"));

        mockMvc.perform(get("/api/group-order/get-link-group")
                        .param("groupId", groupId.toString())
                        .header("Authorization", "Bearer invalidToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Canot view Group"));
    }

    @Test
    void getLinkGroup_ShouldReturnOk_WhenSuccess() throws Exception {
        Integer groupId = 7;
        Integer userId = 101;
        String link = "https://myapp.com/group/7";

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(jwtService.extractUserId("validToken"))
                .thenReturn(userId.toString());

        Mockito.when(groupOrderService.getLinkGroup(groupId, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(link));

        mockMvc.perform(get("/api/group-order/get-link-group")
                        .param("groupId", groupId.toString())
                        .header("Authorization", "Bearer validToken"))
                .andExpect(status().isOk())
                .andExpect(content().string(link));
    }

    @Test
    void confirmPayment_ShouldReturnUnauthorized_WhenLeaderUnauthorized() throws Exception {
        Integer groupId = 1;
        Integer leaderId = 2;

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(leaderId)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

        mockMvc.perform(post("/api/group-order/confirm")
                        .param("groupId", groupId.toString())
                        .param("leaderId", leaderId.toString())
                        .param("language", "VN")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void confirmPayment_ShouldReturnCreated_WhenSuccess() throws Exception {
        Integer groupId = 3;
        Integer leaderId = 4;

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), eq(leaderId)))
                .thenReturn(ResponseEntity.ok().build());

        Mockito.when(groupOrderService.confirmPayment(groupId, leaderId, Language.VN))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.CREATED).body("Group order confirmation"));

        mockMvc.perform(post("/api/group-order/confirm")
                        .param("groupId", groupId.toString())
                        .param("leaderId", leaderId.toString())
                        .param("language", "VN")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Group order confirmation"));
    }

    @Test
    void getGroupRemainingTime_ShouldReturnUnauthorized_WhenMemberNotFound() throws Exception {
        Integer groupId = 1;
        String userIdFromToken = "5";

        Mockito.when(jwtService.extractUserId("test.jwt.token"))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getGroupRemainingTime(groupId, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group"));

        mockMvc.perform(get("/api/group-order/{groupId}/time-remaining", groupId)
                        .header("Authorization", "Bearer test.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Canot view Group"));
    }

    @Test
    void getGroupRemainingTime_ShouldReturnTime_WhenSuccess() throws Exception {
        Integer groupId = 2;
        String userIdFromToken = "10";

        Mockito.when(jwtService.extractUserId("test.jwt.token"))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getGroupRemainingTime(groupId, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.ok("00:30:00"));

        mockMvc.perform(get("/api/group-order/{groupId}/time-remaining", groupId)
                        .header("Authorization", "Bearer test.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("00:30:00"));
    }

    @Test
    void fetchAllInformation_ShouldReturnUnauthorized_WhenMemberNotFound() throws Exception {
        Integer groupId = 1;
        String jwtToken = "test.jwt.token";
        String userIdFromToken = "7";

        Mockito.when(jwtService.extractUserId(jwtToken))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.fetchAllInformationGroupOrder(groupId, Language.VN, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group because you "));

        mockMvc.perform(get("/api/group-order/fetch-all/{groupId}", groupId)
                        .param("language", "VN")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Canot view Group because you "));
    }

    @Test
    void fetchAllInformation_ShouldReturnData_WhenSuccess() throws Exception {
        Integer groupId = 2;
        String jwtToken = "another.jwt.token";
        String userIdFromToken = "10";

        fetchAllInformationGroupOrderResponse mockResponse = new fetchAllInformationGroupOrderResponse();
        mockResponse.setGroupOrderId(groupId);

        Mockito.when(jwtService.extractUserId(jwtToken))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.fetchAllInformationGroupOrder(groupId, Language.VN, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/fetch-all/{groupId}", groupId)
                        .param("language", "VN")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupOrderId").value(groupId));
    }

    @Test
    void getLinkPayment_ShouldReturnUnauthorized_WhenNotLeader() throws Exception {
        Integer groupId = 5;
        String jwtToken = "token.jwt.here";
        String userIdFromToken = "22";

        Mockito.when(jwtService.extractUserId(jwtToken))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getLinkPayment(groupId, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group"));

        mockMvc.perform(get("/api/group-order/link-payment/{groupId}", groupId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Canot view Group"));
    }

    @Test
    void getLinkPayment_ShouldReturnLink_WhenSuccess() throws Exception {
        Integer groupId = 10;
        String jwtToken = "token.jwt.ok";
        String userIdFromToken = "33";
        String link = "https://payment.example.com/link123";

        Mockito.when(jwtService.extractUserId(jwtToken))
                .thenReturn(userIdFromToken);

        Mockito.when(supportFunction.validatePositiveId("groupId", groupId))
                .thenReturn(null);

        Mockito.when(groupOrderService.getLinkPayment(groupId, Integer.parseInt(userIdFromToken)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(link));

        mockMvc.perform(get("/api/group-order/link-payment/{groupId}", groupId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().string(link));
    }

    @Test
    void checkTimeOrders_ShouldReturnSuccess_WhenValid() throws Exception {
        Mockito.when(groupOrderService.checkTimeOrders())
                .thenReturn((ResponseEntity)ResponseEntity.ok("Success"));

        mockMvc.perform(get("/api/group-order/check-time"))
                .andExpect(status().isOk())
                .andExpect(content().string("Success"));
    }

    @Test
    void checkTimeOrders_ShouldReturnServerError_WhenServiceFails() throws Exception {
        Mockito.when(groupOrderService.checkTimeOrders())
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error"));

        mockMvc.perform(get("/api/group-order/check-time"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal error"));
    }

    @Test
    void listCompletedGroupOrders_ShouldReturnSuccess_WhenValid() throws Exception {
        int userId = 1;
        int page = 1;
        int size = 10;

        ListAllGroupOrderCompletedResponse mockResponse = new ListAllGroupOrderCompletedResponse();
        mockResponse.setCurrentPage(page);
        mockResponse.setLimit(size);
        mockResponse.setTotalPages(1L);
        mockResponse.setTotal(1);
        mockResponse.setListGroup(Collections.emptyList());

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn(null);
        Mockito.when(supportFunction.validatePositiveIntegers(Map.of("page", page, "limit", size))).thenReturn(null);
        Mockito.when(groupOrderService.listHistoryGroupOrderCompleted(page, size, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/list-success/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.limit").value(size))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listGroup").isArray());
    }

    @Test
    void listCompletedGroupOrders_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        int userId = -1;
        int page = 1;
        int size = 10;

        ResponseEntity<String> badRequestResponse = ResponseEntity.badRequest().body("Invalid userId");

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn((ResponseEntity)badRequestResponse);

        mockMvc.perform(get("/api/group-order/list-success/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void listCancelledGroupOrders_ShouldReturnSuccess_WhenValid() throws Exception {
        int userId = 1;
        int page = 1;
        int size = 10;

        ListAllGroupOrderCompletedResponse mockResponse = new ListAllGroupOrderCompletedResponse();
        mockResponse.setCurrentPage(page);
        mockResponse.setLimit(size);
        mockResponse.setTotalPages(1L);
        mockResponse.setTotal(1);
        mockResponse.setListGroup(Collections.emptyList());

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn(null);
        Mockito.when(supportFunction.validatePositiveIntegers(Map.of("page", page, "limit", size))).thenReturn(null);
        Mockito.when(groupOrderService.listHistoryGroupOrderCancelled(page, size, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/list-cancelled/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.limit").value(size))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listGroup").isArray());
    }

    @Test
    void listCancelledGroupOrders_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        int userId = -1;
        int page = 1;
        int size = 10;

        ResponseEntity<String> badRequestResponse = ResponseEntity.badRequest().body("Invalid userId");

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn((ResponseEntity)badRequestResponse);

        mockMvc.perform(get("/api/group-order/list-cancelled/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void listRefundGroupOrdersByLeader_ShouldReturnSuccess_WhenValid() throws Exception {
        int userId = 1;
        int page = 1;
        int size = 10;

        ListAllGroupOrderCompletedResponse mockResponse = new ListAllGroupOrderCompletedResponse();
        mockResponse.setCurrentPage(page);
        mockResponse.setLimit(size);
        mockResponse.setTotalPages(1L);
        mockResponse.setTotal(1);
        mockResponse.setListGroup(Collections.emptyList());

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn(null);
        Mockito.when(supportFunction.validatePositiveIntegers(Map.of("page", page, "limit", size))).thenReturn(null);
        Mockito.when(groupOrderService.listAllRefundGroupOrderByLeader(page, size, userId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/list-refund/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.limit").value(size))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listGroup").isArray());
    }

    @Test
    void listRefundGroupOrdersByLeader_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        int userId = -1;
        int page = 1;
        int size = 10;

        ResponseEntity<String> badRequestResponse = ResponseEntity.badRequest().body("Invalid userId");

        Mockito.when(supportFunction.validatePositiveId("userId", userId)).thenReturn((ResponseEntity)badRequestResponse);

        mockMvc.perform(get("/api/group-order/list-refund/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid userId"));
    }

    @Test
    void listRefundGroupOrders_ShouldReturnSuccess_WhenValid() throws Exception {
        int page = 1;
        int size = 10;

        ListAllGroupOrderCompletedResponse mockResponse = new ListAllGroupOrderCompletedResponse();
        mockResponse.setCurrentPage(page);
        mockResponse.setLimit(size);
        mockResponse.setTotalPages(1L);
        mockResponse.setTotal(1);
        mockResponse.setListGroup(Collections.emptyList());

        Mockito.when(supportFunction.validatePositiveIntegers(Map.of("page", page, "limit", size))).thenReturn(null);
        Mockito.when(groupOrderService.listAllRefundGroupOrder(page, size))
                .thenReturn((ResponseEntity)ResponseEntity.ok(mockResponse));

        mockMvc.perform(get("/api/group-order/list-refund/all")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.limit").value(size))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listGroup").isArray());
    }

    @Test
    void listRefundGroupOrders_ShouldReturnBadRequest_WhenInvalidParams() throws Exception {
        int page = -1;
        int size = 0;

        ResponseEntity<String> badRequestResponse = ResponseEntity.badRequest().body("Invalid pagination parameters");

        Mockito.when(supportFunction.validatePositiveIntegers(Map.of("page", page, "limit", size))).thenReturn((ResponseEntity)badRequestResponse);

        mockMvc.perform(get("/api/group-order/list-refund/all")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid pagination parameters"));
    }

    @Test
    void activateBlackList_ShouldReturnSuccess_WhenAuthorizedAndValid() throws Exception {
        CreateBlacklistGroupReq request = new CreateBlacklistGroupReq();
        request.setGroupOrderId(1);
        request.setLeaderId(10);
        request.setUserId(20);

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(HttpServletRequest.class), eq(request.getLeaderId())))
                .thenReturn(ResponseEntity.ok().build());
        Mockito.when(groupOrderService.activateBlackList(request.getGroupOrderId(), request.getLeaderId(), request.getUserId()))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Blacklist has been activated."));

        mockMvc.perform(put("/api/group-order/activate-blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupOrderId\":1,\"leaderId\":10,\"userId\":20}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Blacklist has been activated."));
    }

    @Test
    void activateBlackList_ShouldReturnUnauthorized_WhenAuthorizationFails() throws Exception {
        CreateBlacklistGroupReq request = new CreateBlacklistGroupReq();
        request.setGroupOrderId(1);
        request.setLeaderId(10);
        request.setUserId(20);

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(HttpServletRequest.class), eq(request.getLeaderId())))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized"));

        mockMvc.perform(put("/api/group-order/activate-blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupOrderId\":1,\"leaderId\":10,\"userId\":20}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void restoreBlackList_ShouldReturnSuccess_WhenAuthorizedAndValid() throws Exception {
        CreateBlacklistGroupReq request = new CreateBlacklistGroupReq();
        request.setGroupOrderId(1);
        request.setLeaderId(10);
        request.setUserId(20);

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(HttpServletRequest.class), eq(request.getLeaderId())))
                .thenReturn(ResponseEntity.ok().build());
        Mockito.when(groupOrderService.restoreBlackList(request.getGroupOrderId(), request.getLeaderId(), request.getUserId()))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Blacklist has been restored."));

        mockMvc.perform(put("/api/group-order/restore-blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupOrderId\":1,\"leaderId\":10,\"userId\":20}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Blacklist has been restored."));
    }

    @Test
    void restoreBlackList_ShouldReturnUnauthorized_WhenAuthorizationFails() throws Exception {
        CreateBlacklistGroupReq request = new CreateBlacklistGroupReq();
        request.setGroupOrderId(1);
        request.setLeaderId(10);
        request.setUserId(20);

        Mockito.when(supportFunction.checkUserAuthorization(Mockito.any(HttpServletRequest.class), eq(request.getLeaderId())))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized"));

        mockMvc.perform(put("/api/group-order/restore-blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupOrderId\":1,\"leaderId\":10,\"userId\":20}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void getListAllBlackListFormGroupOrderId_ShouldReturnSuccess_WhenValid() throws Exception {
        Integer groupOrderId = 1;
        Language language = Language.VN;
        Integer userIdFromToken = 10;

        Mockito.when(jwtService.extractUserId("jwt-token")).thenReturn(String.valueOf(userIdFromToken));
        Mockito.when(supportFunction.validatePositiveId("groupOrderId", groupOrderId)).thenReturn(null);

        ListAllGroupOrderMemberBlackListResponse response = new ListAllGroupOrderMemberBlackListResponse();
        response.setGroupOrderId(groupOrderId);
        response.setTotal(0);
        response.setCrudGroupOrderMemberDetailResponseList(Collections.emptyList());

        Mockito.when(groupOrderService.getListAllBlackListFormGroupOrderId(groupOrderId, language, userIdFromToken))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get("/api/group-order/blacklist")
                        .param("groupOrderId", String.valueOf(groupOrderId))
                        .param("language", language.name())
                        .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupOrderId").value(groupOrderId))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getListAllBlackListFormGroupOrderId_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        Integer groupOrderId = -1;
        Language language = Language.VN;

        Mockito.when(supportFunction.validatePositiveId("groupOrderId", groupOrderId))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("groupOrderId must be positive"));

        mockMvc.perform(get("/api/group-order/blacklist")
                        .param("groupOrderId", String.valueOf(groupOrderId))
                        .param("language", language.name())
                        .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("groupOrderId must be positive"));
    }
}
