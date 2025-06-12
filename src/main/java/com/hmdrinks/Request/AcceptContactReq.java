package com.hmdrinks.Request;

import com.hmdrinks.Enum.Status_Contact;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class AcceptContactReq {
    @Min(value = 1, message = "contactId phải lớn hơn 0")
    private int contactId;
    private String content;
}
