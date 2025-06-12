package com.hmdrinks.Request;

import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSizeItemReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "cartItemId phải lớn hơn 0")
    private int cartItemId;
    private Size size;

}
