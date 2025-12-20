-- Create health_check_records table
CREATE TABLE IF NOT EXISTS health_check_records
(
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    details VARCHAR(1000),
    checked_at TIMESTAMP NOT NULL,
    response_time_ms BIGINT NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_health_check_records_service_name ON health_check_records(service_name);
CREATE INDEX IF NOT EXISTS idx_health_check_records_status ON health_check_records(status);
CREATE INDEX IF NOT EXISTS idx_health_check_records_checked_at ON health_check_records(checked_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_check_records_service_status ON health_check_records(service_name, status);
