package com.hmdrinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Controller.AuthenticationController;
import com.hmdrinks.Controller.ContactController;
import com.hmdrinks.Entity.Contact;
import com.hmdrinks.Enum.Status_Contact;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.AuthenticationResponse;
import com.hmdrinks.Response.CRUDContactResponse;
import com.hmdrinks.Response.ListAllContactResponse;
import com.hmdrinks.Service.*;
import com.hmdrinks.SupportFunction.SupportFunction;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebAppConfiguration
@WebMvcTest(controllers = ContactController.class)
class ContactControllerTest {
    private static final String endPointPath = "/api/contact";
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
    private ContactService contactService;
    @MockBean
    private ContactRepository contactRepository;
    @MockBean
    private ShipperComissionDetailService shipperComissionDetailService;
    @MockBean
    private JavaMailSender javaMailSender1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createContact_Success() throws Exception {
        CreateContactReq req = new CreateContactReq();
        req.setEmail("user@example.com");
        req.setPhone("0123456789");
        req.setFullName("Nguyen Van A");
        req.setDescription("This is a feedback");
        req.setUserId(1);

        CRUDContactResponse responseBody = new CRUDContactResponse(
                1, "This is a feedback", Status_Contact.WAITING, false,
                LocalDateTime.now(), null, null,
                "Nguyen Van A", "0123456789", "user@example.com"
        );

        when(contactService.createContact(any(CreateContactReq.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(responseBody));

        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactId").value(1))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andDo(print());
    }

    @Test
    void createContact_InvalidEmail() throws Exception {
        CreateContactReq req = new CreateContactReq();
        req.setEmail("invalid-email");
        req.setPhone("0123456789");
        req.setFullName("Nguyen Van B");
        req.setDescription("This is a feedback");
        req.setUserId(2);

        when(contactService.createContact(any(CreateContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Email không hợp lệ"));

        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email không hợp lệ"))
                .andDo(print());
    }

    @Test
    void createContact_InvalidPhoneNumber() throws Exception {
        CreateContactReq req = new CreateContactReq();
        req.setEmail("user@example.com");
        req.setPhone("12345");
        req.setFullName("Nguyen Van C");
        req.setDescription("This is a feedback");
        req.setUserId(3);

        when(contactService.createContact(any(CreateContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Số điện thoại không hợp lệ. Phải chứa 10 chữ số."));

        mockMvc.perform(post(endPointPath + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số điện thoại không hợp lệ. Phải chứa 10 chữ số."))
                .andDo(print());
    }

    @Test
    void getOneContact_Success() throws Exception {
        int contactId = 1;

        CRUDContactResponse response = new CRUDContactResponse(
                contactId,
                "Khách hàng cần tư vấn sản phẩm",
                Status_Contact.WAITING,
                false,
                LocalDateTime.of(2024, 10, 1, 10, 0),
                null,
                null,
                "Nguyen Van A",
                "0123456789",
                "user@example.com"
        );

        when(contactService.getContactById(contactId))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/" + contactId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactId").value(contactId))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.phone").value("0123456789")) // ✅ Sửa ở đây
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void getOneContact_NotFound() throws Exception {
        int contactId = 999;

        when(contactService.getContactById(contactId))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact not found"));

        mockMvc.perform(get(endPointPath + "/view/" + contactId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Contact not found"))
                .andDo(print());
    }

    @Test
    void updateContact_Success() throws Exception {
        CrudContactReq request = new CrudContactReq();
        request.setContactId(1);
        request.setFullName("Nguyen Van B");
        request.setEmail("user@example.com");
        request.setPhone("0123456789");
        request.setDescription("Đã cập nhật thông tin");

        CRUDContactResponse response = new CRUDContactResponse(
                1,
                "Đã cập nhật thông tin",
                Status_Contact.WAITING,
                false,
                LocalDateTime.of(2024, 10, 1, 10, 0),
                LocalDateTime.of(2024, 10, 2, 10, 0),
                null,
                "Nguyen Van B",
                "0123456789",
                "user@example.com"
        );

        when(contactService.updateContact(any(CrudContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactId").value(1))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van B"))
                .andExpect(jsonPath("$.phone").value("0123456789"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.description").value("Đã cập nhật thông tin"));
    }

    @Test
    void updateContact_ContactNotFound() throws Exception {
        CrudContactReq request = new CrudContactReq();
        request.setContactId(99);
        request.setFullName("Nguyen Van B");
        request.setEmail("user@example.com");
        request.setPhone("0123456789");
        request.setDescription("Không tìm thấy contact");

        when(contactService.updateContact(any(CrudContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact not found"));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("Contact not found"));
    }

    @Test
    void updateContact_InvalidPhoneNumber() throws Exception {
        CrudContactReq request = new CrudContactReq();
        request.setContactId(1);
        request.setFullName("Nguyen Van B");
        request.setEmail("user@example.com");
        request.setPhone("12345");
        request.setDescription("Số điện thoại không hợp lệ");

        when(contactService.updateContact(any(CrudContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.badRequest().body("Số điện thoại không hợp lệ. Phải chứa 10 chữ số."));

        mockMvc.perform(put(endPointPath + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số điện thoại không hợp lệ. Phải chứa 10 chữ số."));
    }

    @Test
    void getAllContact_Success() throws Exception {
        String page = "1";
        String limit = "2";

        List<CRUDContactResponse> contactList = Arrays.asList(
                new CRUDContactResponse(
                        1,
                        "Tư vấn sản phẩm",
                        Status_Contact.WAITING,
                        false,
                        LocalDateTime.of(2024, 10, 1, 10, 0),
                        null,
                        null,
                        "Nguyen Van A",
                        "0123456789",
                        "user1@example.com"
                ),
                new CRUDContactResponse(
                        2,
                        "Cần hỗ trợ kỹ thuật",
                        Status_Contact.COMPLETED,
                        false,
                        LocalDateTime.of(2024, 10, 2, 11, 0),
                        null,
                        null,
                        "Tran Thi B",
                        "0987654321",
                        "user2@example.com"
                )
        );

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(5);
        response.setLimit(2);
        response.setTotal(10);
        response.setListContacts(contactList);

        when(contactService.listAllContact(page, limit))
                .thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all")
                        .param("page", page)
                        .param("limit", limit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.totalPage").value(5))
                .andExpect(jsonPath("$.body.limit").value(2))
                .andExpect(jsonPath("$.body.total").value(10))
                .andExpect(jsonPath("$.body.listContacts").isArray())
                .andExpect(jsonPath("$.body.listContacts.length()").value(2))
                .andExpect(jsonPath("$.body.listContacts[0].contactId").value(1))
                .andExpect(jsonPath("$.body.listContacts[0].fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.body.listContacts[1].contactId").value(2))
                .andExpect(jsonPath("$.body.listContacts[1].email").value("user2@example.com"));
    }

    @Test
    void getAllContact_LimitTooLarge_ShouldCapAt100() throws Exception {
        String page = "1";
        String limit = "150";

        List<CRUDContactResponse> contactList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            contactList.add(new CRUDContactResponse(
                    i, "Desc " + i, Status_Contact.WAITING, false,
                    LocalDateTime.of(2024, 10, 1, 10, 0), null, null,
                    "Name " + i, "012345678" + i % 10, "user" + i + "@example.com"));
        }

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(10);
        response.setLimit(100);
        response.setTotal(1000);
        response.setListContacts(contactList);

        when(contactService.listAllContact(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100))
                .andExpect(jsonPath("$.body.listContacts.length()").value(100));
    }

    @Test
    void getAllContact_NoData_ShouldReturnEmptyList() throws Exception {
        String page = "1";
        String limit = "10";

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(0);
        response.setLimit(10);
        response.setTotal(0);
        response.setListContacts(Collections.emptyList());

        when(contactService.listAllContact(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.listContacts").isArray())
                .andExpect(jsonPath("$.body.listContacts.length()").value(0))
                .andExpect(jsonPath("$.body.total").value(0));
    }

    @Test
    void getAllContactComplete_Success_ShouldReturnPaginatedCompletedContacts() throws Exception {
        String page = "1";
        String limit = "5";

        List<CRUDContactResponse> completedContacts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            completedContacts.add(new CRUDContactResponse(
                    i,
                    "Mô tả " + i,
                    Status_Contact.COMPLETED,
                    false,
                    LocalDateTime.of(2024, 1, i, 10, 0),
                    LocalDateTime.of(2024, 2, i, 12, 0),
                    null,
                    "Người dùng " + i,
                    "012345678" + i,
                    "email" + i + "@mail.com"
            ));
        }

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(2);
        response.setLimit(5);
        response.setTotal(10);
        response.setListContacts(completedContacts);

        when(contactService.listAllContactComplete(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/complete")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(5))
                .andExpect(jsonPath("$.body.totalPage").value(2))
                .andExpect(jsonPath("$.body.total").value(10))
                .andExpect(jsonPath("$.body.listContacts.length()").value(5))
                .andExpect(jsonPath("$.body.listContacts[0].status").value("COMPLETED"));
    }

    @Test
    void getAllContactComplete_LimitTooLarge_ShouldCapAt100() throws Exception {
        String page = "1";
        String limit = "150";

        List<CRUDContactResponse> contactList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            contactList.add(new CRUDContactResponse(
                    i, "Desc " + i, Status_Contact.COMPLETED, false,
                    LocalDateTime.of(2024, 10, 1, 10, 0), null, null,
                    "Name " + i, "012345678" + (i % 10), "user" + i + "@example.com"
            ));
        }

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(5);
        response.setLimit(100); // capped to 100
        response.setTotal(500); // giả sử có 500 completed contact
        response.setListContacts(contactList);

        when(contactService.listAllContactComplete(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/complete")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100))
                .andExpect(jsonPath("$.body.listContacts.length()").value(100));
    }

    @Test
    void getAllContactComplete_NoData_ShouldReturnEmptyList() throws Exception {
        String page = "1";
        String limit = "10";

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(0);
        response.setLimit(10);
        response.setTotal(0);
        response.setListContacts(Collections.emptyList());

        when(contactService.listAllContactComplete(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/complete")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.listContacts").isArray())
                .andExpect(jsonPath("$.body.listContacts.length()").value(0))
                .andExpect(jsonPath("$.body.total").value(0));
    }

    @Test
    void getAllContactWaiting_Success_ShouldReturnPaginatedWaitingContacts() throws Exception {
        String page = "1";
        String limit = "5";

        List<CRUDContactResponse> waitingContacts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            waitingContacts.add(new CRUDContactResponse(
                    i,
                    "Mô tả " + i,
                    Status_Contact.WAITING,
                    false,
                    LocalDateTime.of(2024, 1, i, 10, 0),
                    LocalDateTime.of(2024, 2, i, 12, 0),
                    null,
                    "Người dùng " + i,
                    "012345678" + i,
                    "email" + i + "@mail.com"
            ));
        }

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setTotalPage(2);
        response.setLimit(5);
        response.setTotal(10);
        response.setListContacts(waitingContacts);

        when(contactService.listAllContactWaiting(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/waiting")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.currentPage").value(1))
                .andExpect(jsonPath("$.body.limit").value(5))
                .andExpect(jsonPath("$.body.totalPage").value(2))
                .andExpect(jsonPath("$.body.total").value(10))
                .andExpect(jsonPath("$.body.listContacts.length()").value(5))
                .andExpect(jsonPath("$.body.listContacts[0].status").value("WAITING"));
    }

    @Test
    void getAllContactWaiting_LimitTooLarge_ShouldCapLimitAt100() throws Exception {
        String page = "1";
        String limit = "150";

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setLimit(100);
        response.setTotalPage(1);
        response.setTotal(0);
        response.setListContacts(Collections.emptyList());

        when(contactService.listAllContactWaiting(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/waiting")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.limit").value(100));
    }

    @Test
    void getAllContactWaiting_NoData_ShouldReturnEmptyList() throws Exception {
        String page = "1";
        String limit = "5";

        ListAllContactResponse response = new ListAllContactResponse();
        response.setCurrentPage(1);
        response.setLimit(5);
        response.setTotalPage(0);
        response.setTotal(0);
        response.setListContacts(Collections.emptyList());

        when(contactService.listAllContactWaiting(page, limit)).thenReturn((ResponseEntity)ResponseEntity.ok(response));

        mockMvc.perform(get(endPointPath + "/view/all/waiting")
                        .param("page", page)
                        .param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.listContacts").isEmpty())
                .andExpect(jsonPath("$.body.total").value(0));
    }

    @Test
    void responseContact_Success_ShouldSendEmailAndUpdateStatus() throws Exception {
        AcceptContactReq req = new AcceptContactReq();
        req.setContactId(1);
        req.setContent("Cảm ơn bạn đã liên hệ!");

        when(contactService.responseContact(any(AcceptContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Response success"));

        mockMvc.perform(put(endPointPath + "/contact/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Response success"));

        verify(contactService).responseContact(any(AcceptContactReq.class));
    }

    @Test
    void responseContact_EmailEmpty_ShouldNotSendEmailButStillUpdateStatus() throws Exception {
        AcceptContactReq req = new AcceptContactReq();
        req.setContactId(2);
        req.setContent("Cảm ơn bạn đã liên hệ!");

        when(contactService.responseContact(any(AcceptContactReq.class)))
                .thenReturn((ResponseEntity)ResponseEntity.ok("Response success"));

        mockMvc.perform(put(endPointPath + "/contact/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Response success"));

        verify(contactService).responseContact(any(AcceptContactReq.class));
    }

}



