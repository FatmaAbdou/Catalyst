ALTER TABLE volunteer_tasks
    DROP CONSTRAINT chk_task_campaign_or_event,
    ADD COLUMN volunteers_num INTEGER NOT NULL DEFAULT 1
        CHECK (volunteers_num > 0);

ALTER TABLE service_records
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED'));
