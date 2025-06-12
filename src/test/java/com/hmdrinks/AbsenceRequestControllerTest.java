package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.AbsenceRequestController;
import com.hmdrinks.Controller.UserVoucherController;
import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Entity.User;
import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Status_UserVoucher;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewAbsence;
import com.hmdrinks.Request.GetVoucherReq;
import com.hmdrinks.Request.UpdateAbsenceReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(AbsenceRequestController.class)
class AbsenceRequestControllerTest {
    private static final String endPointPath = "/api/absence-request";
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
    private CartItemService cartItemService;
    @MockBean
    private AbsenceRequestRepository absenceRequestRepository;
    @MockBean
    private UserService userService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private VNPayIpnHandler vnPayIpnHandler;
    @MockBean
    private AbsenceRequestService absenceRequestService;
    @MockBean
    private ProductService productService;
    @MockBean
    private PaymentService paymentService;
    @MockBean
    private ProductVarService productVarService;
    @MockBean
    private SupportFunction supportFunction;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserInfoService myUserDetailsService;
    @MockBean
    private ElasticsearchSyncService elasticsearchSyncService;
    @MockBean
    private TokenRepository tokenRepository;
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
    @MockBean
    private ZaloPayService zaloPayService;
    @MockBean
    private PaymentGroupService paymentGroupService;
    @MockBean
    private ProductTranslationRepository productTranslationRepository;
    @MockBean
    private PaymentGroupRepository paymentGroupRepository;
    @MockBean
    private PaymentRepository paymentRepository;
    @MockBean
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createAbsence_Success() throws Exception {
        ResponseEntity<?> mockAuthResponse = ResponseEntity.ok().build();
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn((ResponseEntity) mockAuthResponse);
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(1);

        CreateNewAbsence req = new CreateNewAbsence();
        req.setUserId(1);
        req.setReason("Nghỉ phép");
        req.setStartDate(start.atStartOfDay());
        req.setEndDate(end.atStartOfDay());

        CRUDAbsenceRequestResponse responseBody = new CRUDAbsenceRequestResponse();
        responseBody.setUserId(1);
        responseBody.setRequestId(100);
        responseBody.setReason("Nghỉ phép");
        responseBody.setStatus(LeaveStatus.WAITING);
        responseBody.setStartDate(start);
        responseBody.setEndDate(end);

        ResponseEntity<?> createdResponse =
                ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.createRequestAbsence(any(CreateNewAbsence.class)))
                .thenReturn((ResponseEntity) createdResponse);

        mockMvc.perform(post("/api/absence-request/create-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.requestId").value(100))
                .andExpect(jsonPath("$.reason").value("Nghỉ phép"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andDo(print());
    }
    @Test
    void createAbsence_UserNotFound() throws Exception {
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(absenceRequestService.createRequestAbsence(any(CreateNewAbsence.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user"));

        CreateNewAbsence req = new CreateNewAbsence();
        req.setUserId(999);
        req.setReason("Lý do");
        req.setStartDate(LocalDate.now().plusDays(2).atStartOfDay());
        req.setEndDate(LocalDate.now().plusDays(3).atStartOfDay());

        mockMvc.perform(post("/api/absence-request/create-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }
    @Test
    void createAbsence_NotShipper() throws Exception {
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(absenceRequestService.createRequestAbsence(any(CreateNewAbsence.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized"));

        CreateNewAbsence req = new CreateNewAbsence();
        req.setUserId(2);
        req.setReason("Lý do");
        req.setStartDate(LocalDate.now().plusDays(2).atStartOfDay());
        req.setEndDate(LocalDate.now().plusDays(3).atStartOfDay());

        mockMvc.perform(post("/api/absence-request/create-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Not authorized"))
                .andDo(print());
    }

    @Test
    void createAbsence_StartDateBeforeToday() throws Exception {
        when(supportFunction.checkUserAuthorization(any(HttpServletRequest.class), anyInt()))
                .thenReturn(ResponseEntity.ok().build());

        when(absenceRequestService.createRequestAbsence(any(CreateNewAbsence.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Ngày bắt đầu không được nhỏ hơn ngày hiện tại."));

        CreateNewAbsence req = new CreateNewAbsence();
        req.setUserId(1);
        req.setReason("Lý do");
        req.setStartDate(LocalDate.now().minusDays(1).atStartOfDay());
        req.setEndDate(LocalDate.now().plusDays(2).atStartOfDay());

        mockMvc.perform(post("/api/absence-request/create-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ngày bắt đầu không được nhỏ hơn ngày hiện tại."))
                .andDo(print());
    }

    @Test
    void getOneAbsence_Success() throws Exception {
        int userId = 1;
        int requestId = 100;
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);

        CRUDAbsenceRequestResponse responseBody = new CRUDAbsenceRequestResponse(
                userId,
                requestId,
                "Nghỉ bệnh",
                LeaveStatus.APPROVED,
                start,
                end
        );

        ResponseEntity<?> mockResponse =
                ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getOneAbsence(eq(requestId), eq(userId)))
                .thenReturn((ResponseEntity) mockResponse);

        mockMvc.perform(get("/api/absence-request/view/{requestId}", requestId)
                        .param("userId", String.valueOf(userId))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.reason").value("Nghỉ bệnh"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andDo(print());
    }
    @Test
    void getOneAbsence_UserNotFound() throws Exception {
        int userId = 999;
        int requestId = 100;

        when(absenceRequestService.getOneAbsence(eq(requestId), eq(userId)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user"));

        mockMvc.perform(get("/api/absence-request/view/{requestId}", requestId)
                        .param("userId", String.valueOf(userId))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }
    @Test
    void getOneAbsence_RequestNotFound() throws Exception {
        int userId = 1;
        int requestId = 999;

        when(absenceRequestService.getOneAbsence(eq(requestId), eq(userId)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found request"));

        mockMvc.perform(get("/api/absence-request/view/{requestId}", requestId)
                        .param("userId", String.valueOf(userId))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found request"))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserId_Success() throws Exception {
        int userId = 1;
        String page = "1";
        String limit = "10";

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                userId, 100, "Nghỉ phép", LeaveStatus.WAITING,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestByUserIdResponse responseBody = new ListAllAbsenceRequestByUserIdResponse(
                userId, 1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsenceByUserId(eq(userId), eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listAbsence[0].requestId").value(100))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserId_UserNotFound() throws Exception {
        int userId = 99;
        String page = "1";
        String limit = "10";

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");

        when(absenceRequestService.getListAbsenceByUserId(eq(userId), eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserId_LimitGreaterThan100() throws Exception {
        int userId = 1;
        String page = "1";
        String limit = "150"; // > 100, sẽ bị giới hạn lại

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                userId, 101, "Nghỉ phép dài", LeaveStatus.APPROVED,
                LocalDate.now().minusDays(1), LocalDate.now());

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestByUserIdResponse responseBody = new ListAllAbsenceRequestByUserIdResponse(
                userId, 1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsenceByUserId(eq(userId), eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserIdAndStatus_Success() throws Exception {
        int userId = 1;
        String page = "1";
        String limit = "10";
        LeaveStatus status = LeaveStatus.APPROVED;

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                userId, 100, "Nghỉ lễ", status,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestByUserIdResponse responseBody = new ListAllAbsenceRequestByUserIdResponse(
                userId, 1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsenceByUserIdAndStatus(eq(userId), eq(page), eq(limit), eq(status)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/status/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listAbsence[0].status").value(status.name()))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserIdAndStatus_UserNotFound() throws Exception {
        int userId = 999;
        String page = "1";
        String limit = "10";
        LeaveStatus status = LeaveStatus.REJECTED;

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");

        when(absenceRequestService.getListAbsenceByUserIdAndStatus(eq(userId), eq(page), eq(limit), eq(status)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/status/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found user"))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByUserIdAndStatus_InvalidEnumStatus() throws Exception {
        int userId = 1;
        String page = "1";
        String limit = "10";
        String invalidStatus = "INVALID_ENUM";

        mockMvc.perform(get("/api/absence-request/view/status/{userId}", userId)
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", invalidStatus)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isBadRequest()) // Spring không parse được enum
                .andDo(print());
    }
    @Test
    void getAllAbsenceByStatus_Success() throws Exception {
        String page = "1";
        String limit = "10";
        LeaveStatus status = LeaveStatus.APPROVED;

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                1, 200, "Nghỉ phép", status,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestResponse responseBody = new ListAllAbsenceRequestResponse(
                1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsenceByStatus(eq(status), eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all/listByStatus")
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listAbsence[0].requestId").value(200))
                .andDo(print());
    }
    @Test
    void getAllAbsenceByStatus_InvalidStatusEnum() throws Exception {
        String page = "1";
        String limit = "10";
        String invalidStatus = "INVALID_STATUS";

        mockMvc.perform(get("/api/absence-request/view/all/listByStatus")
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", invalidStatus)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isBadRequest()) // Spring không parse được enum
                .andDo(print());
    }
    @Test
    void getAllAbsenceByStatus_LimitGreaterThan100() throws Exception {
        String page = "1";
        String limit = "150";
        LeaveStatus status = LeaveStatus.WAITING;

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                2, 999, "Nghỉ bệnh", status,
                LocalDate.now(), LocalDate.now().plusDays(1));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestResponse responseBody = new ListAllAbsenceRequestResponse(
                1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsenceByStatus(eq(status), eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all/listByStatus")
                        .param("page", page)
                        .param("limit", limit)
                        .param("status", status.name())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andDo(print());
    }
    @Test
    void getAllAbsence_Success() throws Exception {
        String page = "1";
        String limit = "10";

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                1, 123, "Nghỉ lễ", LeaveStatus.APPROVED,
                LocalDate.now(), LocalDate.now().plusDays(1));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestResponse responseBody = new ListAllAbsenceRequestResponse(
                1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsence(eq(page), eq(limit)))
                .thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listAbsence[0].requestId").value(123))
                .andDo(print());
    }
    @Test
    void getAllAbsence_LimitGreaterThan100() throws Exception {
        String page = "1";
        String limit = "150";

        CRUDAbsenceRequestResponse response1 = new CRUDAbsenceRequestResponse(
                2, 555, "Nghỉ phép", LeaveStatus.REJECTED,
                LocalDate.now(), LocalDate.now().plusDays(2));

        List<CRUDAbsenceRequestResponse> responseList = List.of(response1);

        ListAllAbsenceRequestResponse responseBody = new ListAllAbsenceRequestResponse(
                1, responseList);

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.getListAbsence(eq(page), eq(limit)))
                .thenReturn((ResponseEntity)responseEntity);

        mockMvc.perform(get("/api/absence-request/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.listAbsence[0].requestId").value(555))
                .andDo(print());
    }
    @Test
    void updateAbsence_Success() throws Exception {
        int requestId = 123;
        LeaveStatus status = LeaveStatus.APPROVED;

        UpdateAbsenceReq updateReq = new UpdateAbsenceReq(requestId, status);

        AbsenceRequest mockRequest = new AbsenceRequest();
        mockRequest.setRequestId(requestId);
        mockRequest.setStatus(LeaveStatus.WAITING);
        mockRequest.setReason("Nghỉ phép");
        mockRequest.setStartDate(LocalDate.now().plusDays(2).atStartOfDay());
        mockRequest.setEndDate(LocalDate.now().plusDays(3).atStartOfDay());

        User user = new User();
        user.setUserId(1);
        mockRequest.setUser(user);

        CRUDAbsenceRequestResponse responseBody = new CRUDAbsenceRequestResponse(
                1, requestId, "Nghỉ phép", status,
                mockRequest.getStartDate().toLocalDate(), mockRequest.getEndDate().toLocalDate());

        ResponseEntity<?> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        when(absenceRequestService.updateStatusAbsence(any(UpdateAbsenceReq.class))).thenReturn((ResponseEntity) responseEntity);

        mockMvc.perform(put("/api/absence-request/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.status").value(status.name()))
                .andDo(print());
    }
    @Test
    void updateAbsence_RequestNotFound() throws Exception {
        UpdateAbsenceReq updateReq = new UpdateAbsenceReq(999, LeaveStatus.APPROVED);

        when(absenceRequestService.updateStatusAbsence(any(UpdateAbsenceReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found request"));

        mockMvc.perform(put("/api/absence-request/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not found request"))
                .andDo(print());
    }
    @Test
    void updateAbsence_TooLateToApprove() throws Exception {
        UpdateAbsenceReq updateReq = new UpdateAbsenceReq(456, LeaveStatus.APPROVED);

        when(absenceRequestService.updateStatusAbsence(any(UpdateAbsenceReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Chỉ được phép duyệt đơn trước ngày bắt đầu nghỉ."));

        mockMvc.perform(put("/api/absence-request/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Chỉ được phép duyệt đơn trước ngày bắt đầu nghỉ."))
                .andDo(print());
    }
    @Test
    void checkTimeAbsence_NoRequestsToReject() throws Exception {
        when(absenceRequestService.checkTimeAbsence())
                .thenReturn((ResponseEntity) ResponseEntity.ok("Đã tự động từ chối 0 đơn nghỉ phép quá hạn."));

        mockMvc.perform(get("/api/absence-request/check-time")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("Đã tự động từ chối 0 đơn nghỉ phép quá hạn."))
                .andDo(print());
    }
    @Test
    void checkTimeAbsence_OneRequestRejected() throws Exception {
        when(absenceRequestService.checkTimeAbsence())
                .thenReturn((ResponseEntity) ResponseEntity.ok("Đã tự động từ chối 1 đơn nghỉ phép quá hạn."));

        mockMvc.perform(get("/api/absence-request/check-time")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("Đã tự động từ chối 1 đơn nghỉ phép quá hạn."))
                .andDo(print());
    }
}
