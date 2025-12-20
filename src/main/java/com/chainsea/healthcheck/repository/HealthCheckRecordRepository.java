package com.chainsea.healthcheck.repository;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthCheckRecordRepository extends JpaRepository<HealthCheckRecord, Long> {

    List<HealthCheckRecord> findByServiceNameOrderByCheckedAtDesc(String serviceName);

    List<HealthCheckRecord> findByStatusOrderByCheckedAtDesc(String status);

    @Query("SELECT h FROM HealthCheckRecord h WHERE h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<HealthCheckRecord> findRecentRecords(LocalDateTime since);

    @Query("SELECT h FROM HealthCheckRecord h WHERE h.serviceName = :serviceName AND h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<HealthCheckRecord> findByServiceNameAndCheckedAtAfterOrderByCheckedAtDesc(String serviceName, LocalDateTime since);

    Optional<HealthCheckRecord> findFirstByServiceNameOrderByCheckedAtDesc(String serviceName);

    long countByServiceNameAndStatus(String serviceName, String status);
}
