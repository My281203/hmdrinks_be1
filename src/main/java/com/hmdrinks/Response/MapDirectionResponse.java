package com.hmdrinks.Response;



import lombok.*;

import java.util.List;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MapDirectionResponse {
    private  Integer directionId;
    private  Integer shipmentId;
    private  String polyline;
    private  String mapRouteHTML;
    List<CRUDStepDetailResponse> listStepDetail;
}

