package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Voucher;
import com.hmdrinks.Service.VoucherService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Voucher findByVoucherIdAndIsDeletedFalse(int voucherId);
    Voucher findByKeyAndIsDeletedFalse(String key);
    Voucher findByVoucherId(int voucherId);
    List<Voucher> findByIsDeletedFalse();
    Voucher findByPostPostIdAndIsDeletedFalse(int postId);
    Voucher findByPostPostId(int postId);
    @Query("""
    SELECT v.voucherId AS voucherId, v.key AS key, v.number AS number, 
           v.startDate AS startDate, v.endDate AS endDate,
           v.discount AS discount, v.status AS status,
           p.postId AS postId
    FROM Voucher v
    JOIN v.post p
""")
    List<VoucherService.SimpleVoucherProjection> findAllSimpleVoucher();


}
