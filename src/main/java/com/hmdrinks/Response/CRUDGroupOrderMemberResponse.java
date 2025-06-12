package com.hmdrinks.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.StatusGroupOrderMember;
import com.hmdrinks.Enum.TypePayment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRUDGroupOrderMemberResponse {

    private int memberId;
    private String name;// ID của thành viên nhóm
    private int groupOrderId;
    private StatusGroupOrder statusGroupOrder;// ID của nhóm
    private String nameGroup;
    private int userId;                   // ID người dùng
    private Double amount;                // Số tiền thành viên cần thanh toán
    private Boolean isPaid;               // Trạng thái thanh toán
    private Boolean isLeader;             // Trạng thái trưởng nhóm hay không
    private String note;                  // Ghi chú của thành viên
    private StatusGroupOrderMember status; // Trạng thái của thành viên trong nhóm
    private TypePayment typePayment;      // Loại thanh toán
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreated;    // Ngày tạo
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateUpdated;    // Ngày cập nhật
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateDeleted;    // Ngày xóa (nếu có)
    private Boolean isDeleted;
    // Trạng thái xóa hay không
}
