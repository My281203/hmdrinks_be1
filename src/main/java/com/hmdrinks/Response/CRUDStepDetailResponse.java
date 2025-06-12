package com.hmdrinks.Response;



import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRUDStepDetailResponse {
    private Integer step_id;
    private String distance;
    private  String duration;
    private String instruction;
    private  Double latitude;
    private  Double longitude;
    private  Integer directionId;

}

