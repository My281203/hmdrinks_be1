package com.hmdrinks.Enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AttendanceStatus {
    ON_TIME,        // Đi đúng giờ
    LATE,           // Đi trễ
    ABSENT,         // Vắng không phép
    ON_LEAVE,
    NONE// Nghỉ phép (có đơn)
}

