package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Type_Post;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNoteShipment {
    @Min(value = 1, message = "ShipmentId phải lớn hơn 0")
    private int shipmentId;
    private String note;

}
