package com.hmdrinks.Request;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNoteShipperAttendance {
    @Min(value = 1, message = "Id phải lớn hơn 0")
    private int id;
    private String note;

    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
}
