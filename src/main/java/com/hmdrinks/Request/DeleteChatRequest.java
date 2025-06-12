package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteChatRequest {
    @Min(value = 1, message = "ChatId phải lớn hơn 0")
    private int chatId;
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
}
