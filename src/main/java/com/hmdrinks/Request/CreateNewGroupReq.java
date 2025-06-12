package com.hmdrinks.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.hmdrinks.Config.LocalTimeDeserializer;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Status_Type_Time_Group;
import com.hmdrinks.Enum.TypeGroupOrder;
import com.hmdrinks.Enum.Type_Post;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateNewGroupReq {
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private Integer userId;
    private String name;
    private  Boolean flexiblePayment;
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime datePayment;
    private TypeGroupOrder type;
    private Status_Type_Time_Group typeTime;
}
