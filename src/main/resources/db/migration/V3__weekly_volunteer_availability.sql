-- Keep the old text values for existing records, and add structured weekly slots.
ALTER TABLE volunteer_availability
    ALTER COLUMN availability_details DROP NOT NULL,
    ADD COLUMN day_of_week VARCHAR(9),
    ADD COLUMN start_time TIME,
    ADD COLUMN end_time TIME,
    ADD CONSTRAINT chk_volunteer_availability_slot CHECK (
        (day_of_week IS NULL AND start_time IS NULL AND end_time IS NULL)
        OR
        (day_of_week IS NOT NULL AND day_of_week IN (
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
            'FRIDAY', 'SATURDAY', 'SUNDAY'
        ) AND start_time IS NOT NULL AND end_time IS NOT NULL
            AND end_time > start_time)
    );

CREATE UNIQUE INDEX uq_volunteer_availability_weekly_slot
    ON volunteer_availability(volunteer_id, day_of_week, start_time, end_time)
    WHERE day_of_week IS NOT NULL;
