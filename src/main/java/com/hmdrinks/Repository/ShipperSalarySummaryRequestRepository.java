package com.hmdrinks.Repository;
import com.hmdrinks.Entity.AbsenceRequest;
import com.hmdrinks.Entity.ShipperSalarySummary;
import com.hmdrinks.Enum.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ShipperSalarySummaryRequestRepository extends JpaRepository<ShipperSalarySummary, Long> {
      ShipperSalarySummary findById(Integer id);
      List<ShipperSalarySummary> findByUserUserId(Integer userId);
      List<ShipperSalarySummary> findByMonth(Integer month);
      List<ShipperSalarySummary> findByYear(Integer year);
      List<ShipperSalarySummary> findByYearAndMonth(Integer year,Integer month);
      List<ShipperSalarySummary> findByYearAndMonthAndUserUserId(Integer year,Integer month,Integer userId);
      List<ShipperSalarySummary> findByYearAndUserUserId(Integer year,Integer userId);
      List<ShipperSalarySummary> findByMonthAndUserUserId(Integer month,Integer userId);

      Page<ShipperSalarySummary> findByUserUserId(Integer userId, Pageable pageable);

      Page<ShipperSalarySummary> findByMonth(Integer month, Pageable pageable);

      Page<ShipperSalarySummary> findByYear(Integer year, Pageable pageable);

      Page<ShipperSalarySummary> findByYearAndMonth(Integer year, Integer month, Pageable pageable);

      Page<ShipperSalarySummary> findByYearAndMonthAndUserUserId(Integer year, Integer month, Integer userId, Pageable pageable);

      Page<ShipperSalarySummary> findByYearAndUserUserId(Integer year, Integer userId, Pageable pageable);

      Page<ShipperSalarySummary> findByMonthAndUserUserId(Integer month, Integer userId, Pageable pageable);

      @Query("SELECT s FROM ShipperSalarySummary s " +
              "WHERE (s.year > :startYear OR (s.year = :startYear AND s.month >= :startMonth)) " +
              "AND (s.year < :endYear OR (s.year = :endYear AND s.month <= :endMonth))")
      List<ShipperSalarySummary> findByMonthYearBetween(
              @Param("startMonth") Integer startMonth,
              @Param("startYear") Integer startYear,
              @Param("endMonth") Integer endMonth,
              @Param("endYear") Integer endYear
      );

      @Query("SELECT s FROM ShipperSalarySummary s " +
              "WHERE (s.year > :startYear OR (s.year = :startYear AND s.month >= :startMonth)) " +
              "AND (s.year < :endYear OR (s.year = :endYear AND s.month <= :endMonth))")
      Page<ShipperSalarySummary> findByMonthYearBetweenPaged(
              @Param("startMonth") Integer startMonth,
              @Param("startYear") Integer startYear,
              @Param("endMonth") Integer endMonth,
              @Param("endYear") Integer endYear,
              Pageable pageable
      );

      @Query("SELECT s FROM ShipperSalarySummary s " +
              "WHERE (s.year > :startYear OR (s.year = :startYear AND s.month >= :startMonth)) " +
              "AND (s.year < :endYear OR (s.year = :endYear AND s.month <= :endMonth)) " +
              "AND s.user.userId = :userId")
      List<ShipperSalarySummary> findByMonthYearRangeAndUser(
              @Param("startMonth") Integer startMonth,
              @Param("startYear") Integer startYear,
              @Param("endMonth") Integer endMonth,
              @Param("endYear") Integer endYear,
              @Param("userId") Integer userId
      );


      @Query("SELECT s FROM ShipperSalarySummary s " +
              "WHERE (s.year > :startYear OR (s.year = :startYear AND s.month >= :startMonth)) " +
              "AND (s.year < :endYear OR (s.year = :endYear AND s.month <= :endMonth)) " +
              "AND s.user.userId = :userId")
      Page<ShipperSalarySummary> findByMonthYearRangeAndUserPaged(
              @Param("startMonth") Integer startMonth,
              @Param("startYear") Integer startYear,
              @Param("endMonth") Integer endMonth,
              @Param("endYear") Integer endYear,
              @Param("userId") Integer userId,
              Pageable pageable
      );


}
