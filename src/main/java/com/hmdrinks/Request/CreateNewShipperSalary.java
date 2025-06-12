package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewShipperSalary {
   @Min(value = 1, message = "userId phải lớn hơn 0")
   private int userId;
   private int month;
   private int year;
   private BigDecimal baseSalary;
   private String note;
}
