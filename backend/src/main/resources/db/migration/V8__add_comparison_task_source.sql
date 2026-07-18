ALTER TABLE comparison_task
    ADD COLUMN source_type VARCHAR(32) NULL,
    ADD COLUMN source_event_id VARCHAR(128) NULL,
    ADD COLUMN source_sender_id VARCHAR(128) NULL,
    ADD UNIQUE KEY uk_task_source_event (source_type, source_event_id);
