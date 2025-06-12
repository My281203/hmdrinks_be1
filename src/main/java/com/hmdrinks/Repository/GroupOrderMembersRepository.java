package com.hmdrinks.Repository;
import com.hmdrinks.Entity.GroupOrderMember;
import com.hmdrinks.Entity.Payment;
import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.Status_Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface GroupOrderMembersRepository extends JpaRepository<GroupOrderMember, Long> {
    @Query("""
           SELECT gom
           FROM GroupOrderMember gom
           WHERE gom.user.userId = :userId
           AND gom.groupOrder.status NOT IN (:completed, :canceled)
           AND (gom.isDeleted = false OR gom.isDeleted IS NULL)
           """)
    List<GroupOrderMember> findActiveGroupOrdersByUserId(
            @Param("userId") int userId,
            @Param("completed") StatusGroupOrder completed,
            @Param("canceled") StatusGroupOrder canceled
    );
    @Query("SELECT COUNT(gm) > 0 FROM GroupOrderMember gm " +
            "WHERE gm.user.userId = :userId " +
            "AND gm.isLeader = true " +
            "AND gm.isDeleted = false " +
            "AND gm.groupOrder.status NOT IN (:excludedStatuses)")
    boolean isUserAlreadyLeadingOtherGroup(@Param("userId") Long userId,
                                           @Param("excludedStatuses") List<StatusGroupOrder> excludedStatuses);

    @Query("SELECT COUNT(gm) > 0 FROM GroupOrderMember gm " +
            "WHERE gm.user.userId = :userId " +
            "AND gm.isLeader = true " +
            "AND gm.isDeleted = false " +
            "AND gm.groupOrder.status NOT IN (:excludedStatuses)")
    boolean isUserLeadingActiveGroup(@Param("userId") Long userId,
                                     @Param("excludedStatuses") List<StatusGroupOrder> excludedStatuses);

    GroupOrderMember findByGroupOrderGroupOrderIdAndUserUserIdAndIsDeletedTrue(int groupOrderId, Integer userId);
    GroupOrderMember findByGroupOrderGroupOrderIdAndUserUserIdAndIsBlacklistTrue(int groupOrderId, Integer userId);
    GroupOrderMember findByGroupOrderGroupOrderIdAndUserUserIdAndIsDeletedFalse(int groupOrderId, long userId);
    List<GroupOrderMember> findByGroupOrderGroupOrderIdAndIsDeletedFalse(int groupOrderId);
    GroupOrderMember findByGroupOrderGroupOrderIdAndUserUserId(int groupOrderId, Integer userId);
    List<GroupOrderMember> findByGroupOrderGroupOrderIdAndIsBlacklistFalse(int groupOrderId);
    List<GroupOrderMember> findByGroupOrderGroupOrderIdAndIsBlacklistTrue(int groupOrderId);
    List<GroupOrderMember> findByUserUserIdAndIsLeaderFalse(int userId);
    List<GroupOrderMember> findByUserUserIdAndGroupOrderStatusNotInAndIsDeletedFalse(int userId, List<StatusGroupOrder> excludedStatuses);
}
