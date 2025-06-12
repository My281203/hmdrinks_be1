package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}
