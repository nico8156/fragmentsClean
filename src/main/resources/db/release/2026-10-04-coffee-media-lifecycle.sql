CREATE TABLE IF NOT EXISTS coffee_photo_retirements (
    photo_id UUID PRIMARY KEY,
    coffee_id UUID NOT NULL REFERENCES coffees(id) ON DELETE CASCADE,
    photo_uri VARCHAR(2000) NOT NULL,
    was_cover BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    retired_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_coffee_photo_retirements_coffee ON coffee_photo_retirements(coffee_id);
