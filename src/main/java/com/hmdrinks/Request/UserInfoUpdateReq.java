package com.hmdrinks.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoUpdateReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String fullName;
    private String phoneNumber;
    private String avatar;
    private String sex;
    private Date birthDay;
    private String address;
}
