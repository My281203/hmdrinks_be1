package com.hmdrinks.Enum;

import lombok.Getter;

@Getter
public enum LeaveStatus {
    APPROVED,   // Đã được duyệt
    WAITING,    // Đang chờ duyệt
    REJECTED    // Bị từ chối
}
