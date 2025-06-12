package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewGroupReq;
import com.hmdrinks.Request.JoinGroupReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.DistanceAndDuration;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupOrderService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private CartGroupRepository cartGroupRepository;
    @Autowired
    private CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private CartGroupRepository groupRepository;
    @Autowired
    private CartItemGroupRepository groupItemRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private  ProductVariantsRepository productVariantsRepository;

    @Value("${api.user-service.url}")
    private String userServiceUrl;
    @Value("${api.group-order.url}")
    private String groupOrderUrl;


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private PaymentGroupRepository paymentGroupRepository;

    public static String generateRandomCode1() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    public String generateRandomCode() {
        Integer maxCode = groupOrdersRepository.findMaxGroupOrderCodeNumber();
        int nextCode = (maxCode != null) ? maxCode + 1 : 1;
        return String.format("GR_%03d", nextCode);  // VD: GR_001, GR_045
    }


    @Transactional
    public ResponseEntity<?> createGroup(CreateNewGroupReq req) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }
        // Check nếu user đang là trưởng nhóm trong group đang hoạt động
        List<StatusGroupOrder> excludedStatuses = Arrays.asList(StatusGroupOrder.CANCELED, StatusGroupOrder.COMPLETED);
        boolean isLeadingActiveGroup = groupOrderMembersRepository
                .isUserLeadingActiveGroup(Long.valueOf(req.getUserId()), excludedStatuses);

        if (isLeadingActiveGroup) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You are already leading an active group. Cannot create a new one.");
        }


        LocalTime paymentTime = req.getDatePayment();


            // Kiểm tra paymentTime nằm trong khoảng từ 8:00 đến 21:15
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(21, 15);

//        if (paymentTime.isBefore(startTime) || paymentTime.isAfter(endTime)) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body("Group time is only allowed from 08:00 to 21:15.");
//            }


        GroupOrders groupOrders = new GroupOrders();
        groupOrders.setUser(user);
        groupOrders.setStatus(StatusGroupOrder.CREATED);
        groupOrders.setDateCreated(LocalDateTime.now());
        groupOrders.setIsDeleted(false);
        groupOrders.setDatePaymentTime(req.getDatePayment());
        if(req.getTypeTime() == Status_Type_Time_Group.NO_TIME)
        {
            groupOrders.setDatePaymentTime(LocalTime.of(0,0));

        }

        groupOrders.setTypeTime(req.getTypeTime());
        groupOrders.setNameGroup(req.getName());
        groupOrders.setTypeGroupOrder(req.getType());
        groupOrders.setTotalPrice(0.0);
        groupOrders.setNote("");
        groupOrders.setIsFlexiblePayment(req.getFlexiblePayment());
        groupOrders.setDeadlinePayment(LocalDateTime.now());
        String randomCode = generateRandomCode1();
        String url = groupOrderUrl + "/?code=" + randomCode;
        System.out.println(url);
        groupOrders.setCode(randomCode);
        groupOrders.setTypePayment(TypePayment.NONE);
        groupOrders.setLink(url);
        groupOrders.setAddress("");
        groupOrders.setOrderDate(LocalDateTime.now());
        groupOrdersRepository.save(groupOrders);

        CRUDGroupOrderResponse crudGroupOrderResponse = new CRUDGroupOrderResponse(
                groupOrders.getGroupOrderId(),
                groupOrders.getUser().getFullName(),
                groupOrders.getAddress(),
                groupOrders.getNote(),
                groupOrders.getLink(),
                groupOrders.getCode(),
                groupOrders.getNameGroup(),
                groupOrders.getTotalPrice(),
                groupOrders.getTypeGroupOrder(),
                groupOrders.getStatus(),
                groupOrders.getIsDeleted(),
                groupOrders.getOrderDate(),
                groupOrders.getDeadlinePayment(),
                groupOrders.getDateCreated(),
                groupOrders.getDateUpdated(),
                groupOrders.getDateDeleted()

        );

        // thêm ng tạo vào group
        GroupOrderMember groupOrderMember = new GroupOrderMember();
        groupOrderMember.setUser(user);
        groupOrderMember.setGroupOrder(groupOrders);
        groupOrderMember.setDateCreated(LocalDateTime.now());
        groupOrderMember.setIsPaid(false);
        groupOrderMember.setIsDeleted(false);
        groupOrderMember.setIsLeader(true);
        groupOrderMember.setTypePayment(TypePayment.NONE);
        groupOrderMember.setNote("");
        groupOrderMember.setStatus(StatusGroupOrderMember.CREATED);
        groupOrderMembersRepository.save(groupOrderMember);

        CartGroup cartGroup = new CartGroup();
        cartGroup.setUser(user);
        cartGroup.setDateCreated(LocalDateTime.now());
        cartGroup.setIsDeleted(false);
        cartGroup.setTotalPrice(0.0);
        cartGroup.setTotalProduct(0);
        cartGroup.setGroupOrderMember(groupOrderMember);
        cartGroupRepository.save(cartGroup);
        groupOrderMember.setCartGroup(cartGroup);
        groupOrderMembersRepository.save(groupOrderMember);
        return ResponseEntity.status(HttpStatus.CREATED).body(crudGroupOrderResponse);

    }


    @Transactional
    public ResponseEntity<?> joinGroup(JoinGroupReq req) {
        CRUDGroupOrderMemberResponse crudGroupOrderMemberResponse = new CRUDGroupOrderMemberResponse();
        GroupOrders groupOrder = groupOrdersRepository.findByCode(req.getCode());
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CHECKOUT,
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }


        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }



        List<StatusGroupOrder> excludedStatuses = Arrays.asList(StatusGroupOrder.CANCELED, StatusGroupOrder.COMPLETED);
        GroupOrderMember groupOrderMember1 = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserIdAndIsBlacklistTrue(groupOrder.getGroupOrderId(),req.getUserId());
        if(groupOrderMember1 != null)
        {
            if(groupOrderMember1.getIsBlacklist())
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Canot join the group");
            }

        }
        boolean alreadyMember = groupOrder.getGroupOrderMembers().stream()
                .anyMatch(member -> member.getUser().getUserId() == req.getUserId() && !member.getIsDeleted());

        if (alreadyMember) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is already a member of the group");
        }


        long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                .filter(member -> !member.getIsDeleted())
                .count();

        if (activeMemberCount >= 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is full. Cannot join.");
        }


        GroupOrderMember existingMember = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserIdAndIsDeletedTrue(groupOrder.getGroupOrderId(), req.getUserId());
        System.out.println(existingMember);
        if (existingMember != null) {
            if(existingMember.getIsBlacklist() == Boolean.TRUE)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot join group if blacklist");
            }
            existingMember.setIsDeleted(false);
            existingMember.setDateDeleted(null);
            existingMember.setStatus(StatusGroupOrderMember.CREATED);
            existingMember.setIsPaid(false);
            groupOrderMembersRepository.save(existingMember);
            CartGroup cartGroup = existingMember.getCartGroup();

            cartGroup.setDateCreated(LocalDateTime.now());
            cartGroup.setIsDeleted(false);
            cartGroup.setTotalPrice(0.0);
            cartGroup.setTotalProduct(0);
            cartGroup.setGroupOrderMember(existingMember);
            cartGroupRepository.save(cartGroup);
            List<CartItemGroup> cartItemGroupList = cartGroup.getCartItems();
            for(CartItemGroup cartItemGroup : cartItemGroupList)
            {
                cartItemGroupRepository.delete(cartItemGroup);
            }

            existingMember.setCartGroup(cartGroup);
            groupOrderMembersRepository.save(existingMember);//
            crudGroupOrderMemberResponse = new CRUDGroupOrderMemberResponse(
                    existingMember != null ? existingMember.getMemberId() : groupOrder.getGroupOrderMembers().stream().filter(m -> m.getUser().getUserId() == req.getUserId()).findFirst().get().getMemberId(),
                    user.getFullName(),
                    groupOrder.getGroupOrderId(),
                    groupOrder.getStatus(),
                    groupOrder.getNameGroup(),
                    user.getUserId(),
                    existingMember != null ? existingMember.getAmount() : 0.0,
                    false,
                    false,
                    "",
                    StatusGroupOrderMember.CREATED,
                    TypePayment.NONE,
                    existingMember.getDateCreated(),
                    existingMember.getDateUpdated(),
                    null,
                    false
            );
        } else {

            GroupOrderMember groupOrderMember = new GroupOrderMember();
            groupOrderMember.setUser(user);
            groupOrderMember.setGroupOrder(groupOrder);
            groupOrderMember.setDateCreated(LocalDateTime.now());
            groupOrderMember.setIsPaid(false);
            groupOrderMember.setIsDeleted(false);
            groupOrderMember.setIsLeader(false);
            groupOrderMember.setTypePayment(TypePayment.NONE);
            groupOrderMember.setNote("");
            groupOrderMember.setStatus(StatusGroupOrderMember.CREATED);


            if (groupOrder.getTypeGroupOrder() == TypeGroupOrder.PAY_FOR_ALL) {
                groupOrderMember.setTypePayment(TypePayment.NONE);
            } else {
                groupOrderMember.setTypePayment(req.getTypePayment());
            }
            groupOrderMembersRepository.save(groupOrderMember);
            CartGroup cartGroup = new CartGroup();
            cartGroup.setUser(user);
            cartGroup.setDateCreated(LocalDateTime.now());
            cartGroup.setIsDeleted(false);
            cartGroup.setTotalPrice(0.0);
            cartGroup.setTotalProduct(0);
            cartGroup.setGroupOrderMember(groupOrderMember);
            cartGroupRepository.save(cartGroup);
            groupOrderMember.setCartGroup(cartGroup);
            groupOrderMembersRepository.save(groupOrderMember);
            Double amount = groupOrderMember != null && groupOrderMember.getAmount() != null
                    ? groupOrderMember.getAmount() : 0.0;

            crudGroupOrderMemberResponse = new CRUDGroupOrderMemberResponse(
                    groupOrderMember.getMemberId(),
                    user.getFullName(),
                    groupOrder.getGroupOrderId(),
                    groupOrder.getStatus(),
                    groupOrder.getNameGroup(),
                    user.getUserId(),
//                    groupOrderMember != null ? groupOrderMember.getAmount() : 0.0,
                    amount,
                    false,
                    false,
                    "",
                    StatusGroupOrderMember.CREATED,
                    TypePayment.NONE,
                    groupOrderMember.getDateCreated(),
                    groupOrderMember.getDateUpdated(),
                    null,
                    false
            );
        }

        try {
            // Lấy leader trong group (người có isLeader = true)
            GroupOrderMember leaderMember = groupOrder.getGroupOrderMembers().stream()
                    .filter(member -> member.getIsLeader() && !member.getIsDeleted())
                    .findFirst().orElse(null);

            if (leaderMember != null) {
                Integer leaderId = leaderMember.getUser().getUserId();
                String message = user.getFullName() + " đã tham gia nhóm đặt hàng";

                groupOrder.getGroupOrderMembers().stream()
                        .filter(member -> !member.getIsDeleted() && member.getUser().getUserId() != req.getUserId())
                        .forEach(member -> {
                            try {
                                Integer receiverId = member.getUser().getUserId();
                                notificationService.sendGroupJoinNotification(receiverId, groupOrder.getGroupOrderId(), message);
                                System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                            } catch (Exception ex) {
                                System.err.println("Không thể gửi thông báo tới userId=" + member.getUser().getUserId() + ": " + ex.getMessage());
                            }
                        });

            }

        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo join group: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(crudGroupOrderMemberResponse);
    }


    private String extractFirstImageUrl(String listProImg) {
        if (listProImg != null && !listProImg.trim().isEmpty()) {
            String[] imageEntries = listProImg.split(", ");
            if (imageEntries.length > 0) {
                String[] parts = imageEntries[0].split(": ");
                if (parts.length == 2) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    @Transactional
    public ResponseEntity<?> getDetailOneGroup(Integer groupOrderId, Language language,Integer userIdReq) {


        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }
        // 1. Kiểm tra group tồn tại
        GroupOrders groupOrders = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        // 2. Build thông tin group
        CRUDGroupOrderResponse crudGroupOrderResponse = new CRUDGroupOrderResponse(
                groupOrders.getGroupOrderId(),
                groupOrders.getUser().getFullName(),
                groupOrders.getAddress(),
                groupOrders.getNote(),
                groupOrders.getLink(),
                groupOrders.getCode(),
                groupOrders.getNameGroup(),
                groupOrders.getTotalPrice(),
                groupOrders.getTypeGroupOrder(),
                groupOrders.getStatus(),
                groupOrders.getIsDeleted(),
                groupOrders.getOrderDate(),
                groupOrders.getDeadlinePayment(),
                groupOrders.getDateCreated(),
                groupOrders.getDateUpdated(),
                groupOrders.getDateDeleted()
        );

        // 3. Lấy danh sách thành viên nhóm
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrderId);
        List<CRUDGroupOrderMemberDetailResponse> crudGroupOrderMemberResponseList = new ArrayList<>();

        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            if(groupOrderMember.getIsBlacklist() == Boolean.TRUE)
            {
                continue;
            }
            // Xử lý CartGroup nếu có
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null; // Mặc định là null

            if (cartGroup != null) {
                Integer quantity = 0;
                Double totalPrice = 0.0;
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartGroup.getCartId());
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        String proname = "";
                        if (language == Language.VN) {
                            proname = cartItem.getProductVariants().getProduct().getProName();
                        } else {
                            Product product = cartItem.getProductVariants().getProduct();
                            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                            proname = productTranslation.getProName();
                        }
                        quantity = quantity + cartItem.getQuantity();
                        totalPrice += cartItem.getTotalPrice();
                        cartItemResponses.add(new CRUDCartItemGroupResponse(
                                cartItem.getCartItemId(),
                                cartItem.getProductVariants().getProduct().getProId(),
                                proname,
                                cartItem.getCartGroup().getCartId(),
                                cartItem.getProductVariants().getSize(),
                                cartItem.getProductVariants().getPrice(),
                                cartItem.getTotalPrice(),
                                cartItem.getQuantity(),
                                extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg())
                        ));
                    }
                }

                crudCartGroupResponse = new CRUDCartGroupResponse(
                        cartGroup.getCartId(),
                        cartGroup.getGroupOrderMember().getGroupOrder().getGroupOrderId(),
                        cartGroup.getUser().getUserId(),
                        cartGroup.getGroupOrderMember().getMemberId(),
                        totalPrice,
                        quantity,
                        cartItemResponses
                );
            }

            // 4. Build member response
            CRUDGroupOrderMemberDetailResponse crudGroupOrderMemberResponse = new CRUDGroupOrderMemberDetailResponse(
                    groupOrderMember.getMemberId(),
                    groupOrderMember.getUser().getFullName(),
                    groupOrderMember.getGroupOrder().getGroupOrderId(),
                    groupOrderMember.getUser().getUserId(),
                    groupOrderMember.getAmount(),
                    groupOrderMember.getIsPaid(),
                    groupOrderMember.getIsLeader(),
                    groupOrderMember.getNote(),
                    groupOrderMember.getStatus(),
                    groupOrderMember.getTypePayment(),
                    groupOrderMember.getDateCreated(),
                    groupOrderMember.getDateUpdated(),
                    groupOrderMember.getDateDeleted(),
                    groupOrderMember.getIsDeleted(),
                    crudCartGroupResponse
            );
            crudGroupOrderMemberResponseList.add(crudGroupOrderMemberResponse);
        }

        // 5. Trả về
        return ResponseEntity.status(HttpStatus.OK)
                .body(new getGroupOrderResponse(
                        crudGroupOrderMemberResponseList.size(),
                        crudGroupOrderResponse,
                        crudGroupOrderMemberResponseList
                ));
    }


    @Transactional
    public ResponseEntity<?> deleteGroupByLeader(int groupOrderId, int leaderUserId) {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderUserId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderUserId);
        if (leader == null || !leader.getIsLeader()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }


        groupOrder.setIsDeleted(true);
        groupOrder.setStatus(StatusGroupOrder.CANCELED);
        groupOrder.setDateDeleted(LocalDateTime.now());


        List<GroupOrderMember> groupOrderMembers = groupOrder.getGroupOrderMembers();
        List<CartGroup> cartGroupsToUpdate = new ArrayList<>();
        List<CartItemGroup> cartItemsToUpdate = new ArrayList<>();

        for (GroupOrderMember member : groupOrderMembers) {
            member.setIsDeleted(true);
            member.setStatus(StatusGroupOrderMember.CANCELED);
            member.setDateDeleted(LocalDateTime.now());

            CartGroup cartGroup = member.getCartGroup();
            if (cartGroup != null) {
                cartGroup.setIsDeleted(true);
                cartGroup.setDateDeleted(LocalDateTime.now());
                cartGroupsToUpdate.add(cartGroup);

                List<CartItemGroup> cartItemGroups = cartGroup.getCartItems();
                if (cartItemGroups != null) {
                    for (CartItemGroup cartItemGroup : cartItemGroups) {
                        cartItemGroup.setIsDeleted(true);
                        cartItemGroup.setDateDeleted(LocalDateTime.now());
                        cartItemsToUpdate.add(cartItemGroup);
                    }
                }

                member.setAmount(0.0);
                member.setQuantity(0);
            }
        }

        // Tính toán lại tổng giá trị và số lượng
        Double totalPrice = 0.0;
        Integer totalQuantity = 0;
        for (GroupOrderMember member : groupOrderMembers) {
            totalPrice += member.getAmount();
            totalQuantity += member.getQuantity();
        }

        groupOrder.setTotalPrice(totalPrice);
        groupOrder.setTotalQuantity(totalQuantity);

        // Lưu tất cả các thay đổi trong một lần
        groupOrdersRepository.save(groupOrder);
        groupOrderMembersRepository.saveAll(groupOrderMembers);
        cartGroupRepository.saveAll(cartGroupsToUpdate);
        cartItemGroupRepository.saveAll(cartItemsToUpdate);

        return ResponseEntity.ok("Group deleted successfully");
    }


    @Transactional
    public ResponseEntity<?> deleteMemberByLeader(int groupOrderId, int leaderUserId, int memberUserId) {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        User leaderUser = userRepository.findByUserIdAndIsDeletedFalse(leaderUserId);
        if (leaderUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }

        User user_member = userRepository.findByUserIdAndIsDeletedFalse(memberUserId);
        if (user_member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderUserId);
        if (leader == null || !Boolean.TRUE.equals(leader.getIsLeader())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }

        GroupOrderMember member = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, memberUserId);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member not found");
        }

        // Đánh dấu thành viên bị xóa
        member.setIsDeleted(true);
        member.setIsDeletedLeader(true);
        member.setDateDeleted(LocalDateTime.now());
        groupOrderMembersRepository.save(member);

        // Xóa giỏ hàng và sản phẩm trong giỏ
        CartGroup cartGroup = member.getCartGroup();
        if (cartGroup != null) {
            cartGroup.setIsDeleted(true);
            cartGroup.setDateDeleted(LocalDateTime.now());
            cartGroupRepository.save(cartGroup);

            List<CartItemGroup> listCartItemGroup = cartGroup.getCartItems();
            if (listCartItemGroup != null) {
                for (CartItemGroup item : listCartItemGroup) {
                    item.setIsDeleted(true);
                    item.setDateDeleted(LocalDateTime.now());
                    cartItemGroupRepository.save(item);
                }
            }

            member.setAmount(0.0);
            member.setQuantity(0);
            groupOrderMembersRepository.save(member);
        }

        // Cập nhật tổng giá và số lượng của nhóm
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrderId);
        Double price = 0.0;
        Integer quantity = 0;

        for (GroupOrderMember m : groupOrderMemberList) {
            if (m.getAmount() != null) {
                price += m.getAmount();
            }
            if (m.getQuantity() != null) {
                quantity += m.getQuantity();
            }
        }

        groupOrder.setTotalPrice(price);
        groupOrder.setTotalQuantity(quantity);
        groupOrdersRepository.save(groupOrder);

        // Gửi thông báo tới thành viên bị xóa
        try {
            String message = "Bạn không còn là thành viên của nhóm " + groupOrder.getNameGroup();
            notificationService.sendMemberDeleteByLeaderNotification(user_member.getUserId(), groupOrderId, message);
            System.out.println("📨 Gửi thông báo tới userId=" + user_member.getUserId() + " với message=" + message);
        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo xóa thành viên: " + e.getMessage());
        }

        return ResponseEntity.ok("Member deleted successfully");
    }


    @Transactional
    public ResponseEntity<?> leaveGroup(int groupOrderId, int userId) {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        User user_member = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user_member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
        }

        GroupOrderMember member = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userId);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member not found");
        }

        // Đánh dấu thành viên đã rời nhóm
        member.setIsDeleted(true);
        member.setStatus(StatusGroupOrderMember.CANCELED);
        member.setDateDeleted(LocalDateTime.now());
        groupOrderMembersRepository.save(member);

        CartGroup cartGroup = member.getCartGroup();
        if (cartGroup != null) {
            cartGroup.setIsDeleted(true);
            cartGroup.setDateDeleted(LocalDateTime.now());
            cartGroupRepository.save(cartGroup);

            List<CartItemGroup> listCartItemGroup = cartGroup.getCartItems();
            if (listCartItemGroup != null) {
                for (CartItemGroup cartItemGroup2 : listCartItemGroup) {
                    cartItemGroup2.setIsDeleted(true);
                    cartItemGroup2.setDateDeleted(LocalDateTime.now());
                    cartItemGroupRepository.save(cartItemGroup2);
//                    cartItemGroupRepository.delete(cartItemGroup2);
                }
                member.setAmount(0.0);
                member.setQuantity(0);
                groupOrderMembersRepository.save(member);
            }
        }

        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrderId);
        Double Price = 0.0;
        Integer Quantity = 0;

        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            Double amount = groupOrderMember.getAmount();
            Integer quantity = groupOrderMember.getQuantity();

            if (amount != null) {
                Price += amount;
            }
            if (quantity != null) {
                Quantity += quantity;
            }
        }

        groupOrder.setTotalPrice(Price);
        groupOrder.setTotalQuantity(Quantity);
        groupOrdersRepository.save(groupOrder);

        // Gửi thông báo cho các thành viên còn lại
        try {
            String message = user_member.getFullName() + " đã rời nhóm đặt hàng";
            groupOrder.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted()) // chỉ gửi cho các thành viên chưa rời nhóm
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendMemberGroupLeaveNotification(receiverId, groupOrder.getGroupOrderId(), message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Không thể gửi thông báo leave group: " + e.getMessage());
        }

        return ResponseEntity.ok("Successfully left the group");
    }



    public ResponseEntity<?> getIdGroup(int userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

        // Lấy các GroupOrders mà người dùng tham gia và chưa hoàn thành/cancel
        List<GroupOrders> activeGroups = groupOrdersRepository.findActiveGroupOrdersByUserId(
                userId, StatusGroupOrder.COMPLETED, StatusGroupOrder.CANCELED);

        if (activeGroups.isEmpty()) {
            // Trả về 0 nếu không tìm thấy nhóm nào
            return ResponseEntity.ok(0);
        }


        GroupOrders latestGroup = activeGroups.stream()
                .max((g1, g2) -> g1.getDateCreated().compareTo(g2.getDateCreated()))
                .orElseThrow(() -> new RuntimeException("Error finding latest group"));

        return ResponseEntity.ok(latestGroup.getGroupOrderId());
    }


    public ResponseEntity<?> getIdCartGroup(int userId,Integer groupId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }

        GroupOrderMember groupOrderMember = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, userId);
        CartGroup cartGroup = groupOrderMember.getCartGroup();
        if (cartGroup == null) {
            return ResponseEntity.ok(0);
        }
        return ResponseEntity.ok(cartGroup.getCartId());
    }

    @Transactional
    public ResponseEntity<?> getActiveGroupIds(int userId) {

        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not found");
        }


        List<StatusGroupOrder> excludedStatuses = Arrays.asList(StatusGroupOrder.COMPLETED, StatusGroupOrder.CANCELED);
        List<GroupOrderMember> activeMemberships = groupOrderMembersRepository
                .findByUserUserIdAndGroupOrderStatusNotInAndIsDeletedFalse(userId, excludedStatuses);

        List<CRUDGroupOrderMemberResponse> crudGroupOrderMemberResponseList = new ArrayList<>();
        for(GroupOrderMember groupOrderMember : activeMemberships) {

            crudGroupOrderMemberResponseList.add(new CRUDGroupOrderMemberResponse(
                    groupOrderMember.getMemberId(),
                    groupOrderMember.getUser().getFullName(),
                    groupOrderMember.getGroupOrder().getGroupOrderId(),
                    groupOrderMember.getGroupOrder().getStatus(),
                    groupOrderMember.getGroupOrder().getNameGroup(),
                    groupOrderMember.getUser().getUserId(),
                    groupOrderMember.getAmount(),
                    groupOrderMember.getIsPaid(),
                    groupOrderMember.getIsLeader(),
                    groupOrderMember.getNote(),
                    groupOrderMember.getStatus(),
                    groupOrderMember.getTypePayment(),
                    groupOrderMember.getDateCreated(),
                    groupOrderMember.getDateUpdated(),
                    groupOrderMember.getDateDeleted(),
                    groupOrderMember.getIsDeleted()
            ));

        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllGroupActiveResponse(
                userId,
                crudGroupOrderMemberResponseList.size(),
                crudGroupOrderMemberResponseList
        ));
    }



    public ResponseEntity<?> updateName(String nameGroup, Integer groupOrderId, Integer leaderId) {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }


        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }
        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderId);
        if (leader.getIsLeader() == Boolean.FALSE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }
        groupOrder.setNameGroup(nameGroup);
        groupOrder.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrder);
        return ResponseEntity.ok("Successfully updated name group");
    }


    private boolean isValidAddress(String address) {
        String regex = "^\\s*[^,]+\\s*,\\s*[^,]+\\s*,\\s*[^,]+\\s*,\\s*[^,]+\\s*$";
        return address != null && address.matches(regex);
    }


    @Transactional
    public ResponseEntity<?> updateAddress(String address, Integer groupOrderId, Integer leaderId) {
        if (!isValidAddress(address)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Địa chỉ không đúng định dạng. Định dạng yêu cầu: 'số nhà, phường/xã , huyện/thị xã/thành phố, tỉnh/thành phố'");
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }

        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderId);
        if (leader == null || !Boolean.TRUE.equals(leader.getIsLeader())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }

        // ✅ Cập nhật địa chỉ
        groupOrder.setAddress(address);
        groupOrder.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrder);

        // 🔔 Gửi thông báo đến các thành viên chưa rời nhóm, trừ leader
        try {
            String message = "Leader đã cập nhật địa chỉ giao hàng nhóm thành: " + address;
            groupOrder.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(leaderId)) // chỉ gửi cho thành viên chưa rời nhóm và khác leader
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendProductUpdateNotification(receiverId, groupOrderId, message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("❌ Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("❌ Không thể gửi thông báo cập nhật địa chỉ: " + e.getMessage());
        }

        return ResponseEntity.ok("Successfully updated address");
    }



    public ResponseEntity<?> changeTypeGroup(TypeGroupOrder typeGroupOrder, Integer groupOrderId, Integer leaderId) {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }


        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }
        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderId);
        if (leader.getIsLeader() == Boolean.FALSE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }
        TypeGroupOrder currentType = groupOrder.getTypeGroupOrder();
        if (currentType == TypeGroupOrder.PAY_FOR_ALL) {
            if (typeGroupOrder == TypeGroupOrder.SPLIT_BILL_WITH_ALL) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể chuyển đổi trạng thái");
            }

            if (typeGroupOrder == TypeGroupOrder.PAY_FOR_ALL) {
                groupOrder.setTypeGroupOrder(typeGroupOrder);
                groupOrder.setDateUpdated(LocalDateTime.now());
                groupOrdersRepository.save(groupOrder);
                List<GroupOrderMember> groupOrderMemberList = groupOrder.getGroupOrderMembers();
                for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
                    groupOrderMember.setTypePayment(TypePayment.NONE);
                    groupOrderMember.setDateUpdated(LocalDateTime.now());
                    groupOrderMembersRepository.save(groupOrderMember);
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update success");
            }
        } else if (currentType == TypeGroupOrder.SPLIT_BILL_WITH_ALL) {
            if (typeGroupOrder == TypeGroupOrder.PAY_FOR_ALL) {
                groupOrder.setTypeGroupOrder(typeGroupOrder);
                groupOrder.setDateUpdated(LocalDateTime.now());
                groupOrdersRepository.save(groupOrder);
                List<GroupOrderMember> groupOrderMemberList = groupOrder.getGroupOrderMembers();
                for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
                    groupOrderMember.setTypePayment(TypePayment.NONE);
                    groupOrderMember.setDateUpdated(LocalDateTime.now());
                    groupOrderMembersRepository.save(groupOrderMember);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body("Update success");
    }

    @Transactional
    public ResponseEntity<?> updateTypePaymentGroupMain(TypePayment typePayment, Integer groupId, Integer leaderId) {
        // Kiểm tra nhóm tồn tại
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }
        List<StatusGroupOrder> invalidStatuses = Arrays.asList(
                StatusGroupOrder.CANCELED,
                StatusGroupOrder.COMPLETED
        );

        if (invalidStatuses.contains(groupOrder.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group is not joinable at the current status.");
        }


        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, leaderId);
        if (leader == null || !leader.getIsLeader()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }

        if (groupOrder.getTypeGroupOrder() != TypeGroupOrder.PAY_FOR_ALL) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This action is only allowed for PAY_FOR_ALL group type");
        }

        groupOrder.setTypePayment(typePayment);
        groupOrder.setDateUpdated(LocalDateTime.now());
        groupOrdersRepository.save(groupOrder);

        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupId);
        for (GroupOrderMember member : groupOrderMemberList) {
            member.setTypePayment(typePayment);
            member.setDateUpdated(LocalDateTime.now());
            groupOrderMembersRepository.save(member);
        }

        try {
            String message = user.getFullName() + " đã xác nhận phương thức thanh toán cho đơn hàng nhóm.";
            groupOrder.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(leaderId))
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendCheckoutNotification(receiverId, groupId, message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("❌ Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("❌ Không thể gửi thông báo xác nhận thanh toán: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body("Group payment type updated successfully");
    }

    @Transactional
    public ResponseEntity<?> updateTypePaymentGroupMember(TypePayment typePayment, Integer groupId, Integer userId) {

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20

// Nếu now >= groupTime thì không cho phép

        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }


        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Member not found");
        }

        GroupOrderMember member = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, userId);
        if (member == null || member.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member not found in the group");
        }

        if (groupOrder.getTypeGroupOrder() == TypeGroupOrder.PAY_FOR_ALL) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Individual payment type selection is not allowed for PAY_FOR_ALL group type");
        }

        member.setTypePayment(typePayment);
        member.setDateUpdated(LocalDateTime.now());
        groupOrderMembersRepository.save(member);

        return ResponseEntity.status(HttpStatus.OK).body("Member payment type updated successfully");
    }


    public static double calculateFee(double distance) {
        if (distance < 1) {
            return 5000.0;
        } else if (distance >= 1 && distance < 5) {
            return 15000.0;
        } else if (distance >= 5 && distance < 10) {
            return 25000.0;
        } else if (distance >= 10 && distance < 15) {
            return 35000.0;
        } else if (distance >= 15 && distance <= 20) {
            return 50000.0;
        } else {
            return 0.0;
        }
    }
    @Transactional
    public ResponseEntity<?> previewPayment(Integer groupId, Language language, Integer userIdReq) {
        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        if(groupOrder.getAddress() == null || groupOrder.getAddress().isEmpty())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Address is empty");
        }
        Integer total = 0;
        Double Subtotal = 0.0;

        List<CRUDCartGroupResponse> cartGroupResponseArrayList = new ArrayList<>();
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupId);
        List<CRUDGroupOrderMemberDetailResponse> crudGroupOrderMemberResponseList = new ArrayList<>();


        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;
            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartGroup.getCartId());
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        String proname = "";
                        if (language == Language.VN) {
                            proname = cartItem.getProductVariants().getProduct().getProName();
                        } else {
                            Product product = cartItem.getProductVariants().getProduct();
                            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                            proname = productTranslation.getProName();
                        }
                        total += cartItem.getQuantity();
                        Subtotal += cartItem.getTotalPrice();
                        cartItemResponses.add(new CRUDCartItemGroupResponse(
                                cartItem.getCartItemId(),
                                cartItem.getProductVariants().getProduct().getProId(),
                                proname,
                                cartItem.getCartGroup().getCartId(),
                                cartItem.getProductVariants().getSize(),
                                cartItem.getProductVariants().getPrice(),
                                cartItem.getTotalPrice(),
                                cartItem.getQuantity(),
                                extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg())
                        ));
                    }
                }

                crudCartGroupResponse = new CRUDCartGroupResponse(
                        cartGroup.getCartId(),
                        cartGroup.getGroupOrderMember().getGroupOrder().getGroupOrderId(),
                        cartGroup.getUser().getUserId(),
                        cartGroup.getGroupOrderMember().getMemberId(),
                        cartGroup.getTotalPrice(),
                        cartGroup.getTotalProduct(),
                        cartItemResponses
                );
                cartGroupResponseArrayList.add(crudCartGroupResponse);


            }

        }
        int discountPercent = 0;
        long activeMemberCount = groupOrder.getGroupOrderMembers().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
        if (activeMemberCount >= 8) {
            discountPercent = 10;
        } else if (activeMemberCount >= 5) {
            discountPercent = 6;
        } else if (activeMemberCount >= 3) {
            discountPercent = 4;
        } else if (activeMemberCount >= 2) {
            discountPercent = 2;
        }
        Double deliveryFee = 0.0;
        String address = groupOrder.getAddress();
        if(address.isEmpty())
        {
            deliveryFee = 0.0;
        }
        String place_id = supportFunction.getLocation(address);
        double[] destinations= supportFunction.getCoordinates(place_id);
        double[] origins = {10.850575879000075,106.77190192800003};
        groupOrder.setLatitude(destinations[0]);
        groupOrder.setLongitude(destinations[1]);
        groupOrdersRepository.save(groupOrder);
        // Số 1-3 Võ Văn Ngân, Thủ Đức, Tp HCM
        DistanceAndDuration distanceAndDuration = supportFunction.getShortestDistance(origins, destinations);
        double distance = distanceAndDuration.getDistance();
        if(distance > 20){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Distance exceeded, please update address");
        }
        deliveryFee = calculateFee(distance);
        double discountAmount = Subtotal * discountPercent / 100.0;
        Double TotalPrice = Subtotal + deliveryFee - discountAmount;
        return ResponseEntity.status(HttpStatus.OK).body(new previewPaymentGroupOrderResponse(
                groupId,
                deliveryFee,
                discountAmount,
                Subtotal,
                TotalPrice,
                total,
                cartGroupResponseArrayList
        ));
    }


    public boolean isInWorkingHours() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(8, 0);      // 8:00 AM
        LocalTime end = LocalTime.of(21, 30);      // 9:30 PM

        return !now.isBefore(start) && !now.isAfter(end);
    }

    public void processGroupOrder(GroupOrders groupOrder) {
        List<GroupOrderMember> groupOrderMemberList = groupOrder.getGroupOrderMembers();

        // Preload product variants
        Set<Integer> variantIds = groupOrderMemberList.stream()
                .flatMap(member -> member.getCartGroup().getCartItems().stream())
                .map(cartItem -> cartItem.getProductVariants().getVarId())
                .collect(Collectors.toSet());
        Map<Integer, ProductVariants> variantMap = productVariantsRepository.findAllById(variantIds)
                .stream().collect(Collectors.toMap(ProductVariants::getVarId, Function.identity()));

        List<Integer> variantIdList = new ArrayList<>(variantIds);

        Map<Integer, List<CartItem>> cartItemsByVariant = cartItemRepository
                .findByProductVariants_VarIdIn(variantIdList)
                .stream()
                .collect(Collectors.groupingBy(ci -> ci.getProductVariants().getVarId()));


        Map<Integer, List<CartItemGroup>> cartItemGroupsByVariant = cartItemGroupRepository
                .findByProductVariants_VarIdIn(new ArrayList<>(variantIds))
                .stream()
                .collect(Collectors.groupingBy(cig -> cig.getProductVariants().getVarId()));


        for (GroupOrderMember member : groupOrderMemberList) {
            CartGroup cartGroup = member.getCartGroup();

            for (CartItemGroup cartItem : cartGroup.getCartItems()) {
                ProductVariants productVariants = variantMap.get(cartItem.getProductVariants().getVarId());

                if (productVariants.getStock() >= cartItem.getQuantity()) {
                    productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());

                    if (productVariants.getStock() == 0) {
                        // Update CartItems (single cart)
                        List<CartItem> cartItems = cartItemsByVariant.getOrDefault(productVariants.getVarId(), Collections.emptyList());
                        for (CartItem cartItem1 : cartItems) {
                            Cart cart = cartItem1.getCart();
                            if (cart.getStatus() == Status_Cart.NEW) {
                                cartItem1.setQuantity(0);
                                cartItem1.setNote("Hiện đang hết hàng");
                                cartItem1.setTotalPrice(0.0);
                            }
                        }
                        cartItemRepository.saveAll(cartItems);

                        // Recalculate and update affected Carts
                        Map<Integer, List<CartItem>> cartItemsGrouped = cartItems.stream()
                                .collect(Collectors.groupingBy(ci -> ci.getCart().getCartId()));

                        for (Map.Entry<Integer, List<CartItem>> entry : cartItemsGrouped.entrySet()) {
                            Cart cart = entry.getValue().get(0).getCart();
                            double total = entry.getValue().stream().mapToDouble(CartItem::getTotalPrice).sum();
                            int quantity = entry.getValue().stream().mapToInt(CartItem::getQuantity).sum();
                            cart.setTotalPrice(total);
                            cart.setTotalProduct(quantity);
                        }
                        cartRepository.saveAll(cartItemsGrouped.values().stream().map(l -> l.get(0).getCart()).collect(Collectors.toList()));

                        // Update CartItemGroups
                        List<CartItemGroup> cartItemGroups = cartItemGroupsByVariant.getOrDefault(productVariants.getVarId(), Collections.emptyList());
                        Map<Integer, List<CartItemGroup>> cartItemGroupsByCart = cartItemGroups.stream()
                                .collect(Collectors.groupingBy(cig -> cig.getCartGroup().getCartId()));

                        for (List<CartItemGroup> itemGroupList : cartItemGroupsByCart.values()) {
                            CartGroup group = itemGroupList.get(0).getCartGroup();
                            GroupOrders checkOrder = group.getGroupOrderMember().getGroupOrder();

                            if (checkOrder != null && checkOrder.getStatus() != StatusGroupOrder.COMPLETED
                                    && checkOrder.getStatus() != StatusGroupOrder.CANCELED) {

                                double total = itemGroupList.stream().mapToDouble(CartItemGroup::getTotalPrice).sum();
                                int quantity = itemGroupList.stream().mapToInt(CartItemGroup::getQuantity).sum();

                                group.setTotalPrice(total);
                                group.setTotalProduct(quantity);
                                group.setDateUpdated(LocalDateTime.now());
                                cartGroupRepository.save(group);

                                GroupOrderMember groupMember = group.getGroupOrderMember();
                                groupMember.setAmount(total);
                                groupMember.setQuantity(quantity);
                                groupMember.setStatus(StatusGroupOrderMember.SHOPPING);
                                groupMember.setDateUpdated(LocalDateTime.now());
                                groupOrderMembersRepository.save(groupMember);

                                GroupOrders groupOrders = groupMember.getGroupOrder();
                                GroupOrders reloadedOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());

                                List<GroupOrderMember> allMembers = reloadedOrder.getGroupOrderMembers();
                                double totalPrice = allMembers.stream()
                                        .filter(m -> m.getAmount() != null)
                                        .mapToDouble(GroupOrderMember::getAmount).sum();
                                int totalQty = allMembers.stream()
                                        .filter(m -> m.getQuantity() != null)
                                        .mapToInt(GroupOrderMember::getQuantity).sum();

                                reloadedOrder.setTotalQuantity(totalQty);
                                reloadedOrder.setTotalPrice(totalPrice);
                                reloadedOrder.setStatus(StatusGroupOrder.SHOPPING);
                                reloadedOrder.setDateUpdated(LocalDateTime.now());
                                groupOrdersRepository.save(reloadedOrder);
                            }
                        }
                    }
                }
            }
        }

        productVariantsRepository.saveAll(variantMap.values());
    }


    @Transactional
    public ResponseEntity<?> confirmPayment(Integer groupId, Integer leaderId,Language language)
    {
//        if (!isInWorkingHours()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Outside of working hours.");
//        }

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime();
        if (groupOrder.getTypeTime() == Status_Type_Time_Group.TIME) {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("The group action time has expired and can no longer be performed.");
            }
        }

        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Leader not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, leaderId);
        if (leader == null || !Boolean.TRUE.equals(leader.getIsLeader())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Leader not found");
        }



        for(GroupOrderMember groupOrderMember: groupOrder.getGroupOrderMembers())
        {
            List<CartItemGroup> cartItem_Check = groupOrderMember.getCartGroup().getCartItems();
            for(CartItemGroup cartItem : cartItem_Check) {
                CartGroup cartGroup = cartItem.getCartGroup();
                Integer quantity = cartItem.getQuantity();
                Integer quantity_product_remain = cartItem.getProductVariants().getStock();
                if (quantity_product_remain == 0) {
                    cartItem.setQuantity(0);
                    cartItem.setTotalPrice(0.0);
                    cartItemGroupRepository.save(cartItem);
                    Integer quantity_remain = groupOrderMember.getCartGroup().getTotalProduct() - quantity;
                    Double total_price = groupOrderMember.getCartGroup().getTotalPrice() - quantity * cartItem.getProductVariants().getPrice();
                    cartGroup.setTotalPrice(total_price);
                    cartGroup.setTotalProduct(quantity_remain);
                    cartGroupRepository.save(cartGroup);
                    String product_name = "";

                    if (language == Language.VN) {
                        product_name = cartItem.getProductVariants().getProduct().getProName();
                    } else {
                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
                        product_name = productTranslation.getProName();
                    }
                    String errorMessage = String.format(
                            "Not enough product for %s, size %s. Requested quantity: %d, Available: %d",
                            product_name, cartItem.getProductVariants().getSize(), quantity, quantity_product_remain
                    );

                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
                }
                if (quantity_product_remain < quantity) {
                    String product_name = "";

                    if (language == Language.VN) {
                        product_name = cartItem.getProductVariants().getProduct().getProName();
                    } else {
                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(cartItem.getProductVariants().getProduct().getProId());
                        product_name = productTranslation.getProName();
                    }
                    String errorMessage = String.format(
                            "Not enough product for %s, size %s. Requested quantity: %d, Available: %d",
                            product_name, cartItem.getProductVariants().getSize(), quantity, quantity_product_remain
                    );

                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
                }
            }
        }




        groupOrder.setStatus(StatusGroupOrder.CHECKOUT);
        groupOrder.setDateCheckout(LocalDateTime.now());
        groupOrdersRepository.save(groupOrder);
        processGroupOrder(groupOrder);
//        List<GroupOrderMember> groupOrderMemberList = groupOrder.getGroupOrderMembers();
//        for(GroupOrderMember groupOrderMember: groupOrderMemberList)
//        {
//            CartGroup cartGroup = groupOrderMember.getCartGroup();
//            List<CartItemGroup> cartItems1 = cartGroup.getCartItems();
//            for (CartItemGroup cartItem : cartItems1) {
//                ProductVariants productVariants = cartItem.getProductVariants();
//                if (productVariants.getStock() >= cartItem.getQuantity()) {
//                    productVariants.setStock(productVariants.getStock() - cartItem.getQuantity());
//                    productVariantsRepository.save(productVariants);
//                    if (productVariants.getStock() == 0) {
//                        List<CartItem> cartItemList = cartItemRepository.findByProductVariants_VarId(productVariants.getVarId());
//                        for (CartItem cartItemList1 : cartItemList) {
//                            Cart cart1 = cartItemList1.getCart();
//                            if (cart1.getStatus() == Status_Cart.NEW) {
//                                cartItemList1.setQuantity(0);
//                                cartItemList1.setNote("Hiện đang hết hàng");
//                                cartItemList1.setTotalPrice(0.0);
//                                cartItemRepository.save(cartItemList1);
//                            }
//                            List<CartItem> cartItemList2 = cartItemRepository.findByCart_CartIdAndIsDisabledFalse(cart1.getCartId());
//                            double total = 0.0;
//                            int total_quantity = 0;
//                            for (CartItem cartItemList3 : cartItemList2) {
//                                total += cartItemList3.getTotalPrice();
//                                total_quantity += cartItemList3.getQuantity();
//                            }
//                            cart1.setTotalPrice(total);
//                            cart1.setTotalProduct(total_quantity);
//                            cartRepository.save(cart1);
//                        }
//                        List<CartItemGroup> cartItemList0 = cartItemGroupRepository.findByProductVariants_VarId(productVariants.getVarId());
//                        for (CartItemGroup cartItemList1 : cartItemList0) {
//                            CartGroup cart1 = cartItemList1.getCartGroup();
//                            GroupOrders groupOrders_check = cart1.getGroupOrderMember().getGroupOrder();
//                            if (groupOrders_check != null && groupOrders_check.getStatus() != StatusGroupOrder.COMPLETED && groupOrders_check.getStatus() != StatusGroupOrder.CANCELED) {
//                                List<CartItemGroup> cartItemList2 = cartItemGroupRepository.findByCartGroupCartIdAndIsDisabledFalse(cart1.getCartId());
//                                double total = 0.0;
//                                int total_quantity = 0;
//                                for (CartItemGroup cartItemList3 : cartItemList2) {
//                                    total += cartItemList3.getTotalPrice();
//                                    total_quantity += cartItemList3.getQuantity();
//                                }
//                                cart1.setTotalPrice(total);
//                                cart1.setTotalProduct(total_quantity);
//                                cart1.setDateUpdated(LocalDateTime.now());
//                                cartGroupRepository.save(cart1);
//                                GroupOrderMember groupOrderMember1 = cart1.getGroupOrderMember();
//                                groupOrderMember1.setAmount(total);
//                                groupOrderMember1.setQuantity(total_quantity);
//                                groupOrderMember1.setStatus(StatusGroupOrderMember.SHOPPING);
//                                groupOrderMember1.setDateUpdated(LocalDateTime.now());
//                                groupOrderMembersRepository.save(groupOrderMember);
//
//                                GroupOrders groupOrders = groupOrderMember.getGroupOrder();
//                                GroupOrders groupOrders1 = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrders.getGroupOrderId());
//                                List<GroupOrderMember> groupOrderMemberList1 = groupOrders1.getGroupOrderMembers();
//                                Double Price_Group = 0.0;
//                                Integer Quantity_Group = 0;
//                                for (GroupOrderMember groupOrderMember2 : groupOrderMemberList1) {
//                                    if (groupOrderMember2.getAmount() != null) {
//                                        Price_Group += groupOrderMember2.getAmount();
//                                    }
//                                    if (groupOrderMember2.getQuantity() != null) {
//                                        Quantity_Group += groupOrderMember2.getQuantity();
//                                    }
//                                }
//
//                                groupOrders1.setTotalQuantity(Quantity_Group);
//                                groupOrders1.setTotalPrice(Price_Group);
//                                groupOrders1.setStatus(StatusGroupOrder.SHOPPING);
//                                groupOrders1.setDateUpdated(LocalDateTime.now());
//                                groupOrdersRepository.save(groupOrders1);
//                            }
//                        }
//                    }
//                }
//
//
//            }
//        }


        // 🔔 Gửi thông báo đến các thành viên khác (trừ leader)
        try {
            String message = user.getFullName() + " đã xác nhận thanh toán cho đơn hàng nhóm.";
            groupOrder.getGroupOrderMembers().stream()
                    .filter(m -> !m.getIsDeleted() && !m.getUser().getUserId().equals(leaderId))
                    .forEach(m -> {
                        try {
                            Integer receiverId = m.getUser().getUserId();
                            notificationService.sendCheckoutNotification(receiverId, groupId, message);
                            System.out.println("📨 Gửi thông báo tới userId=" + receiverId + " với message=" + message);
                        } catch (Exception ex) {
                            System.err.println("❌ Không thể gửi thông báo tới userId=" + m.getUser().getUserId() + ": " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("❌ Không thể gửi thông báo xác nhận thanh toán: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Group order confirmation");
    }



    public  ResponseEntity<?> getLinkGroup(Integer groupId,Integer userIdReq )
    {

        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        LocalTime localTime = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime(); // ví dụ: 09:20


        if(groupOrder.getTypeTime() == Status_Type_Time_Group.TIME)
        {
            if (!localTime.isBefore(groupTime)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("❌ Đã quá thời gian thực hiện nhóm, không thể thao tác.");
            }
        }

        String link = groupOrder.getLink();
        return ResponseEntity.status(HttpStatus.OK).body(link);
    }




    public ResponseEntity<?> getGroupRemainingTime(Integer groupId,Integer userIdReq) {
        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        if (groupOrder.getTypeTime() != Status_Type_Time_Group.TIME) {
            return ResponseEntity.ok(-1);
        }

        LocalTime now = LocalTime.now();
        LocalTime groupTime = groupOrder.getDatePaymentTime();

        if (!now.isBefore(groupTime)) {

            return ResponseEntity.ok("00:00:00");
        }

        Duration duration = Duration.between(now, groupTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        String timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return ResponseEntity.ok(timeLeft);
    }


    @Transactional
    public ResponseEntity<?> checkTimeOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<GroupOrders> allOrders = groupOrdersRepository.findAll();

        for (GroupOrders order : allOrders) {
            StatusGroupOrder status = order.getStatus();
            Status_Type_Time_Group typeTime = order.getTypeTime();

            if (typeTime != Status_Type_Time_Group.TIME) {
                continue;
            }

            List<PaymentGroup> payments = order.getPayment();


            if ((status == StatusGroupOrder.CREATED || status == StatusGroupOrder.SHOPPING) &&
                    (payments == null || payments.isEmpty())) {
                order.setStatus(StatusGroupOrder.CANCELED);
                groupOrdersRepository.save(order);
                continue;
            }

            // CASE B: Đơn CREATED, kiểu TIME, và đã quá giờ hạn trong hôm nay => huỷ
            if (status == StatusGroupOrder.CREATED) {
                LocalTime deadlineTime = order.getDatePaymentTime();
                if (deadlineTime != null) {
                    LocalDateTime deadlineDateTime = LocalDateTime.of(today, deadlineTime);
                    if (now.isAfter(deadlineDateTime)) {
                        order.setStatus(StatusGroupOrder.CANCELED);
                        groupOrdersRepository.save(order);
                        continue;
                    }
                }
            }

            // CASE C: Đơn SHOPPING cùng ngày tạo, quá 29 phút mà chưa shipment => huỷ
            if (status == StatusGroupOrder.SHOPPING &&
                    order.getDateCreated().toLocalDate().isEqual(today)) {

                for (PaymentGroup payment : payments) {
                    if (payment == null || payment.getShipment() != null) {
                        continue;
                    }

                    if (payment.getStatus() == Status_Payment.PENDING &&
                            payment.getDateCreated().plusMinutes(29).isBefore(now)) {

                        order.setStatus(StatusGroupOrder.CANCELED);
                        payment.setStatus(Status_Payment.FAILED);

                        paymentGroupRepository.save(payment);
                        groupOrdersRepository.save(order);
                        break;
                    }
                }
            }
        }

        return ResponseEntity.ok("Success");
    }

    @Transactional
    @Scheduled(cron = "0 */15 * * * *")
    public void checkTimeCheckout() {
        LocalDateTime now = LocalDateTime.now();

        List<GroupOrders> allOrders = groupOrdersRepository.findAll();

        for (GroupOrders order : allOrders) {
            if (order.getStatus() == StatusGroupOrder.CHECKOUT) {
                LocalDateTime checkoutTime = order.getDateCheckout();
                if (checkoutTime != null && checkoutTime.plusMinutes(30).isBefore(now)) {

                    List<PaymentGroup> payments = order.getPayment();

                    boolean hasSuccessfulPayment = false;
                    if (payments != null && !payments.isEmpty()) {
                        for (PaymentGroup payment : payments) {
                            if (payment.getStatus() == Status_Payment.COMPLETED) {
                                hasSuccessfulPayment = true;
                                break;
                            }
                        }
                    }

                    if (!hasSuccessfulPayment) {
                        order.setStatus(StatusGroupOrder.CANCELED);
                        order.setDateUpdated(now);
                        // Nếu bạn có trường CancelReason, bạn có thể set thêm lý do bị hủy
                    }
                }
            }
        }

        // Sau khi thay đổi trạng thái, save lại toàn bộ
        groupOrdersRepository.saveAll(allOrders);
    }


    @Transactional
    @Scheduled(cron = "0 */30 * * * *")
    public void checkTimeOrdersScheduled() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<GroupOrders> allOrders = groupOrdersRepository.findAll();

        for (GroupOrders order : allOrders) {
            StatusGroupOrder status = order.getStatus();
            Status_Type_Time_Group typeTime = order.getTypeTime();

            if (typeTime != Status_Type_Time_Group.TIME) {
                continue;
            }

            List<PaymentGroup> payments = order.getPayment();


            if ((status == StatusGroupOrder.CREATED || status == StatusGroupOrder.SHOPPING) &&
                    (payments == null || payments.isEmpty())) {
                order.setStatus(StatusGroupOrder.CANCELED);
                groupOrdersRepository.save(order);
                continue;
            }

            // CASE B: Đơn CREATED, kiểu TIME, và đã quá giờ hạn trong hôm nay => huỷ
            if (status == StatusGroupOrder.CREATED) {
                LocalTime deadlineTime = order.getDatePaymentTime();
                if (deadlineTime != null) {
                    LocalDateTime deadlineDateTime = LocalDateTime.of(today, deadlineTime);
                    if (now.isAfter(deadlineDateTime)) {
                        order.setStatus(StatusGroupOrder.CANCELED);
                        groupOrdersRepository.save(order);
                        continue;
                    }
                }
            }

            // CASE C: Đơn SHOPPING cùng ngày tạo, quá 29 phút mà chưa shipment => huỷ
            if (status == StatusGroupOrder.SHOPPING &&
                    order.getDateCreated().toLocalDate().isEqual(today)) {

                for (PaymentGroup payment : payments) {
                    if (payment == null || payment.getShipment() != null) {
                        continue;
                    }

                    if (payment.getStatus() == Status_Payment.PENDING &&
                            payment.getDateCreated().plusMinutes(29).isBefore(now)) {

                        order.setStatus(StatusGroupOrder.CANCELED);
                        payment.setStatus(Status_Payment.FAILED);

                        paymentGroupRepository.save(payment);
                        groupOrdersRepository.save(order);
                        break;
                    }
                }
            }
        }
    }


    @Transactional
    @Scheduled(cron = "0 */30 * * * *")
    public void checkTimeOrdersLocal() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<GroupOrders> allOrders = groupOrdersRepository.findAll();

        for (GroupOrders order : allOrders) {
            StatusGroupOrder status = order.getStatus();
            Status_Type_Time_Group typeTime = order.getTypeTime();

            if (typeTime != Status_Type_Time_Group.TIME) {
                continue;
            }

            List<PaymentGroup> payments = order.getPayment();


            if ((status == StatusGroupOrder.CREATED || status == StatusGroupOrder.SHOPPING) &&
                    (payments == null || payments.isEmpty())) {
                order.setStatus(StatusGroupOrder.CANCELED);
                groupOrdersRepository.save(order);
                continue;
            }

            // CASE B: Đơn CREATED, kiểu TIME, và đã quá giờ hạn trong hôm nay => huỷ
            if (status == StatusGroupOrder.CREATED) {
                LocalTime deadlineTime = order.getDatePaymentTime();
                if (deadlineTime != null) {
                    LocalDateTime deadlineDateTime = LocalDateTime.of(today, deadlineTime);
                    if (now.isAfter(deadlineDateTime)) {
                        order.setStatus(StatusGroupOrder.CANCELED);
                        groupOrdersRepository.save(order);
                        continue;
                    }
                }
            }

            // CASE C: Đơn SHOPPING cùng ngày tạo, quá 29 phút mà chưa shipment => huỷ
            if (status == StatusGroupOrder.SHOPPING &&
                    order.getDateCreated().toLocalDate().isEqual(today)) {

                for (PaymentGroup payment : payments) {
                    if (payment == null || payment.getShipment() != null) {
                        continue;
                    }

                    if (payment.getStatus() == Status_Payment.PENDING &&
                            payment.getDateCreated().plusMinutes(29).isBefore(now)) {

                        order.setStatus(StatusGroupOrder.CANCELED);
                        payment.setStatus(Status_Payment.FAILED);

                        paymentGroupRepository.save(payment);
                        groupOrdersRepository.save(order);
                        break;
                    }
                }
            }
        }

    }



    public CRUDGroupOrderResponse mapToCRUDGroupOrderResponse(GroupOrders entity) {
        CRUDGroupOrderResponse dto = new CRUDGroupOrderResponse();
        dto.setGroupOrderId(entity.getGroupOrderId());
        dto.setNameLeader(entity.getUser().getFullName());
        dto.setAddress(entity.getAddress());
        dto.setNote(entity.getNote());
        dto.setLink(entity.getLink());
        dto.setCode(entity.getCode());
        dto.setNameGroup(entity.getNameGroup());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setTypeGroupOrder(entity.getTypeGroupOrder());
        dto.setStatus(entity.getStatus());
        dto.setIsDeleted(entity.getIsDeleted());
        dto.setOrderDate(entity.getOrderDate());
        dto.setDeadlinePayment(entity.getDeadlinePayment());
        dto.setDateCreated(entity.getDateCreated());
        dto.setDateUpdated(entity.getDateUpdated());
        dto.setDateDeleted(entity.getDateDeleted());
        return dto;
    }

    public CRUDGroupOrderMemberDetailResponse mapToCRUDGroupOrderMemberDetail(GroupOrderMember entity) {
        System.out.println(entity.getGroupOrder().getStatus());
        CRUDGroupOrderMemberDetailResponse dto = new CRUDGroupOrderMemberDetailResponse();
        dto.setMemberId(entity.getMemberId());
        dto.setName(entity.getUser().getFullName());
        dto.setGroupOrderId(entity.getGroupOrder().getGroupOrderId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setAmount(entity.getAmount());
        dto.setIsPaid(entity.getIsPaid());
        dto.setIsLeader(entity.getIsLeader());
        dto.setNote(entity.getNote());
        dto.setStatus(entity.getStatus());
        dto.setTypePayment(entity.getTypePayment());
        dto.setDateCreated(entity.getDateCreated());
        dto.setDateUpdated(entity.getDateUpdated());
        dto.setDateDeleted(entity.getDateDeleted());
        dto.setIsDeleted(entity.getIsDeleted());
        return dto;
    }

    public CRUDCartGroupResponse mapToCRUDCartGroupResponse(CartGroup entity) {
        CRUDCartGroupResponse dto = new CRUDCartGroupResponse();
        dto.setCartGroupId(entity.getCartId());
        dto.setGroupId(entity.getGroupOrderMember().getGroupOrder().getGroupOrderId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setMemberId(entity.getGroupOrderMember().getMemberId());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setTotalQuantity(entity.getTotalProduct());

        List<CRUDCartItemGroupResponse> itemList = entity.getCartItems().stream()
                .map(this::mapToCRUDCartItemGroupResponse)
                .collect(Collectors.toList());
        dto.setListCartItemGroup(itemList);

        return dto;
    }

    public CRUDCartItemGroupResponse mapToCRUDCartItemGroupResponse(CartItemGroup entity) {
        CRUDCartItemGroupResponse dto = new CRUDCartItemGroupResponse();
        dto.setCartItemGroupId(entity.getCartItemId());
        dto.setProId(entity.getProductVariants().getProduct().getProId());
        dto.setProName(entity.getProductVariants().getProduct().getProName());
        dto.setCartGroupId(entity.getCartGroup().getCartId());
        dto.setSize(entity.getProductVariants().getSize());
        dto.setItemPrice(entity.getItemPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setImageUrl(entity.getProductVariants().getProduct().getListProImg());
        return dto;
    }

    @Transactional
    public ResponseEntity<?> listHistoryGroupOrderCompleted(int page, int size, int userId) {

        int limit = Integer.parseInt(String.valueOf(size));
        if (limit >= 100) limit = 100;
        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit,sort);

        Page<GroupOrders> pageResult = groupOrdersRepository.findAllByIsDeletedFalseAndStatusAndUserUserId(StatusGroupOrder.COMPLETED,userId,pageable);
        for(GroupOrders groupOrders: pageResult)
        {
            System.out.println(groupOrders.getGroupOrderId());
        }

        List<getGroupOrderResponse> groupResponseList = pageResult.getContent().stream()
                .map(groupOrder -> {
                    System.out.println(groupOrder.getGroupOrderId());
                    CRUDGroupOrderResponse crudGroupOrderResponse = mapToCRUDGroupOrderResponse(groupOrder);

                    // Lấy danh sách thành viên của nhóm
                    List<GroupOrderMember> members = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrder.getGroupOrderId());

                    // Mapping member list
                    List<CRUDGroupOrderMemberDetailResponse> memberResponses = members.stream()
                            .map(member -> {
                                CRUDGroupOrderMemberDetailResponse memberDetail = mapToCRUDGroupOrderMemberDetail(member);
                                // Set giỏ hàng của từng thành viên
                                CartGroup cart = cartGroupRepository.findByUserUserIdAndGroupOrderMemberMemberId(userId, member.getMemberId());
                                if (cart != null) {
                                    memberDetail.setCrudCartGroupResponse(mapToCRUDCartGroupResponse(cart));
                                }
                                return memberDetail;
                            }).collect(Collectors.toList());

                    // Tính tổng giá trị đơn
                    double totalPrice = memberResponses.stream()
                            .mapToDouble(member -> member.getCrudCartGroupResponse() != null ? member.getCrudCartGroupResponse().getTotalPrice() : 0.0)
                            .sum();

                    // Gộp vào response
                    getGroupOrderResponse groupResponse = new getGroupOrderResponse();
                    groupResponse.setTotal((int) totalPrice);
                    groupResponse.setCrudGroupOrderResponse(crudGroupOrderResponse);
                    groupResponse.setCrudGroupOrderResponseList(memberResponses);

                    return groupResponse;
                })
                .collect(Collectors.toList());

        ListAllGroupOrderCompletedResponse response = new ListAllGroupOrderCompletedResponse();
        response.setCurrentPage(page);
        response.setTotalPages(pageResult.getTotalPages());
        response.setLimit(size);
        response.setTotal((int) pageResult.getTotalElements());
        response.setListGroup(groupResponseList);

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> listHistoryGroupOrderCancelled(int page, int size, int userId) {

        int limit = Integer.parseInt(String.valueOf(size));
        if (limit >= 100) limit = 100;
        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit,sort);

        Page<GroupOrders> pageResult = groupOrdersRepository.findAllByIsDeletedFalseAndStatusAndUserUserId(StatusGroupOrder.CANCELED,userId,pageable);
        for(GroupOrders groupOrders: pageResult)
        {
            System.out.println(groupOrders.getGroupOrderId());
        }

        List<getGroupOrderResponse> groupResponseList = pageResult.getContent().stream()
                .map(groupOrder -> {
                    System.out.println(groupOrder.getGroupOrderId());
                    CRUDGroupOrderResponse crudGroupOrderResponse = mapToCRUDGroupOrderResponse(groupOrder);

                    // Lấy danh sách thành viên của nhóm
                    List<GroupOrderMember> members = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrder.getGroupOrderId());

                    // Mapping member list
                    List<CRUDGroupOrderMemberDetailResponse> memberResponses = members.stream()
                            .map(member -> {
                                CRUDGroupOrderMemberDetailResponse memberDetail = mapToCRUDGroupOrderMemberDetail(member);
                                // Set giỏ hàng của từng thành viên
                                CartGroup cart = cartGroupRepository.findByUserUserIdAndGroupOrderMemberMemberId(userId, member.getMemberId());
                                if (cart != null) {
                                    memberDetail.setCrudCartGroupResponse(mapToCRUDCartGroupResponse(cart));
                                }
                                return memberDetail;
                            }).collect(Collectors.toList());

                    // Tính tổng giá trị đơn
                    double totalPrice = memberResponses.stream()
                            .mapToDouble(member -> member.getCrudCartGroupResponse() != null ? member.getCrudCartGroupResponse().getTotalPrice() : 0.0)
                            .sum();

                    // Gộp vào response
                    getGroupOrderResponse groupResponse = new getGroupOrderResponse();
                    groupResponse.setTotal((int) totalPrice);
                    groupResponse.setCrudGroupOrderResponse(crudGroupOrderResponse);
                    groupResponse.setCrudGroupOrderResponseList(memberResponses);

                    return groupResponse;
                })
                .collect(Collectors.toList());

        ListAllGroupOrderCompletedResponse response = new ListAllGroupOrderCompletedResponse();
        response.setCurrentPage(page);
        response.setTotalPages(pageResult.getTotalPages());
        response.setLimit(size);
        response.setTotal((int) pageResult.getTotalElements());
        response.setListGroup(groupResponseList);

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> listAllRefundGroupOrderByLeader(int page, int size,int userId) {

        int limit = Integer.parseInt(String.valueOf(size));
        if (limit >= 100) limit = 100;
        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit,sort);
        Page<GroupOrders> pageResult = groupOrdersRepository.findGroupOrdersWithRefundPaymentByUserId(userId,pageable);
        List<getGroupOrderResponse> groupResponseList = pageResult.getContent().stream()
                .filter(groupOrder ->
                        groupOrder.getPayment().stream()
                                .anyMatch(payment -> payment.getStatus() == Status_Payment.REFUND)
                )
                .map(groupOrder -> {

                    CRUDGroupOrderResponse crudGroupOrderResponse = mapToCRUDGroupOrderResponse(groupOrder);
                    List<GroupOrderMember> members = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrder.getGroupOrderId());
                    List<CRUDGroupOrderMemberDetailResponse> memberResponses = members.stream()
                            .map(member -> {
                                CRUDGroupOrderMemberDetailResponse memberDetail = mapToCRUDGroupOrderMemberDetail(member);
                                // Set giỏ hàng của từng thành viên
                                CartGroup cart = cartGroupRepository.findByUserUserIdAndGroupOrderMemberMemberId(userId, member.getMemberId());
                                if (cart != null) {
                                    memberDetail.setCrudCartGroupResponse(mapToCRUDCartGroupResponse(cart));
                                }
                                return memberDetail;
                            }).collect(Collectors.toList());

                    // Tính tổng giá trị đơn
                    double totalPrice = memberResponses.stream()
                            .mapToDouble(member -> member.getCrudCartGroupResponse() != null ? member.getCrudCartGroupResponse().getTotalPrice() : 0.0)
                            .sum();

                    // Gộp vào response
                    getGroupOrderResponse groupResponse = new getGroupOrderResponse();
                    groupResponse.setTotal((int) totalPrice);
                    groupResponse.setCrudGroupOrderResponse(crudGroupOrderResponse);
                    groupResponse.setCrudGroupOrderResponseList(memberResponses);

                    return groupResponse;
                })
                .collect(Collectors.toList());

        ListAllGroupOrderCompletedResponse response = new ListAllGroupOrderCompletedResponse();
        response.setCurrentPage(page);
        response.setTotalPages(pageResult.getTotalPages());
        response.setLimit(size);
        response.setTotal((int) pageResult.getTotalElements());
        response.setListGroup(groupResponseList);

        return ResponseEntity.ok(response);
    }


    @Transactional
    public ResponseEntity<?> listAllRefundGroupOrder( int page, int size) {
        int limit = Integer.parseInt(String.valueOf(size));
        if (limit >= 100) limit = 100;
        Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
        Pageable pageable = PageRequest.of(page - 1, limit,sort);
        Page<GroupOrders> pageResult = groupOrdersRepository.findGroupOrdersWithRefundPayment(pageable);
        List<getGroupOrderResponse> groupResponseList = pageResult.getContent().stream()
                .filter(groupOrder ->
                        groupOrder.getPayment().stream()
                                .anyMatch(payment -> payment.getStatus() == Status_Payment.REFUND)
                )
                .map(groupOrder -> {

                    CRUDGroupOrderResponse crudGroupOrderResponse = mapToCRUDGroupOrderResponse(groupOrder);

                    // Lấy danh sách thành viên của nhóm
                    List<GroupOrderMember> members = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrder.getGroupOrderId());

                    // Mapping member list
                    List<CRUDGroupOrderMemberDetailResponse> memberResponses = members.stream()
                            .map(member -> {
                                CRUDGroupOrderMemberDetailResponse memberDetail = mapToCRUDGroupOrderMemberDetail(member);
                                CartGroup cart = cartGroupRepository.findByUserUserIdAndGroupOrderMemberMemberId(memberDetail.getUserId(), member.getMemberId());
                                if (cart != null) {
                                    memberDetail.setCrudCartGroupResponse(mapToCRUDCartGroupResponse(cart));
                                }
                                return memberDetail;
                            }).collect(Collectors.toList());
                    double totalPrice = memberResponses.stream()
                            .mapToDouble(member -> member.getCrudCartGroupResponse() != null ? member.getCrudCartGroupResponse().getTotalPrice() : 0.0)
                            .sum();
                    getGroupOrderResponse groupResponse = new getGroupOrderResponse();
                    groupResponse.setTotal((int) totalPrice);
                    groupResponse.setCrudGroupOrderResponse(crudGroupOrderResponse);
                    groupResponse.setCrudGroupOrderResponseList(memberResponses);
                    return groupResponse;
                })
                .collect(Collectors.toList());

        ListAllGroupOrderCompletedResponse response = new ListAllGroupOrderCompletedResponse();
        response.setCurrentPage(page);
        response.setTotalPages(pageResult.getTotalPages());
        response.setLimit(size);
        response.setTotal((int) pageResult.getTotalElements());
        response.setListGroup(groupResponseList);

        return ResponseEntity.ok(response);
    }


    @Transactional
    public ResponseEntity<?> fetchAllInformationGroupOrder(Integer groupOrderId, Language language,Integer userIdReq) {

        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group because you ");
        }

        if(!groupOrderMember_check.getIsLeader())
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }



        CRUDPaymentGroupResponse crudPaymentGroupResponse = null;
        CRUDShipmentResponse crudShipmentResponse = null;
        PaymentGroup paymentGroup = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndStatusAndIsDeletedFalse(groupOrderId, Status_Payment.COMPLETED);
        if (paymentGroup != null) {
            crudPaymentGroupResponse = new CRUDPaymentGroupResponse(
                    paymentGroup.getPaymentId(),
                    paymentGroup.getAmount(),
                    paymentGroup.getDateCreated(),
                    paymentGroup.getDateDeleted(),
                    paymentGroup.getDateRefunded(),
                    paymentGroup.getIsDeleted(),
                    paymentGroup.getPaymentMethod(),
                    paymentGroup.getStatus(),
                    paymentGroup.getGroupOrder().getGroupOrderId(),
                    paymentGroup.getIsRefund(),
                    paymentGroup.getLink()

            );
            ShippmentGroup shippment = paymentGroup.getShipment();
            if (shippment != null) {
                User customer = groupOrder.getUser();
                crudShipmentResponse = new CRUDShipmentResponse(
                        shippment.getShipmentId(),
                        shippment.getUser() != null ? shippment.getUser().getFullName() : null,
                        shippment.getDateCreated(),
                        shippment.getDateDeleted(),
                        shippment.getDateDelivered(),
                        shippment.getDateShip(),
                        shippment.getDateCancel(),
                        shippment.getIsDeleted(),
                        shippment.getStatus(),
                        shippment.getNote(),
                        shippment.getPayment().getPaymentId(),
                        shippment.getUser() != null ? shippment.getUser().getUserId() : null,
                        customer.getFullName(),
                        customer.getUserId(),
                        paymentGroup.getGroupOrder().getAddress(),
                        customer.getPhoneNumber(),
                        customer.getEmail(),
                        paymentGroup.getGroupOrder().getGroupOrderId()

                );
            }
        }
        List<CRUDCartGroupResponse> cartGroupResponseArrayList = new ArrayList<>();
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsDeletedFalse(groupOrderId);
        List<CRUDGroupOrderMemberDetailResponse> crudGroupOrderMemberResponseList = new ArrayList<>();


        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;


            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartGroup.getCartId());
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        String proname = "";
                        if (language == Language.VN) {
                            proname = cartItem.getProductVariants().getProduct().getProName();
                        } else {
                            Product product = cartItem.getProductVariants().getProduct();
                            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                            proname = productTranslation.getProName();
                        }
                        cartItemResponses.add(new CRUDCartItemGroupResponse(
                                cartItem.getCartItemId(),
                                cartItem.getProductVariants().getProduct().getProId(),
                                proname,
                                cartItem.getCartGroup().getCartId(),
                                cartItem.getProductVariants().getSize(),
                                cartItem.getProductVariants().getPrice(),
                                cartItem.getTotalPrice(),
                                cartItem.getQuantity(),
                                extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg())
                        ));
                    }

                }

                crudCartGroupResponse = new CRUDCartGroupResponse(
                        cartGroup.getCartId(),
                        cartGroup.getGroupOrderMember().getGroupOrder().getGroupOrderId(),
                        cartGroup.getUser().getUserId(),
                        cartGroup.getGroupOrderMember().getMemberId(),
                        cartGroup.getTotalPrice(),
                        cartGroup.getTotalProduct(),
                        cartItemResponses
                );
                CRUDGroupOrderMemberDetailResponse crudGroupOrderMemberResponse = new CRUDGroupOrderMemberDetailResponse(
                        groupOrderMember.getMemberId(),
                        groupOrderMember.getUser().getFullName(),
                        groupOrderMember.getGroupOrder().getGroupOrderId(),
                        groupOrderMember.getUser().getUserId(),
                        groupOrderMember.getAmount(),
                        groupOrderMember.getIsPaid(),
                        groupOrderMember.getIsLeader(),
                        groupOrderMember.getNote(),
                        groupOrderMember.getStatus(),
                        groupOrderMember.getTypePayment(),
                        groupOrderMember.getDateCreated(),
                        groupOrderMember.getDateUpdated(),
                        groupOrderMember.getDateDeleted(),
                        groupOrderMember.getIsDeleted(),
                        crudCartGroupResponse
                );
                cartGroupResponseArrayList.add(crudCartGroupResponse);
                crudGroupOrderMemberResponseList.add(crudGroupOrderMemberResponse);
            }



        }
        CRUDGroupOrderResponse crudGroupOrderResponse = new CRUDGroupOrderResponse(
                groupOrder.getGroupOrderId(),
                groupOrder.getUser().getFullName(),
                groupOrder.getAddress(),
                groupOrder.getNote(),
                groupOrder.getLink(),
                groupOrder.getCode(),
                groupOrder.getNameGroup(),
                groupOrder.getTotalPrice(),
                groupOrder.getTypeGroupOrder(),
                groupOrder.getStatus(),
                groupOrder.getIsDeleted(),
                groupOrder.getOrderDate(),
                groupOrder.getDeadlinePayment(),
                groupOrder.getDateCreated(),
                groupOrder.getDateUpdated(),
                groupOrder.getDateDeleted()

        );

        return ResponseEntity.status(HttpStatus.OK).body(new fetchAllInformationGroupOrderResponse(
                groupOrderId,
                crudGroupOrderResponse,
                crudShipmentResponse,
                crudPaymentGroupResponse,
                crudGroupOrderMemberResponseList,
                cartGroupResponseArrayList

        ));



//    @Transactional
//    public ResponseEntity<?> listGroupOrderCancelAndPaymentRefund(Language language) {
//        // Query orders with cancelled status and refunds in one batch
//        List<GroupOrders> orders = groupOrdersRepository.findAllByStatusAndPaymentStatus(StatusGroupOrder.CANCELED, Status_Payment.REFUND);
//
//
//        for(GroupOrders order : orders) {
//            if(order.getStatus() == StatusGroupOrder.CANCELED)
//            {
//
//            }
//        }
//
//        // Sort results by refund date or order date in descending order
//        historyOrderResponses.sort(Comparator.comparing((OrderCancelPaymentRefund h) ->
//                        h.getPayment().getDateRefund() != null ? h.getPayment().getDateRefund() : h.getOrder().getDateCreated(),
//                Comparator.reverseOrder()));
//
//        return ResponseEntity.ok(new ListAllOrderCancelAndPaymentRefund(historyOrderResponses.size(), historyOrderResponses));
//    }

    }

    public ResponseEntity<?> getLinkPayment(Integer groupOrderId,Integer userIdReq)
    {


        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group because you ");
        }

        if(!groupOrderMember_check.getIsLeader())
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group");
        }

        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }

        String link = "";
        PaymentGroup paymentGroup = paymentGroupRepository.findByGroupOrder_GroupOrderIdAndStatusAndIsDeletedFalse(groupOrderId,Status_Payment.PENDING);
        if (paymentGroup != null) {
            link = paymentGroup.getLink();
        }

        return ResponseEntity.status(HttpStatus.OK).body(link);
    }

    public ResponseEntity<?>  activateBlackList(Integer groupOrderId,Integer leaderId, Integer userId)
    {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LeaderId not found");
        }

        User user_member = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user_member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderId);
        if (leader == null || !Boolean.TRUE.equals(leader.getIsLeader())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not is leader");
        }

        GroupOrderMember groupOrderMember1 = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userId);
        if(groupOrderMember1 == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in group order");
        }

        if(groupOrderMember1.getIsDeletedLeader() == Boolean.FALSE)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not deleted by leader");
        }

        GroupOrderMember groupOrderMember = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserIdAndIsBlacklistTrue(groupOrderId, userId);
        if (groupOrderMember != null && Boolean.TRUE.equals(groupOrderMember.getIsBlacklist())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Blacklist has been activated.");
        }
        else {
            groupOrderMember1.setIsBlacklist(true);
            groupOrderMember1.setDateUpdated(LocalDateTime.now());
            groupOrderMembersRepository.save(groupOrderMember1);
            return ResponseEntity.status(HttpStatus.OK).body("Blacklist has been activated.");
        }
    }

    public ResponseEntity<?>  restoreBlackList(Integer groupOrderId,Integer leaderId, Integer userId)
    {
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        User user = userRepository.findByUserIdAndIsDeletedFalse(leaderId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LeaderId not found");
        }

        User user_member = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if (user_member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        GroupOrderMember leader = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, leaderId);
        if (leader == null || !Boolean.TRUE.equals(leader.getIsLeader())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not is leader");
        }

        GroupOrderMember groupOrderMember1 = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userId);
        if(groupOrderMember1 == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in group order");
        }

        groupOrderMember1.setIsBlacklist(false);
        groupOrderMembersRepository.save(groupOrderMember1);
        return ResponseEntity.status(HttpStatus.OK).body("Blacklist has been restored.");
    }

    @Transactional
    public ResponseEntity<?> getListAllBlackListFormGroupOrderId(Integer groupOrderId,Language language,Integer userIdReq) {

        GroupOrderMember groupOrderMember_check = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndUserUserId(groupOrderId, userIdReq);
        if(groupOrderMember_check == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group because you are not member");
        }

        if(!groupOrderMember_check.getIsLeader())
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Canot view Group because you are not leader");
        }
        GroupOrders groupOrder = groupOrdersRepository.findByGroupOrderIdAndIsDeletedFalse(groupOrderId);
        if (groupOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found");
        }
        List<CRUDGroupOrderMemberDetailResponse> crudGroupOrderMemberResponseList = new ArrayList<>();
        List<GroupOrderMember> groupOrderMemberList = groupOrderMembersRepository.findByGroupOrderGroupOrderIdAndIsBlacklistTrue(groupOrderId);
        for (GroupOrderMember groupOrderMember : groupOrderMemberList) {
            CartGroup cartGroup = groupOrderMember.getCartGroup();
            CRUDCartGroupResponse crudCartGroupResponse = null;


            if (cartGroup != null) {
                List<CRUDCartItemGroupResponse> cartItemResponses = new ArrayList<>();
                List<CartItemGroup> cartItems = cartItemGroupRepository.findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(cartGroup.getCartId());
                if (cartItems != null) {
                    for (CartItemGroup cartItem : cartItems) {
                        String proname = "";
                        if (language == Language.VN) {
                            proname = cartItem.getProductVariants().getProduct().getProName();
                        } else {
                            Product product = cartItem.getProductVariants().getProduct();
                            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                            proname = productTranslation.getProName();
                        }
                        cartItemResponses.add(new CRUDCartItemGroupResponse(
                                cartItem.getCartItemId(),
                                cartItem.getProductVariants().getProduct().getProId(),
                                proname,
                                cartItem.getCartGroup().getCartId(),
                                cartItem.getProductVariants().getSize(),
                                cartItem.getProductVariants().getPrice(),
                                cartItem.getTotalPrice(),
                                cartItem.getQuantity(),
                                extractFirstImageUrl(cartItem.getProductVariants().getProduct().getListProImg())
                        ));
                    }

                }

                crudCartGroupResponse = new CRUDCartGroupResponse(
                        cartGroup.getCartId(),
                        cartGroup.getGroupOrderMember().getGroupOrder().getGroupOrderId(),
                        cartGroup.getUser().getUserId(),
                        cartGroup.getGroupOrderMember().getMemberId(),
                        cartGroup.getTotalPrice(),
                        cartGroup.getTotalProduct(),
                        cartItemResponses
                );
                CRUDGroupOrderMemberDetailResponse crudGroupOrderMemberResponse = new CRUDGroupOrderMemberDetailResponse(
                        groupOrderMember.getMemberId(),
                        groupOrderMember.getUser().getFullName(),
                        groupOrderMember.getGroupOrder().getGroupOrderId(),
                        groupOrderMember.getUser().getUserId(),
                        groupOrderMember.getAmount(),
                        groupOrderMember.getIsPaid(),
                        groupOrderMember.getIsLeader(),
                        groupOrderMember.getNote(),
                        groupOrderMember.getStatus(),
                        groupOrderMember.getTypePayment(),
                        groupOrderMember.getDateCreated(),
                        groupOrderMember.getDateUpdated(),
                        groupOrderMember.getDateDeleted(),
                        groupOrderMember.getIsDeleted(),
                        crudCartGroupResponse
                );

                crudGroupOrderMemberResponseList.add(crudGroupOrderMemberResponse);
            }


        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListAllGroupOrderMemberBlackListResponse(
                groupOrderId,
                crudGroupOrderMemberResponseList.size(),
                crudGroupOrderMemberResponseList
        ));
    }



}