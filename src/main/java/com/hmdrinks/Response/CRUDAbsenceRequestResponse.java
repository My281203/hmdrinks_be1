package com.hmdrinks.Response;

import com.hmdrinks.Enum.LeaveStatus;
import com.hmdrinks.Enum.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.discovery.zen.MembershipAction;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CRUDAbsenceRequestResponse {
    private  int userId;
    private int requestId;
    private  String reason;
    private LeaveStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

}
