package com.hmdrinks.Response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ListAllAbsenceRequestByUserIdResponse {
    private  int userId;
    private int total;
    List<CRUDAbsenceRequestResponse> listAbsence;
}
