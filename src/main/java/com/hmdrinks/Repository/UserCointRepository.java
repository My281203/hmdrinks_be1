package com.hmdrinks.Repository;

import com.hmdrinks.Entity.UserCoin;
import com.hmdrinks.Entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCointRepository extends JpaRepository<UserCoin,Integer> {


    UserCoin findByUserCoinId(int userId);

    UserCoin findByUserUserId(int userId);

    UserCoin findByUserUserIdAndUserCoinId(int userId,int voucherId);
}
