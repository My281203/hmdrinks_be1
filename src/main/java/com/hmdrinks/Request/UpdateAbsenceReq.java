package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.LeaveStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAbsenceReq {
   @Min(value = 1, message = "RequestId phải lớn hơn 0")
   private int requestId;
   private LeaveStatus status;
}
