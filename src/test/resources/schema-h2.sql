-- H2 compatible schema for tests
-- H2 doesn't support JSONB, so we use CLOB to store JSON strings
CREATE TABLE IF NOT EXISTS health_check_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    details CLOB,
    checked_at TIMESTAMP NOT NULL,
    response_time_ms BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_health_check_records_service_name ON health_check_records(service_name);
CREATE INDEX IF NOT EXISTS idx_health_check_records_status ON health_check_records(status);
CREATE INDEX IF NOT EXISTS idx_health_check_records_checked_at ON health_check_records(checked_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_check_records_service_status ON health_check_records(service_name, status);
