package com.hmdrinks.Service;

import com.hmdrinks.Entity.User;
import com.hmdrinks.Entity.UserCoin;
import com.hmdrinks.Repository.UserCointRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Response.CRUDUserCoinResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class UserCoinService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCointRepository userCointRepository;

    public ResponseEntity<?> getInfoPointCoin(Integer userId)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if(user == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        UserCoin userCoin = userCointRepository.findByUserUserId(userId);
        if(userCoin == null)
        {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user Coin");
        }

        return  ResponseEntity.status(HttpStatus.OK).body(new CRUDUserCoinResponse(
                userCoin.getUserCoinId(),
                userCoin.getPointCoin(),
                userCoin.getUser().getUserId()
        ));
    }


}