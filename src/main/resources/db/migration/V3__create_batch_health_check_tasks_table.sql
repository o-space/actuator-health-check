CREATE TABLE batch_health_check_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(255) NOT NULL UNIQUE,
    service_names TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_batch_task_task_id ON batch_health_check_tasks(task_id);
CREATE INDEX idx_batch_task_status ON batch_health_check_tasks(status);
