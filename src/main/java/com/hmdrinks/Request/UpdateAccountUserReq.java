package com.hmdrinks.Request;

import com.hmdrinks.Enum.Role;
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
public class UpdateAccountUserReq {
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
    private String fullName;
    private String userName;
    @Email
    private String email;
    private String password;
    private Role role;
    private String phoneNumber;
    private Boolean isDeleted;
    private String dateUpdated;
}
