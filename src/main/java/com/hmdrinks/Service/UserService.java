package com.hmdrinks.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Sex;
import com.hmdrinks.Enum.TypeLogin;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Exception.ConflictException;
import com.hmdrinks.Exception.NotFoundException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.RepositoryElasticsearch.UserElasticsearchRepository;
import com.hmdrinks.Request.ChangePasswordReq;
import com.hmdrinks.Request.UserInfoUpdateReq;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import  org.springframework.mail.*;

import com.hmdrinks.Response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {
    private final  UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final SupportFunction supportFunction;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserElasticsearchRepository userElasticsearchRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private  PaymentRepository paymentRepository;
    @Autowired
    private  ReviewRepository reviewRepository;
    @Autowired
    private  GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private  GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private  GroupOrderService groupOrderService;


    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";


    public interface UserProjection {
        Integer getUserId();
        String getUserName();
        String getFullName();
        String getAvatar();
        LocalDateTime getBirthDate();
        String getStreet();
        String getWard();
        String getDistrict();
        String getCity();
        String getEmail();
        String getPhoneNumber();
        String getSex();
        String getType();
        Boolean getIsDeleted();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateUpdated();
        LocalDateTime getDateCreated();
        String getRole();
    }


    @Transactional()
    public ResponseEntity<?> getListAllUser(String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<UserProjection> userPage = userRepository.findAllUserProjection(pageable);

        ZoneId zoneId = ZoneId.systemDefault();

        List<DetailUserResponse> detailUserResponseList = userPage.stream().map(user -> {
            String fullLocation = String.join(", ",
                    Optional.ofNullable(user.getStreet()).orElse(""),
                    Optional.ofNullable(user.getWard()).orElse(""),
                    Optional.ofNullable(user.getDistrict()).orElse(""),
                    Optional.ofNullable(user.getCity()).orElse("")
            );



            return new DetailUserResponse(
                    user.getUserId(),
                    user.getUserName(),
                    user.getFullName(),
                    user.getAvatar(),
                    user.getBirthDate() != null ? Date.from(user.getBirthDate().atZone(zoneId).toInstant()) : null,
                    fullLocation,
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getSex(),
                    user.getType(),
                    user.getIsDeleted(),
                    user.getDateDeleted() != null ? Date.from(user.getDateDeleted().atZone(zoneId).toInstant()) : null,
                    user.getDateUpdated() != null ? Date.from(user.getDateUpdated().atZone(zoneId).toInstant()) : null,
                    user.getDateCreated() != null ? Date.from(user.getDateCreated().atZone(zoneId).toInstant()) : null,
                    user.getRole()
            );

        }).toList();

        return ResponseEntity.ok(new ListAllUserResponse(
                page,
                userPage.getTotalPages(),
                limit,
                (int) userPage.getTotalElements(),
                detailUserResponseList
        ));
    }



    @Transactional
    public ResponseEntity<?> getListAllUserByRole(String pageFromParam, String limitFromParam, Role role) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<UserProjection> userPage = userRepository.findAllByRole(role, pageable);
        long totalUsers = userRepository.countByRole(role);

        List<DetailUserResponse> detailUserResponseList = userPage.stream().map(user -> {
            String fullLocation = String.join(", ",
                    user.getStreet(),
                    user.getWard(),
                    user.getDistrict(),
                    user.getCity()
            );

            return new DetailUserResponse(
                    user.getUserId(),
                    user.getUserName(),
                    user.getFullName(),
                    user.getAvatar(),
                    user.getBirthDate() != null ? Date.from(user.getBirthDate().atZone(ZoneId.systemDefault()).toInstant()) : null,
                    fullLocation,
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getSex(),
                    user.getType(),
                    user.getIsDeleted(),
                    user.getDateDeleted() != null ? Date.from(user.getDateDeleted().atZone(ZoneId.systemDefault()).toInstant()) : null,
                    user.getDateUpdated() != null ? Date.from(user.getDateUpdated().atZone(ZoneId.systemDefault()).toInstant()) : null,
                    user.getDateCreated() != null ? Date.from(user.getDateCreated().atZone(ZoneId.systemDefault()).toInstant()) : null,
                    user.getRole()
            );
        }).toList();

        return ResponseEntity.status(HttpStatus.OK).body(
                new ListAllUserResponse(
                        page,
                        userPage.getTotalPages(),
                        limit,
                        (int) totalUsers,
                        detailUserResponseList
                )
        );
    }


    public ResponseEntity<?> getDetailUserInfoResponse(Integer id){
        User userList = userRepository.findUserByIdNative(id);
        if (userList == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        String fullLocation = Stream.of(
                        userList.getStreet(),
                        userList.getWard(),
                        userList.getDistrict(),
                        userList.getCity()
                ).filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.OK).body(new GetDetailUserInfoResponse(
                userList.getUserId(),
                userList.getUserName(),
                userList.getFullName(),
                userList.getAvatar(),
                userList.getBirthDate(),
                fullLocation,
                userList.getEmail(),
                userList.getPhoneNumber(),
                userList.getSex().toString(),
                userList.getType().toString(),
                userList.getIsDeleted(),
                userList.getDateDeleted(),
                userList.getDateUpdated(),
                userList.getDateCreated(),
                userList.getRole().toString()
        ));
    }

    public static boolean isAge18OrAbove(java.util.Date birthDate) {
        LocalDate birthLocalDate = birthDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate currentDate = LocalDate.now();

        int age = Period.between(birthLocalDate, currentDate).getYears();
        return age >= 15;
    }


    private  UserDocument convertToElasticsearch(User user) {
        return new UserDocument(
                user.getUserId(),
                user.getType(),
                user.getUserName(),
                user.getEmail(),
                user.getRole(),
                user.getSex(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getBirthDate(),
                user.getAvatar(),
                user.getCity(),
                user.getDistrict(),
                user.getWard(),
                user.getStreet(),
                user.getIsDeleted(),
                user.getDateUpdated(),
                user.getDateCreated()

        );
    }

    @Transactional
    public ResponseEntity<?> updateUserInfoResponse(UserInfoUpdateReq req) {

        if (req.getUserId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "UserId is required"));
        }

        User existingUser = userRepository.findByUserId(req.getUserId());
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            Optional<User> emailConflict = userRepository.findByEmailAndUserIdNot(req.getEmail(), req.getUserId());
            if (emailConflict.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Email already exists"));
            }
        }

        existingUser.setEmail(req.getEmail());
        existingUser.setFullName(req.getFullName());
        existingUser.setPhoneNumber(req.getPhoneNumber());
        existingUser.setAvatar(req.getAvatar());
        existingUser.setBirthDate(req.getBirthDay());

        try {
            if (req.getSex() != null) {
                existingUser.setSex(Sex.valueOf(req.getSex().toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid gender value"));
        }

        if (req.getAddress() != null) {
            String[] parts = req.getAddress().split(",");
            if (parts.length >= 4) {
                existingUser.setStreet(parts[0].trim());
                existingUser.setWard(parts[1].trim());
                existingUser.setDistrict(parts[2].trim());
                existingUser.setCity(parts[3].trim());
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid address format"));
            }
        }

        userRepository.save(existingUser);
        return ResponseEntity.ok(Map.of("message", "Update successful"));
    }



    @Transactional
    @Async
    public CompletableFuture<ResponseEntity<?>> sendEmail(String email) {
        Random random = new Random();
        User users = userRepository.findByEmail(email);
        if (users == null) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Username or email does not exist"));
        }
        OTP otpEntity = otpRepository.findByUser_Email(email);
        if (otpEntity != null) {
            otpRepository.deleteById(otpEntity.getOtpId());
        }
        int randomNumber = random.nextInt(900000) + 100000;
        String to = users.getEmail();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("HMDRINKS");
        message.setTo(to);
        String text = "Dear user,\n\n"
                + "We have received a request to reset the password for your account.\n"
                + "Please use the following One-Time Password (OTP) to proceed with the password recovery process:\n\n"
                + "Your Password Recovery OTP: " + randomNumber + "\n\n"
                + "This OTP is valid for a single use and will expire in 10 minutes for security reasons.\n"
                + "Please make sure to use the OTP within this timeframe.\n\n"
                + "For security purposes, do not share this OTP with anyone.\n"
                + "If you did not initiate this request, you can safely ignore this email.\n"
                + "Please ensure the security of your account and do not reply to this message.\n\n"
                + "Best regards,\n"
                + "Your Application Support Team";
        message.setSubject("Password Recovery OTP");
        message.setText(text);
        javaMailSender.send(message);
        LocalDateTime currentDateTime = LocalDateTime.now();
        OTP otp = new OTP();
        otp.setUser(users);
        otp.setUserName(users.getUserName());
        otp.setOtp(String.valueOf(randomNumber));
        otp.setTimeOtp(currentDateTime);
        otp.setStatus(Boolean.TRUE);
        otpRepository.save(otp);
        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.OK).body(new SendEmailResponse(otp.getUser().getEmail(), "OTP has been sent to your email.")));
    }

    @Transactional
    public CompletableFuture<ResponseEntity<?>> AcceptOTP(String email, int OTP) {
        String newPass = "Pass12345@";
        OTP otp = otpRepository.findOTP(email, String.valueOf(OTP));
        User users = userRepository.findByEmail(email);
        System.out.println(users.getUserName());
        System.out.println(users.getUserId());
        if (otp != null && OTP == Integer.parseInt(otp.getOtp())) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDateTime tenMinutesAgo = currentDateTime.minusMinutes(10);
            boolean isWithinTenMinutes = (otp.getTimeOtp().isAfter(tenMinutesAgo) && otp.getTimeOtp().isBefore(currentDateTime));
            if (isWithinTenMinutes) {
                users.setPassword(passwordEncoder.encode(newPass));
                userRepository.save(users);
                String to = users.getEmail();
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("HMDRINKS");
                message.setTo(to);
                String text = "Hello user,\n\n"
                        + "We have received a password recovery request for your account. Below is your new password:\n\n"
                        + "Username:" + users.getUserName() + "\n\n"
                        + "New Password: " + newPass + "\n\n"
                        + "Please make sure to copy the password above and paste it into the login page to avoid any errors.\n\n"
                        + "If you did not initiate this request, please disregard this email and ensure the security of your account.\n\n"
                        + "Best regards,\n"
                        + "Your Application Support Team";

                message.setSubject("Password Recovery Information for Your Account");
                message.setText(text);
                javaMailSender.send(message);
                otp.setStatus(Boolean.FALSE);
                otpRepository.save(otp);
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.OK).body(new MessageResponse("Your new password was sent to your email. Please check email.")));
            } else {
                otp.setStatus(Boolean.FALSE);
                otpRepository.save(otp);
                return  CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP existing time is expired."));
            }
        } else {
            return  CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Your username, email or OTP is not correct."));
        }
    }

    public static Page<User> mapToUserPage(Page<UserDocument> elasticsearchPage, Pageable pageable) {
        List<User> users = elasticsearchPage.getContent().stream().map(elastic -> {
            User user = new User();
            user.setUserId(elastic.getUserId());
            user.setUserName(elastic.getUserName());
            user.setEmail(elastic.getEmail());
            user.setRole(elastic.getRole());
            user.setSex(elastic.getSex());
            user.setFullName(elastic.getFullName());
            user.setPhoneNumber(elastic.getPhoneNumber());
            user.setBirthDate(elastic.getBirthDate());
            user.setAvatar(elastic.getAvatar());
            user.setCity(elastic.getCity());
            user.setDistrict(elastic.getDistrict());
            user.setWard(elastic.getWard());
            user.setStreet(elastic.getStreet());
            user.setIsDeleted(elastic.getIsDeleted());
            user.setDateUpdated(elastic.getDateUpdated());
            user.setDateCreated(elastic.getDateCreated());
            user.setType(elastic.getTypeLogin());
            return user;
        }).collect(Collectors.toList());

        return new PageImpl<>(users, pageable, elasticsearchPage.getTotalElements());
    }

    public  static List<User> mapToUserList(List<UserDocument> elasticsearchList) {
        return elasticsearchList.stream().map(elastic -> {
            User user = new User();
            user.setUserId(elastic.getUserId());
            user.setUserName(elastic.getUserName());
            user.setEmail(elastic.getEmail());
            user.setRole(elastic.getRole());
            user.setSex(elastic.getSex());
            user.setFullName(elastic.getFullName());
            user.setPhoneNumber(elastic.getPhoneNumber());
            user.setBirthDate(elastic.getBirthDate());
            user.setAvatar(elastic.getAvatar());
            user.setCity(elastic.getCity());
            user.setDistrict(elastic.getDistrict());
            user.setWard(elastic.getWard());
            user.setStreet(elastic.getStreet());
            user.setIsDeleted(elastic.getIsDeleted());
            user.setDateUpdated(elastic.getDateUpdated());
            user.setDateCreated(elastic.getDateCreated());
            user.setType(elastic.getTypeLogin());
            return user;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity<?> totalSearchUser(String keyword,String pageFromParam, String limitFromParam) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<UserDocument> userDocumentPage = userElasticsearchRepository.searchUsers(keyword,pageable);
        List<UserDocument> userDocuments = userElasticsearchRepository.searchUsers(keyword);
        Page<User> userList = mapToUserPage(userDocumentPage,pageable);

//                userRepository.findByUserNameContainingOrEmailContainingOrFullNameContainingOrStreetContainingOrDistrictContainingOrCityContainingOrPhoneNumberContaining(
//                keyword,keyword,keyword,keyword,keyword,keyword,keyword,pageable);
        List<User> userList1 = mapToUserList(userDocuments);
//                userRepository.findByUserNameContainingOrEmailContainingOrFullNameContainingOrStreetContainingOrDistrictContainingOrCityContainingOrPhoneNumberContaining(
//                keyword,keyword,keyword,keyword,keyword,keyword,keyword);
        List<DetailUserResponse> detailUserResponseList = new ArrayList<>();
        for (User user : userList) {
            System.out.println(user.getType());
            System.out.println(user.getSex());
            String fullLocation = user.getStreet() + "," + user.getWard() + ", " + user.getDistrict() + ","+ user.getCity();
            detailUserResponseList.add(new DetailUserResponse(
                    user.getUserId(),
                    user.getUserName(),
                    user.getFullName(),
                    user.getAvatar(),
                    user.getBirthDate(),
                    fullLocation,
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getSex().toString(),
                    user.getType().toString(),
                    user.getIsDeleted(),
                    user.getDateDeleted(),
                    user.getDateUpdated(),
                    user.getDateCreated(),
                    user.getRole().toString()
            ));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new TotalSearchUserResponse(page,userList.getTotalPages(),limit,userList1.size(), detailUserResponseList));
    }

    @Transactional
    public ResponseEntity<?> changePasswordResponse(ChangePasswordReq req) {
        User users = userRepository.findByUserId(req.getUserId());

        if (users == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Not found user");
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), users.getPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "The current password is incorrect");
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "You're using an old password");
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (!req.getNewPassword().matches(req.getConfirmNewPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "No overlap");
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        users.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(users);
        UserDocument userDocument = convertToElasticsearch(users);
        userElasticsearchRepository.save(userDocument);
        String message = "Password change successfully";
        return ResponseEntity.status(HttpStatus.OK).body(new ChangePasswordResponse(
                message,
                req.getNewPassword()
        ));
    }

    @Transactional
    public  ResponseEntity<?> disableAccount(int userId)
    {
        User users = userRepository.findByUserId(userId);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if(users.getRole() == Role.ADMIN)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not delete Admin");
        }
        if(users.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already disable");
        }
        if(users.getRole() == Role.CUSTOMER)
        {
            List<Review> reviews = reviewRepository.findByUser_UserId(userId);
            for(Review review : reviews)
            {
                review.setIsDeleted(true);
                review.setDateDeleted(LocalDateTime.now());
                reviewRepository.save(review);
            }
            List<Orders> orders = orderRepository.findAllByUserUserId(userId);
            for(Orders order : orders)
            {
                order.setIsDeleted(true);
                order.setDateDeleted(LocalDateTime.now());
                orderRepository.save(order);
                Payment payment = paymentRepository.findByOrderOrderId(order.getOrderId());
                if(payment != null){
                    payment.setIsDeleted(true);
                    payment.setDateDeleted(LocalDateTime.now());
                    paymentRepository.save(payment);
                    Shippment shippment = shipmentRepository.findByPaymentPaymentId(payment.getPaymentId());
                    if(shippment != null){
                        shippment.setIsDeleted(true);
                        shippment.setDateDeleted(LocalDateTime.now());
                        shipmentRepository.save(shippment);
                    }
                }

            }
            List<GroupOrders> groupOrders = groupOrdersRepository.findAllByUserUserId(userId);
            for(GroupOrders groupOrder : groupOrders)
            {
                groupOrderService.deleteGroupByLeader(groupOrder.getGroupOrderId(), userId);
            }

            List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByUserUserIdAndIsLeaderFalse(userId);
            for(GroupOrderMember groupOrderMember : groupOrderMemberList)
            {
                groupOrderService.leaveGroup(groupOrderMember.getGroupOrder().getGroupOrderId(), userId);
            }
        }


        users.setIsDeleted(true);
        users.setDateDeleted(Date.valueOf(LocalDate.now()));
        userRepository.save(users);
        UserDocument userDocument = convertToElasticsearch(users);
        userElasticsearchRepository.save(userDocument);
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDAccountUserResponse(
                users.getUserId(),
                users.getUserName(),
                users.getFullName(),
                users.getAvatar(),
                users.getBirthDate(),
                users.getStreet() + ", " + users.getWard() + ", " + users.getDistrict() + ", " + users.getCity(),
                users.getEmail(),
                users.getPhoneNumber(),
                users.getSex().toString(),
                users.getType().toString(),
                users.getIsDeleted(),
                users.getDateDeleted(),
                users.getDateUpdated(),
                users.getDateCreated(),
                users.getRole().toString()
        ));
    }

    @Transactional
    public  ResponseEntity<?>  enableAccount(int userId)
    {
        User users = userRepository.findByUserId(userId);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if(!users.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already enable");
        }
        List<Review> reviews = reviewRepository.findByUser_UserId(userId);
        for(Review review : reviews)
        {
            review.setIsDeleted(false);
            review.setDateDeleted(null);
            reviewRepository.save(review);
        }
        List<Orders> orders = orderRepository.findAllByUserUserId(userId);
        for(Orders order : orders)
        {
            order.setIsDeleted(false);
            order.setDateDeleted(null);
            orderRepository.save(order);
            Payment payment = paymentRepository.findByOrderOrderId(order.getOrderId());
            if(payment != null){
                payment.setIsDeleted(false);
                payment.setDateDeleted(null);
                paymentRepository.save(payment);
                Shippment shippment = shipmentRepository.findByPaymentPaymentId(payment.getPaymentId());
                if(shippment != null){
                    shippment.setIsDeleted(false);
                    shippment.setDateDeleted(null);
                    shipmentRepository.save(shippment);
                }
            }
        }
        users.setIsDeleted(false);
        users.setDateDeleted(null);
        userRepository.save(users);
        UserDocument userDocument = convertToElasticsearch(users);
        userElasticsearchRepository.save(userDocument);
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDAccountUserResponse(
                users.getUserId(),
                users.getUserName(),
                users.getFullName(),
                users.getAvatar(),
                users.getBirthDate(),
                users.getStreet() + ", " + users.getWard() + ", " + users.getDistrict() + ", " + users.getCity(),
                users.getEmail(),
                users.getPhoneNumber(),
                users.getSex().toString(),
                users.getType().toString(),
                users.getIsDeleted(),
                users.getDateDeleted(),
                users.getDateUpdated(),
                users.getDateCreated(),
                users.getRole().toString()
        ));
    }

    public String googleLogin(String code) {
        String accessToken = exchangeCodeForAccessToken(code);
        if (accessToken != null) {
            return getGoogleUserInfo(accessToken);}
        return null;
    }

    private String exchangeCodeForAccessToken(String code) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GOOGLE_TOKEN_URL)
                    .queryParam("code", code)
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("grant_type", "authorization_code");

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String accessToken = parseAccessToken(response.getBody());
                return accessToken;
            } else {
                throw new RuntimeException("Failed to get access token, status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String parseAccessToken(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.get("access_token").asText();
            return accessToken;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public String getGoogleUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode userInfo = objectMapper.readTree(responseBody);
                String email = userInfo.get("email").asText();
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    if(!user.getIsDeleted())
                    {
                        user = new User();
                        user.setEmail(email);
                        String givenName = userInfo.get("given_name").asText();
                        String familyName = userInfo.get("family_name").asText();
                        String fullName = givenName + " " + familyName;
                        user.setFullName(fullName);
                        user.setAvatar(userInfo.get("picture").asText());
                        user.setType(TypeLogin.EMAIL);
                        user.setUserName(email);
                        user.setPassword("");
                        user.setPhoneNumber(null);
                        user.setCity("");
                        user.setDistrict("");
                        user.setStreet("");
                        user.setWard("");
                        user.setSex(Sex.OTHER);
                        user.setRole(Role.CUSTOMER);
                        user.setIsDeleted(false);
                        user.setDateCreated(Date.valueOf(LocalDate.now()));
                        userRepository.save(user);
                    }
                } else {
                    if (user.getType() == TypeLogin.BASIC && !user.getIsDeleted()) {
                        user.setType(TypeLogin.BOTH);
                        userRepository.save(user);
                    }
                }
                return email;
            } else {
                throw new RuntimeException("Failed to get user info, status code: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            return null;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public User getUserById(Integer userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

}
