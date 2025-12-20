package com.chainsea.healthcheck.repository;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.flyway.enabled=false"
})
class HealthCheckRecordRepositoryTest {

    @Autowired
    private HealthCheckRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private Map<String, Object> createDetails(String message) {
        return Map.of("message", message);
    }

    @Test
    void shouldReturnSavedRecordGivenHealthCheckRecordWhenSavingAndFinding() {
        // Given
        HealthCheckRecord healthCheckRecord = new HealthCheckRecord("test-service", "UP", createDetails("Service is healthy"), 150L);

        // When
        HealthCheckRecord saved = repository.save(healthCheckRecord);
        Optional<HealthCheckRecord> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getServiceName()).isEqualTo("test-service");
        assertThat(found.get().getStatus()).isEqualTo("UP");
        assertThat(found.get().getDetails()).containsEntry("message", "Service is healthy");
        assertThat(found.get().getResponseTimeMs()).isEqualTo(150L);
        assertThat(found.get().getCheckedAt()).isNotNull();
    }

    @Test
    void shouldReturnRecordsGivenServiceNameWhenFindingByServiceName() {
        // Given
        repository.save(new HealthCheckRecord("service1", "UP", createDetails("OK"), 100L));
        repository.save(new HealthCheckRecord("service1", "DOWN", createDetails("Error"), 200L));
        repository.save(new HealthCheckRecord("service2", "UP", createDetails("OK"), 150L));

        // When
        List<HealthCheckRecord> service1Records = repository.findByServiceNameOrderByCheckedAtDesc("service1");

        // Then
        assertThat(service1Records).hasSize(2);
        assertThat(service1Records.get(0).getServiceName()).isEqualTo("service1");
        assertThat(service1Records.get(1).getServiceName()).isEqualTo("service1");
    }

    @Test
    void shouldReturnRecordsGivenStatusWhenFindingByStatus() {
        // Given
        repository.save(new HealthCheckRecord("service1", "UP", createDetails("OK"), 100L));
        repository.save(new HealthCheckRecord("service2", "DOWN", createDetails("Error"), 200L));
        repository.save(new HealthCheckRecord("service3", "UP", createDetails("OK"), 150L));

        // When
        List<HealthCheckRecord> downRecords = repository.findByStatusOrderByCheckedAtDesc("DOWN");

        // Then
        assertThat(downRecords).hasSize(1);
        assertThat(downRecords.get(0).getStatus()).isEqualTo("DOWN");
        assertThat(downRecords.get(0).getServiceName()).isEqualTo("service2");
    }

    @Test
    void shouldReturnRecentRecordsGivenTimeWindowWhenFindingRecentRecords() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        HealthCheckRecord oldRecord = new HealthCheckRecord("service1", "UP", createDetails("OK"), 100L);
        oldRecord.setCheckedAt(now.minusHours(25));
        repository.save(oldRecord);

        HealthCheckRecord recentRecord1 = new HealthCheckRecord("service2", "UP", createDetails("OK"), 150L);
        recentRecord1.setCheckedAt(now.minusHours(5));
        repository.save(recentRecord1);

        HealthCheckRecord recentRecord2 = new HealthCheckRecord("service3", "DOWN", createDetails("Error"), 200L);
        recentRecord2.setCheckedAt(now.minusHours(1));
        repository.save(recentRecord2);

        // When
        List<HealthCheckRecord> recent = repository.findRecentRecords(now.minusHours(24));

        // Then
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getServiceName()).isEqualTo("service3");
        assertThat(recent.get(1).getServiceName()).isEqualTo("service2");
    }

    @Test
    void shouldReturnLatestRecordGivenServiceNameWhenFindingLatestRecord() {
        // Given
        HealthCheckRecord record1 = new HealthCheckRecord("service1", "UP", createDetails("OK"), 100L);
        record1.setCheckedAt(LocalDateTime.now().minusHours(2));
        repository.save(record1);

        HealthCheckRecord record2 = new HealthCheckRecord("service1", "DOWN", createDetails("Error"), 200L);
        record2.setCheckedAt(LocalDateTime.now().minusHours(1));
        repository.save(record2);

        HealthCheckRecord record3 = new HealthCheckRecord("service1", "UP", createDetails("OK"), 150L);
        record3.setCheckedAt(LocalDateTime.now());
        repository.save(record3);

        // When
        Optional<HealthCheckRecord> latest = repository.findFirstByServiceNameOrderByCheckedAtDesc("service1");

        // Then
        assertThat(latest).isPresent();
        assertThat(latest.get().getStatus()).isEqualTo("UP");
        assertThat(latest.get().getResponseTimeMs()).isEqualTo(150L);
    }

    @Test
    void shouldReturnCountGivenServiceNameAndStatusWhenCountingRecords() {
        // Given
        repository.save(new HealthCheckRecord("service1", "UP", createDetails("OK"), 100L));
        repository.save(new HealthCheckRecord("service1", "DOWN", createDetails("Error"), 200L));
        repository.save(new HealthCheckRecord("service1", "DOWN", createDetails("Error"), 300L));
        repository.save(new HealthCheckRecord("service2", "DOWN", createDetails("Error"), 400L));

        // When
        long downCount = repository.countByServiceNameAndStatus("service1", "DOWN");

        // Then
        assertThat(downCount).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyGivenNonExistentServiceNameWhenFindingLatestRecord() {
        // When
        Optional<HealthCheckRecord> latest = repository.findFirstByServiceNameOrderByCheckedAtDesc("non-existent");

        // Then
        assertThat(latest).isEmpty();
    }
}
