package com.hmdrinks.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CRUDReviewReq {
   @Min(value = 1, message = "ReviewId phải lớn hơn 0")
   private int reviewId;
   @Min(value = 1, message = "UserId phải lớn hơn 0")
   private int userId;
   @Min(value = 1, message = "ProId phải lớn hơn 0")
   private int proId;
   private String content;
   @Min(value = 0, message = "Rating must be at least 0")
   @Max(value = 5, message = "Rating must be at most 5")
   private int ratingStart;
}
