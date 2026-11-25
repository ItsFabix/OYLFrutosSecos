CREATE TABLE IF NOT EXISTS notification_events (
    event_id VARCHAR(120) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_notification_events_order ON notification_events(order_id, created_at);
