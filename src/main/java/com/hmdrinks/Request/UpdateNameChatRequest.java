package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNameChatRequest {
    @Min(value = 1, message = "chatId phải lớn hơn 0")
    private int chatId;
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
    private String chatName;
}
