-- Preserve beneficiary_needs for future categorized needs.
-- Support both fresh databases and columns already added manually.
ALTER TABLE beneficiaries
    ADD COLUMN IF NOT EXISTS needs TEXT NOT NULL DEFAULT '';

UPDATE beneficiaries SET needs = '' WHERE needs IS NULL;

ALTER TABLE beneficiaries
    ALTER COLUMN needs TYPE TEXT,
    ALTER COLUMN needs SET DEFAULT '',
    ALTER COLUMN needs SET NOT NULL;

ALTER TABLE assistance_distributions
    ADD COLUMN IF NOT EXISTS notes TEXT NOT NULL DEFAULT '';

UPDATE assistance_distributions SET notes = '' WHERE notes IS NULL;

ALTER TABLE assistance_distributions
    ALTER COLUMN notes TYPE TEXT,
    ALTER COLUMN notes SET DEFAULT '',
    ALTER COLUMN notes SET NOT NULL;
