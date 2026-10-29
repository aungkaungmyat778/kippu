-- One row per seat per trip. The Seats service owns this data: the trip itself
-- lives in the Catalog service, so trip_id is just an ID with no foreign key.
CREATE TABLE trip_seats (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id         UUID        NOT NULL,
    seat_no         VARCHAR(5)  NOT NULL,
    row_no          INT         NOT NULL,
    col_label       VARCHAR(2)  NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booking_id      UUID,                 -- who holds or owns the seat
    hold_expires_at TIMESTAMPTZ,          -- only while the seat is held

    UNIQUE (trip_id, seat_no),

    -- The database refuses impossible states, even if the code has a bug.
    CONSTRAINT chk_trip_seat_status CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),
    CONSTRAINT chk_trip_seat_state CHECK (
        (status = 'AVAILABLE' AND booking_id IS NULL     AND hold_expires_at IS NULL)
     OR (status = 'HELD'      AND booking_id IS NOT NULL AND hold_expires_at IS NOT NULL)
     OR (status = 'SOLD'      AND booking_id IS NOT NULL AND hold_expires_at IS NULL)
    )
);

CREATE INDEX idx_trip_seats_booking ON trip_seats (booking_id);
CREATE INDEX idx_trip_seats_expiry  ON trip_seats (hold_expires_at) WHERE status = 'HELD';
