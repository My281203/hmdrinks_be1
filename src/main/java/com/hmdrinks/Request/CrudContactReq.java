package com.hmdrinks.Request;

import com.hmdrinks.Enum.Status_Contact;
import com.hmdrinks.Enum.Status_Voucher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class CrudContactReq {
    @Min(value = 1, message = "contactId phải lớn hơn 0")
    private int contactId;
    @Email
    private String email;
    private String phone;
    private String fullName;
    private String description;
    private Status_Contact statusContact;
}
