ALTER TABLE communications
    ADD COLUMN assignment_id INTEGER
        REFERENCES volunteer_assignments(assignment_id) ON DELETE SET NULL,
    ADD COLUMN read_at TIMESTAMP;
