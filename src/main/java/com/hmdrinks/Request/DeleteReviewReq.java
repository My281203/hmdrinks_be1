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
public class DeleteReviewReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "reviewId phải lớn hơn 0")
    private int reviewId;
}
