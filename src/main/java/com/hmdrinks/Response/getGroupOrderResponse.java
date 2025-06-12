package com.hmdrinks.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.TypeGroupOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import scala.Int;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class getGroupOrderResponse {
    private Integer total;
    CRUDGroupOrderResponse crudGroupOrderResponse;
    List<CRUDGroupOrderMemberDetailResponse> crudGroupOrderResponseList;
}
