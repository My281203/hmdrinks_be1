package com.hmdrinks.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewReview {
   @Min(value = 1, message = "userId phải lớn hơn 0")
   private int userId;
   @Min(value = 1, message = "proId phải lớn hơn 0")
   private int proId;
   private String content;
   @Min(value = 0, message = "Rating must be at least 0")
   @Max(value = 5, message = "Rating must be at most 5")
   private int ratingStart;
}
