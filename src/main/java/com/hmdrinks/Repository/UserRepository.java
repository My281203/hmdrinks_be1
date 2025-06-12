package com.hmdrinks.Repository;

import com.hmdrinks.Entity.User;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.TypeLogin;
import com.hmdrinks.Service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
 public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserId(int userId);
    @Query(value = "SELECT * FROM user WHERE user_id = :id", nativeQuery = true)
    User findUserByIdNative(@Param("id") Integer id);

    User findByUserIdAndIsDeletedFalse(int userId);

    @Query(value = "SELECT * FROM user WHERE user_id = :userId AND is_deleted = FALSE LIMIT 1", nativeQuery = true)
    User findActiveUserById(@Param("userId") int userId);

    User findByEmail(String email);
    User findByEmailAndIsDeletedFalse(String email);

    Page<User> findAll(Pageable pageable);
    List<User> findAllByIsDeletedFalse();

//    @Query("SELECT u FROM User u WHERE u.role = :role")
//    Page<User> findAllByRole(@Param("role") Role role, Pageable pageable);


    @Query("""
        SELECT 
            u.userId AS userId,
            u.userName AS userName,
            u.fullName AS fullName,
            u.avatar AS avatar,
            u.birthDate AS birthDate,
            u.street AS street,
            u.ward AS ward,
            u.district AS district,
            u.city AS city,
            u.email AS email,
            u.phoneNumber AS phoneNumber,
            u.sex AS sex,
            u.type AS type,
            u.isDeleted AS isDeleted,
            u.dateDeleted AS dateDeleted,
            u.dateUpdated AS dateUpdated,
            u.dateCreated AS dateCreated,
            u.role AS role
        FROM User u
        WHERE u.role = :role
    """)
    Page<UserService.UserProjection> findAllByRole(@Param("role") Role role, Pageable pageable);
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);




    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findAllByRole(@Param("role") Role role);

   Page<User> findByUserNameContainingOrEmailContainingOrFullNameContainingOrStreetContainingOrDistrictContainingOrCityContainingOrPhoneNumberContaining(
           String userName,
           String email,
           String fullName,
           String street,
           String district,
           String city,
           String phoneNumber,
           Pageable pageable);

    List<User> findByUserNameContainingOrEmailContainingOrFullNameContainingOrStreetContainingOrDistrictContainingOrCityContainingOrPhoneNumberContaining(
            String userName,
            String email,
            String fullName,
            String street,
            String district,
            String city,
            String phoneNumber);

    Optional<User> findByEmailAndUserIdNot(String email, Integer userId);
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false AND u.type IN :types")
    Optional<User> findByEmailAndIsDeletedFalseAndTypeIn(@Param("email") String email, @Param("types") List<TypeLogin> types);

    @Query("SELECT u FROM User u WHERE u.userName = :userName AND u.isDeleted = false")
    Optional<User> findByUserNameAndIsDeletedFalse(@Param("userName") String userName);

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.isDeleted = false")
    Optional<User> findByPhoneNumberAndIsDeletedFalse(@Param("phoneNumber") String phoneNumber);


    @Query("""
    SELECT u.userId AS userId,
           u.userName AS userName,
           u.fullName AS fullName,
           u.avatar AS avatar,
           u.birthDate AS birthDate,
           u.street AS street,
           u.ward AS ward,
           u.district AS district,
           u.city AS city,
           u.email AS email,
           u.phoneNumber AS phoneNumber,
           u.sex AS sex,
           u.type AS type,
           u.isDeleted AS isDeleted,
           u.dateDeleted AS dateDeleted,
           u.dateUpdated AS dateUpdated,
           u.dateCreated AS dateCreated,
           u.role AS role
    FROM User u
""")
    Page<UserService.UserProjection> findAllUserProjection(Pageable pageable);



}
